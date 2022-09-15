(ns cljfx.dev
  (:require [cljfx.lifecycle :as lifecycle]
            [cljfx.api :as fx]
            [clojure.spec.alpha :as s]))

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

(defn- type->lifecycle [x]
  (or (*type->lifecycle* x) x))

(defn- valid-fx-type? [m]
  (let [lifecycle (type->lifecycle (:fx/type m))]
    (or (contains? (meta lifecycle) `lifecycle/create)
        (satisfies? lifecycle/Lifecycle lifecycle))))

(defmacro defdynaspec [name & fn-tail]
  (let [multi-name (symbol (str name "-multi-fn"))]
    `(do
       (defmulti ~multi-name (constantly nil))
       (defmethod ~multi-name nil ~@fn-tail)
       (def ~name (s/multi-spec ~multi-name nil)))))

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

(defn- desc-of [c-sym]
  (fn [desc]
    (let [type (:fx/type desc)
          id (*type->id* type)]
      (if (nil? id)
        true
        (if-let [of (-> @registry :types id :of)]
          (cond
            (symbol? of) (isa? (resolve of) (resolve c-sym))
            (keyword? of) (recur (get desc of))
            :else (throw (ex-info (str "Unknown 'instance of' definition: " of)
                                  {:of of})))
          true)))))

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

(load "definitions")

(load "extensions")

(defmulti short-keyword-prop-help-string :type)
(defn- short-prop-help-string [{:keys [type] :as prop-desc}]
  (if (symbol? type)
    (str "instance of " type)
    (short-keyword-prop-help-string prop-desc)))
(defmethod short-keyword-prop-help-string :default [{:keys [type]}]
  (name type))
(defmethod short-keyword-prop-help-string :desc [{:keys [of]}]
  (str "cljfx desc of " of))
(defmethod short-keyword-prop-help-string :enum [{:keys [of]}]
  (let [options (into (sorted-set)
                      (map #(keyword (str/replace (str/lower-case (.name ^Enum %)) #"_" "-")))
                      (.getEnumConstants (resolve of)))]
    (str "either of: " (str/join ", " options))))

(defmethod short-keyword-prop-help-string :coll [{:keys [item]}]
  (str "coll of " (short-keyword-prop-help-string item)))

(defmethod short-keyword-prop-help-string :add-props [{:keys [to props]}]
  (str (short-keyword-prop-help-string to) " with extra props (" (str/join  ", "(sort (keys props))) ")"))

(defmethod short-keyword-prop-help-string :pref-or-computed-size-double [_]
  (str "number, :use-computed-size or :use-pref-size"))
(defmethod short-keyword-prop-help-string :computed-size-double [_]
  (str "number or :use-computed-size"))
(defmethod short-keyword-prop-help-string :animation [_]
  (str "number or :indefinite"))
(defmethod short-keyword-prop-help-string :animation-status [_]
  "either of: :running, :paused, :stopped")
(defmethod short-keyword-prop-help-string :map [{:keys [key value]}]
  (str "map from " (short-keyword-prop-help-string key) " to " (short-keyword-prop-help-string value)))
(defmethod short-keyword-prop-help-string :media-player-state [_]
  "either of: :playing, :paused, :stopped")

(defmulti long-keyword-prop-help-string :type)
(defmethod long-keyword-prop-help-string :default [prop]
  (short-keyword-prop-help-string prop))
(defn long-prop-help-string [{:keys [type] :as prop}]
  (if (symbol? type)
    (str "Instance of:\n" type)
    (long-keyword-prop-help-string prop)))
(defmethod long-keyword-prop-help-string :enum [{:keys [of]}]
  (let [consts (.getEnumConstants (resolve of))]
    (str "Enum:\n"
         of
         "\n\nIdiomatic values:\n- "
         (->> consts
              (map #(keyword (str/replace (str/lower-case (.name ^Enum %)) #"_" "-")))
              (into (sorted-set))
              (str/join "\n- "))
         "\n\nAlso works:\n- "
         (->> consts
              (map #(str of "/" (.name ^Enum %)))
              sort
              (str/join "\n- ")))))
(defmethod long-keyword-prop-help-string :insets [_]
  "Insets, either:
- number
- map with optional keys - :top, :bottom, :left, :right - numbers
- literal - :empty
- instance of javafx.geometry.Insets")
(defmethod long-keyword-prop-help-string :image [_]
  "Image, either:
- instance of javafx.scene.image.Image
- string: either a resource path or URL string pointing to image
- map with required :url (url string) or :is (input stream) keys and optional :requested-width (number), :requested-height (number), :preserve-ratio (boolean), :smooth (boolean) and :background-loading (boolean) keys")
(defmethod long-keyword-prop-help-string :duration [_]
  "Duration, either:
- tuple of number and time unit (:ms, :s, :m, or :h), e.g. [10 :s]
- string in the format [number][ms|s|m|h], e.g. 10s
- number (ms)
- literal - :zero, :one (ms), :indefinite or :unknown
- instance of javafx.util.Duration")
(defmethod long-keyword-prop-help-string :font [_]
  "Font, either:
- string, font family name
- number, font size (of a default font)
- literal - :default
- map with required :family key (string) and optional :weight, :posture and :size (number) keys
- instance of javafx.scene.text.Font")

(comment
  (sort
    (clojure.set/difference
      (set (keys (.getMethodTable ^clojure.lang.MultiFn keyword-prop->spec-form)))
      (into #{:int :ifn :any :string :boolean}
            (keys (.getMethodTable ^clojure.lang.MultiFn long-keyword-prop-help-string))))))

(defn- print-table [items & columns]
  (let [columns (vec columns)
        column-strs (mapv #(into [(:label %)] (map (comp str (:fn %))) items) columns)
        max-lengths (mapv #(transduce (map count) max 0 %) column-strs)]
    (dotimes [i (inc (count items))]
      (let [row (mapv #(% i) column-strs)]
        (println
          (str/join
            "    "
            (map (fn [max-length item]
                   (str item (str/join (repeat (- max-length (count item)) \space))))
                 max-lengths row)))))))

(defn help
  ([fx-type]
   (cond
     (or (keyword? fx-type) (qualified-symbol? fx-type))
     (let [r @registry
           props (get-in r [:props fx-type])
           type (get-in r [:types fx-type])]
       (when (symbol? (:of type))
         (println "Class:")
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
         (print-table
           (sort (keys props))
           {:label "Props" :fn identity}
           {:label "Value type" :fn #(-> % props short-prop-help-string)}))
       (when (and (not props) (:spec type))
         (println "Spec:")
         (println (s/form (:spec type))))
       (when (and (not type) (not props))
         (println '???)))

     (or (simple-symbol? fx-type) (class? fx-type))
     (let [cls (if (symbol? fx-type) (resolve fx-type) fx-type)
           r @registry
           ts (->> r
                   :types
                   vals
                   (filter (fn [{:keys [of]}]
                             (and (symbol? of)
                                  (isa? (resolve of) cls))))
                   (sort-by (comp str :id)))]
       (println "Class:")
       (println cls)
       (println)
       (when (seq ts)
         (println "Known cljfx types:")
         (print-table
           ts
           {:label "Cljfx type" :fn :id}
           {:label "Class" :fn :of})))

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
         (println (long-prop-help-string prop))
         (println '???)))

     :else
     (println '???))))

(comment

  (help :image-view :image)
  (help :grid-pane :children)
  (help :grid-pane :node-orientation)
  (help :grid-pane :accessible-role)

  (help :text-formatter)
  (help :labeled :font))

;; next steps:
;; 1. api for looking up type and prop information:
;;    - extended prop description
;;    - generic help?
;; 2. dev cljfx type->lifecycle wrapper that validates and contextualizes errors
;;    in terms of a cljfx component hierarchy
;; stretch goals
;; 3. ui reference for searching the props/types/etc
;; 4. dev cljfx type->lifecycle wrapper that adds inspector capabilities.
;; 5. dev ui builder