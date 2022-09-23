(ns cljfx.dev
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
  ([id props]
   (register-props! id nil props))
  ([id parent props]
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

(defn ^{:arglists '([id & {:keys [spec of]}])} register-type! [id & {:as opts}]
  {:pre [(ident? id)]}
  (swap! registry update :types assoc id (assoc opts :id id))
  id)

(defn only-keys [ks]
  (fn [m]
    (every? #(contains? ks %) (keys m))))

(defn instance-of [c]
  (fn [x]
    (instance? c x)))

(defmulti keyword-prop->spec-form :type)

(defn prop->spec-form [prop]
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

(defn register-composite! [id & {:keys [parent props of req]}]
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

(defn wrap-type->lifecycle [type->lifecycle]
  (fn [type]
    (wrap-lifecycle type type->lifecycle)))

(def type->lifecycle
  (wrap-type->lifecycle (some-fn fx/keyword->lifecycle fx/fn->lifecycle)))

;; next steps:
;; - profile if validation is a bottleneck?
;; - documentation
;; - release on clojars
;; stretch goals
;; - ui reference for searching the props/types/etc
;; - dev cljfx type->lifecycle wrapper that adds inspector capabilities.
;; - dev ui builder