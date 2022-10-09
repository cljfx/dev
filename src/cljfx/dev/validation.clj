(in-ns 'cljfx.dev)

(defn- type->string [type->id fx-type]
  (str (or (type->id fx-type) fx-type)))

(defn- re-throw-with-stack [type->id ^Throwable ex stack]
  (if (::cause (ex-data ex))
    (throw ex)
    (throw (doto (ex-info
                   (str (ex-message ex)
                        "\n\nCljfx component stack:\n  "
                        (->> stack
                             (map #(type->string type->id %))
                             (str/join "\n  "))
                        "\n")
                   (with-meta {::cause ex} {:type ::hidden}))
             (.setStackTrace (.getStackTrace ex))))))

(defmethod print-method ::hidden [_ _])

(defn- explain-str [explain-data]
  (->> explain-data
       ::s/problems
       (mapcat (fn [{:keys [pred val in] :as problem}]
                 (cond
                   (and (sequential? pred)
                        (= `only-keys (first pred)))
                   (let [ks (set/difference (set (keys val)) (second pred))]
                     (for [k ks]
                       (assoc problem :val k :reason "unexpected prop")))

                   (and (sequential? pred)
                        (= `keys-satisfy (first pred)))
                   (if (map? val)
                     (let [k->spec (second pred)]
                       (for [[k spec-form] k->spec
                             :let [v (get val k ::not-found)]
                             :when (not= v ::not-found)
                             :let [spec (eval spec-form)]
                             problem (::s/problems (s/explain-data spec v))]
                         (update problem :in #(into (conj (or in []) k) %))))
                     [(assoc problem :reason 'map?)])

                   (= `valid-fx-type? pred)
                   (if (contains? val :fx/type)
                     [(-> problem
                          (update :val :fx/type)
                          (update :in conj :fx/type))]
                     [(assoc problem :reason "(contains? % :fx/type)")])


                   :else
                   [problem])))
       (map (fn [{:keys [pred val in reason]}]
              (str val
                   " - failed: "
                   (or reason (let [abbrev (s/abbrev pred)]
                                (cond-> abbrev (sequential? abbrev) pr-str)))
                   (when (seq in)
                     (str " in " in)))))
       (str/join "\n")))

(defn- ensure-valid-desc [desc fx-type type->lifecycle type->id]
  (binding [*type->lifecycle* type->lifecycle
            *type->id* type->id]
    (when-let [explain-data (s/explain-data :cljfx/desc (assoc desc :fx/type fx-type))]
      (throw (ex-info (str "Invalid cljfx description of " (type->string type->id fx-type) " type:\n"
                           (explain-str explain-data))
                      explain-data)))))

(defn- wrap-lifecycle [fx-type type->lifecycle type->id]
  (let [lifecycle (or (type->lifecycle fx-type) fx-type)]
    (reify lifecycle/Lifecycle
      (create [_ desc opts]
        (let [stack (conj (::stack opts) fx-type)
              opts (assoc opts ::stack stack)]
          (try
            (ensure-valid-desc desc fx-type type->lifecycle type->id)
            (lifecycle/create lifecycle desc opts)
            (catch Exception ex (re-throw-with-stack type->id ex stack)))))
      (advance [_ component desc opts]
        (let [stack (conj (::stack opts) fx-type)
              opts (assoc opts ::stack stack)]
          (try
            (ensure-valid-desc desc fx-type type->lifecycle type->id)
            (lifecycle/advance lifecycle component desc opts)
            (catch Exception ex (re-throw-with-stack type->id ex stack)))))
      (delete [_ component opts]
        (let [stack (conj (::stack opts) fx-type)
              opts (assoc opts ::stack stack)]
          (try
            (lifecycle/delete lifecycle component opts)
            (catch Exception ex (re-throw-with-stack type->id ex stack))))))))

;; Ensure there is state at top level
;; When root node is found, add listener (F12 by default) to open UI
;; Should be configurable if open by default or wait for root node to add listener (default)
;; inspector view:
;; 1. show tree of components
;; 2. for components, try to show props. by looking up something like :child* :props?