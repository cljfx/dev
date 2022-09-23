(in-ns 'cljfx.dev)

(defn- type->string [fx-type]
  (if (fn? fx-type)
    (-> fx-type class .getName Compiler/demunge)
    fx-type))

(defn- re-throw-with-stack [^Throwable ex stack]
  (if (::cause (ex-data ex))
    (throw ex)
    (throw (doto (ex-info
                   (str (ex-message ex)
                        "\n\nCljfx component stack:\n  "
                        (->> stack (map type->string) (str/join "\n  ")))
                   {::cause ex})
             (.setStackTrace (.getStackTrace ex))))))

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

(defn- ensure-valid-desc [desc fx-type]
  (when-let [explain-data (s/explain-data :cljfx/desc (assoc desc :fx/type fx-type))]
    (throw (ex-info (str "Invalid cljfx description of " (type->string fx-type) " type:\n"
                         (explain-str explain-data))
                    explain-data))))

(defn- wrap-lifecycle [fx-type lifecycle]
  (reify lifecycle/Lifecycle
    (create [_ desc opts]
      (let [stack (conj (::stack opts) fx-type)
            opts (assoc opts ::stack stack)]
        (try
          (ensure-valid-desc desc fx-type)
          (lifecycle/create lifecycle desc opts)
          (catch Exception ex (re-throw-with-stack ex stack)))))
    (advance [_ component desc opts]
      (let [stack (conj (::stack opts) fx-type)
            opts (assoc opts ::stack stack)]
        (try
          (ensure-valid-desc desc fx-type)
          (lifecycle/advance lifecycle component desc opts)
          (catch Exception ex (re-throw-with-stack ex stack)))))
    (delete [_ component opts]
      (let [stack (conj (::stack opts) fx-type)
            opts (assoc opts ::stack stack)]
        (try
          (lifecycle/delete lifecycle component opts)
          (catch Exception ex (re-throw-with-stack ex stack))))
      (lifecycle/delete lifecycle component opts))))