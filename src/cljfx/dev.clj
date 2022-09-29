(ns cljfx.dev
  "Helpers for cljfx app development that shouldn't be included into production

  You can get help for existing lifecycles and props by using help fn:

    (cljfx.dev/help)
    ;; prints information about all cljfx lifecycles including :label
    (cljfx.dev/help :label)
    ;; prints information about label and its props including :graphic
    (cljfx.dev/help :label :graphic)
    ;; prints information about :graphic prop of a label

  You can also add cljfx component validation that greatly improves error
  messages using cljfx.dev/type->lifecycle (or cljfx.dev/wrap-type->lifecycle):

    (fx/create-component
      {:fx/type :stage
       :scene {:fx/type :scene
               :root {:fx/type :label
                      :text true}}}
      {:fx.opt/type->lifecycle cljfx.dev/type->lifecycle})
    ;; Execution error (ExceptionInfo) at cljfx.dev/ensure-valid-desc (validation.clj:62).
    ;; Invalid cljfx description of :stage type:
    ;; true - failed: string? in [:scene :root :text]
    ;;
    ;; Cljfx component stack:
    ;;   :stage"
  (:require [cljfx.lifecycle :as lifecycle]
            [cljfx.api :as fx]
            [clojure.spec.alpha :as s]
            [clojure.set :as set]))

(def ^:private registry
  ;; types is a map:
  ;;   id -> lifecycle config (lc)
  ;;   id is either:
  ;;   - keyword (assume invalid if validated id not registered)
  ;;   - symbol (assume valid if validated id not registered)
  ;;   lifecycle config is a map with keys:
  ;;   - :id
  ;;   - :spec (optional, the spec)
  ;;   - :of (symbol of a class this lifecycle produces, or maybe fn from desc to symbol)
  ;; props is a map of id -> prop kw -> prop type maps (i.e. {:type ...})
  (atom {:types {}
         :props {}}))

(defn any->id [x]
  (:cljfx/id (meta x)))

(defn keyword->id [x]
  (when (keyword? x) x))

(defn fn->id [x]
  (when (fn? x)
    (when-let [[_ str] (->> x
                            class
                            .getName
                            Compiler/demunge
                            (re-matches #"^([^/]*?/([^/]*?|/))(--\d\d\d\d)?$"))]
      (symbol str))))

(def ^:dynamic *type->id*
  (some-fn any->id keyword->id fn->id))

(def ^:dynamic *type->lifecycle*
  (some-fn fx/keyword->lifecycle fx/fn->lifecycle))

(defn- valid-fx-type? [m]
  (let [type (:fx/type m)
        lifecycle (or (*type->lifecycle* type) type)]
    (or (contains? (meta lifecycle) `lifecycle/create)
        (satisfies? lifecycle/Lifecycle lifecycle))))

(defmacro defdynaspec [name & fn-tail]
  (let [multi-name (symbol (str name "-multi-fn"))]
    `(do
       (defmulti ~multi-name (constantly :cljfx/desc))
       (defmethod ~multi-name :cljfx/desc ~@fn-tail)
       (def ~name (s/multi-spec ~multi-name :cljfx/desc)))))

(defn- keyword-id-should-be-registered [_] false)
(def ^:private keyword-id-should-be-registered-spec (s/spec keyword-id-should-be-registered))
(def ^:private any-spec (s/spec any?))

(defdynaspec desc->spec [m]
  (let [type (:fx/type m)
        id (*type->id* type)]
    (if (nil? id)
      any-spec
      (if-let [lc (-> @registry :types id)]
        (or (some->> (:spec lc) (s/and (s/conformer #(dissoc % :fx/type))))
            any-spec)
        (if (keyword? id)
          keyword-id-should-be-registered-spec              ;; assume typo
          any-spec)))))

(s/def :cljfx/desc
  (s/and map? valid-fx-type? desc->spec))

(defn register-props!
  "Associate props description with some id

  Args:
    id        prop identifier, either keyword or symbol
    parent    semantical parent id of the prop map, meaning props with the id
              should also accept props with parent id
    props     a map from keyword to prop description, which is a map with
              a :type key that can be either:
              - symbol of a class name
              - keyword that defines a corresponding spec form by extending
                keyword-prop->spec-form multi-method"
  ([id props]
   (register-props! id nil props))
  ([id parent props]
   {:pre [(ident? id)
          (or (nil? parent) (ident? parent))
          (or (nil? props)
              (and (map? props)
                   (every? (fn [[k v]]
                             (and (keyword? k)
                                  (contains? v :type)))
                           props)))]}
   (swap!
     registry
     (fn [registry]
       (update
         registry
         :props (fn [id->props]
                  (let [props
                        (cond->> props
                                 parent
                                 (merge
                                   (or (id->props parent)
                                       (throw
                                         (ex-info
                                           (str "parent " parent " not registered")
                                           {:parent parent
                                            :ids (set (keys id->props))})))))]
                    (assoc id->props id props))))))
   id))

(defn register-type!
  "Associate cljfx type description with some id

  Optional kv-args:
    :spec    a spec to use when validating props of components with the id
    :of      component instance class identifier, either:
             - symbol of a class name, e.g. javafx.scene.Node
             - keyword of a prop that hold another cljfx description that
               defines component instance class, e.g. :desc"
  [id & {:keys [spec of] :as opts}]
  {:pre [(ident? id)
         (or (nil? of)
             (ident? of))]}
  (swap! registry update :types assoc id (assoc opts :id id))
  id)

(defn only-keys [ks]
  (fn [m]
    (every? #(contains? ks %) (keys m))))

(defn instance-of [c]
  (fn [x]
    (instance? c x)))

(defmulti keyword-prop->spec-form :type)

(defn prop->spec-form
  "Convert prop type config to spec form (i.e. clojure form that evals to spec)

  You can extend prop type configs by adding more implementations to
  keyword-prop->spec-form multimethod"
  [prop]
  (let [{:keys [type]} prop]
    (if (symbol? type)
      `(instance-of ~type)
      (keyword-prop->spec-form prop))))

(defn make-composite-spec [id & {:keys [req]}]
  (let [props (-> @registry :props (get id))
        ks (set (keys props))
        spec-ns (str "cljfx.composite." (name id))
        k->spec-kw #(keyword spec-ns (name %))]
    (eval
      `(do
         ~@(for [k ks]
             `(s/def ~(k->spec-kw k) ~(prop->spec-form (get props k))))
         (s/and (s/keys
                  ~@(when req
                      [:req-un (if (set? req)
                                 [(list* 'or (mapv #(list* 'and (mapv k->spec-kw %)) req))]
                                 (mapv k->spec-kw req))])
                  :opt-un ~(into [] (map k->spec-kw) (sort ks)))
                (only-keys ~ks))))))

(defn register-composite!
  "Associate a composite lifecycle type description with some id

  Required kv-args:
    :of    symbol of a component instance class

  Optional kv-args:
    :parent    semantic parent id of a lifecycle, meaning lifecycle with the id
               should also accept all props of parent id
    :props     a map from keyword to prop description, which is a map with
               a :type key that can be either:
               - symbol of a class name
               - keyword that defines a corresponding spec form by extending
                 keyword-prop->spec-form multi-method
    :req       required props on the component, either:
               - a vector of prop keywords (all are required)
               - a set of vectors of prop keywords (either vector is required)"
  [id & {:keys [parent props of req]}]
  {:pre [(symbol? of)
         (every? simple-keyword? (keys props))]}
  (register-props! id parent props)
  (apply register-type! id :spec (make-composite-spec id :req req) :of of (when req
                                                                            [:req req])))

(load "dev/definitions")

(load "dev/extensions")

(defmulti short-keyword-prop-help-string :type)
(defmethod short-keyword-prop-help-string :default [{:keys [type]}]
  (name type))
(defn- short-prop-help-string [{:keys [type] :as prop-desc}]
  (if (symbol? type)
    (str "instance of " type)
    (short-keyword-prop-help-string prop-desc)))

(defmulti long-keyword-prop-help-syntax :type)
(defmethod long-keyword-prop-help-syntax :default [prop]
  (short-keyword-prop-help-string prop))
(defn long-prop-help-syntax [{:keys [type] :as prop}]
  (if (symbol? type)
    (str "Instance of:\n" type)
    (long-keyword-prop-help-syntax prop)))

(load "dev/help")

(defn help
  "Print help about cljfx types and props"
  ([]
   (let [ts (->> @registry :types)]
    (println "Available cljfx types:")
    (println
      (str-table
        (->> ts keys (sort-by str))
        {:label "Cljfx type" :fn identity}
        {:label "Instance class" :fn #(let [of (:of (get ts %))]
                                        (if (symbol? of) (str of) ""))}))))
  ([fx-type]
   (cond
     (or (keyword? fx-type) (qualified-symbol? fx-type))
     (let [r @registry
           props (get-in r [:props fx-type])
           type (get-in r [:types fx-type])]
       (when (or type props)
         (println "Cljfx type:")
         (println fx-type)
         (println))
       (when (symbol? (:of type))
         (println "Instance class:")
         (println (:of type))
         (println))
       (when (:req type)
         (if (set? (:req type))
           (do (println "Required props, either:")
               (doseq [req (:req type)]
                 (println "-" (str/join ", " (sort req)))))
           (do (println "Required props:")
               (println (str/join ", " (sort (:req type))))))
         (println))
       (when props
         (println
           (str-table
             (sort (keys props))
             {:label "Props" :fn identity}
             {:label "Value type" :fn #(-> % props short-prop-help-string)})))
       (when (and (not props) (:spec type))
         (println "Spec:")
         (println (s/describe (:spec type))))
       (when (and (not type) (not props))
         (println '???)))

     (or (simple-symbol? fx-type) (class? fx-type))
     (let [ts (known-types-of fx-type)]
       (println "Class:")
       (println fx-type)
       (println)
       (when (seq ts)
         (println "Fitting cljfx types:")
         (println
           (str-table
             ts
             {:label "Cljfx type" :fn :id}
             {:label "Class" :fn :of}))))

     (*type->id* fx-type)
     (recur (*type->id* fx-type))

     :else
     (println '???)))
  ([fx-type prop-kw]
   (cond
     (or (keyword? fx-type) (qualified-symbol? fx-type))
     (let [r @registry
           prop (get-in r [:props fx-type prop-kw])]
       (if prop
         (do
           (println (str "Prop of " fx-type " - " prop-kw))
           (println)
           (println (convert-help-syntax-to-string (long-prop-help-syntax prop))))
         (println '???)))

     :else
     (println '???))))

(load "dev/validation")

(defn wrap-type->lifecycle
  "Wrap type->lifecycle used in the cljfx UI app with improved error messages

  Wrapped lifecycle performs spec validation of cljfx descriptions that results
  in better error messages shown when cljfx descriptions are invalid.

  Additionally, exceptions thrown during cljfx lifecycle show a cljfx component
  stack to help with debugging.

  Optional kv-args:
    type->lifecycle    the type->lifecycle fn used in opts of your app
    type->id           custom type->id if you need a way to get id from your
                       custom lifecycles"
  [& {:keys [type->lifecycle type->id]
      :or {type->lifecycle *type->lifecycle*
           type->id *type->id*}}]
  (let [f (memoize wrap-lifecycle)]
    (fn [type]
      (f type type->lifecycle type->id))))

(def type->lifecycle
  "Default type->lifecycle that can be used in the cljfx UI app to improve error
  messages"
  (wrap-type->lifecycle))

(defn explain-desc
  "Validate cljfx description and report any issues

  Args:
    type->lifecycle    the type->lifecycle fn used in opts of your app
    type->id           custom type->id if you need a way to get id from your
                       custom lifecycles"
  [desc & {:keys [type->lifecycle type->id]
           :or {type->lifecycle *type->lifecycle*
                type->id *type->id*}}]
  (binding [*type->lifecycle* type->lifecycle
            *type->id* type->id]
    (if-let [explain-data (s/explain-data :cljfx/desc desc)]
      (println (explain-str explain-data))
      (println "Success!"))))

(load "dev/ui")

(defn help-ui
  "Open a window with cljfx type and prop reference"
  []
  (launch-help-ui!)
  nil)

;; stretch goals
;; - integrate javadocs
;; - dev cljfx type->lifecycle wrapper that adds inspector capabilities.
;; - dev ui builder