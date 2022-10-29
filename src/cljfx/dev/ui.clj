(in-ns 'cljfx.dev)

(require '[cljfx.ext.list-view :as fx.ext.list-view])

(defn- set-help-ui-selection [state {:keys [key fx/event]}]
  (update state key assoc :selection event))

(defn- set-help-ui-filter-term [state {:keys [key ^KeyEvent fx/event]}]
  (let [ch (.getCharacter event)]
    (cond
      (and (= 1 (count ch)) (#{127 27} (int (first ch))))
      (update state key assoc :filter-term "")

      (= ch "\b")
      (update-in state [key :filter-term] #(cond-> % (pos? (count %)) (subs 0 (dec (count %)))))

      (= ch "\t")
      state

      (re-matches #"^[a-zA-Z0-9:/\-.+!@#$%^&*()-={}\[\]<>?,/\\'\"]$" ch)
      (update-in state [key :filter-term] str (.getCharacter event))

      :else
      state)))

(defn- help-list-view [{:keys [filter-term selection items key]}]
  {:fx/type :stack-pane
   :min-width 150
   :max-width 150
   :children [{:fx/type fx.ext.list-view/with-selection-props
               :props {:selected-item selection
                       :on-selected-item-changed {:fn #'set-help-ui-selection :key key}}
               :desc {:fx/type :list-view
                      :on-key-typed {:fn #'set-help-ui-filter-term :key key}
                      :items items}}
              {:fx/type :label
               :visible (boolean (seq filter-term))
               :style-class ["label" "filter-term"]
               :stack-pane/margin 7
               :stack-pane/alignment :bottom-right
               :text filter-term}]})

(defn- process-filter-selection [{:keys [filter-term selection] :as m} items]
  (let [items (cond->> items
                       (not (str/blank? filter-term))
                       (filter #(str/includes? % filter-term)))
        items (sort-by str items)
        selection (or (and selection (some #{selection} items))
                      (first items))]
    (assoc m :items items :selection selection)))

(def ^:private ext-recreate-on-key-changed
  (reify lifecycle/Lifecycle
    (create [_ {:keys [key desc]} opts]
      (with-meta {:key key
                  :child (lifecycle/create lifecycle/dynamic desc opts)}
                 {`component/instance #(-> % :child component/instance)}))
    (advance [this component {:keys [key desc] :as this-desc} opts]
      (if (= (:key component) key)
        (update component :child #(lifecycle/advance lifecycle/dynamic % desc opts))
        (do (lifecycle/delete this component opts)
            (lifecycle/create this this-desc opts))))
    (delete [_ component opts]
      (lifecycle/delete lifecycle/dynamic (:child component) opts))))

(def ^:private ext-with-shown-on
  (fx/make-ext-with-props
    {:shown-on (prop/make
                 (mutator/adder-remover
                   (fn [^Popup popup ^Node node]
                     (let [bounds (.getBoundsInLocal node)
                           node-pos (.localToScreen node
                                                    -8
                                                    (- (.getHeight bounds) 6))]
                       (.show popup node
                              (.getX node-pos)
                              (.getY node-pos))))
                   (fn [^Popup popup _]
                     (.hide popup)))
                 lifecycle/dynamic)}))

(defn- hover-help-syntax-element [state {:keys [path key]}]
  (assoc state key path))

(defn- hide-hover-help-popup [state {:keys [key]}]
  (update state key pop))

(defn- help-ui-syntax-view [{:keys [syntax key hover] :as props}]
  (letfn [(apply-syntax [syntax path]
            (cond
              (string? syntax)
              [{:fx/type :text
                :font {:family "monospace" :size 14}
                :text syntax}]

              (vector? syntax)
              (map-indexed
                (fn [i x]
                  (cond
                    (string? x)
                    {:fx/type :text
                     :font {:family "monospace" :size 14}
                     :text x}

                    (vector? x)
                    (let [[text syntax] x
                          current-path (conj path i)]
                      {:fx/type fx/ext-let-refs
                       :refs
                       {::view
                        {:fx/type :label
                         :underline true
                         :font {:family "monospace" :size 14}
                         :text-fill "#000e26"
                         :on-mouse-entered
                         {:fn #'hover-help-syntax-element
                          :path current-path
                          :key key}
                         :text text}}
                       :desc
                       {:fx/type fx/ext-let-refs
                        :refs
                        {::popup
                         {:fx/type ext-with-shown-on
                          :props (if (= current-path (take (count current-path) hover))
                                   {:shown-on
                                    {:fx/type fx/ext-get-ref
                                     :ref ::view}}
                                   {})
                          :desc {:fx/type :popup
                                 :anchor-location :window-top-left
                                 :auto-hide true
                                 :hide-on-escape true
                                 :on-auto-hide {:fn #'hide-hover-help-popup
                                                :key key}
                                 :content [{:fx/type :stack-pane
                                            :max-width 960
                                            :pref-height :use-computed-size
                                            :max-height 600
                                            :style-class "popup-root"
                                            :children
                                            [{:fx/type :scroll-pane
                                              :content {:fx/type :text-flow
                                                        :padding 5
                                                        :children (apply-syntax syntax current-path)}}]}]}}}
                        :desc {:fx/type fx/ext-get-ref :ref ::view}}})

                    :else
                    (throw (ex-info "Invalid syntax" {:syntax x}))))

                syntax)

              :else
              (throw (ex-info "Invalid syntax" {:syntax syntax}))))]
   (-> props
       (dissoc :syntax :key :hover)
       (assoc
         :fx/type :scroll-pane
         :content
         {:fx/type :text-flow
          :padding 5
          :children (apply-syntax syntax [])}))))

(defn- help-ui-view [{:keys [registry type prop type-hover prop-hover]}]
  (let [filtered-type-map (process-filter-selection type (keys (:types registry)))
        selected-type (:selection filtered-type-map)
        selected-props (-> registry :props (get selected-type))
        filtered-prop-map (process-filter-selection prop (keys selected-props))
        selected-prop-id (:selection filtered-prop-map)
        selected-prop (get selected-props selected-prop-id)
        javadoc (get-in registry [:javadoc selected-type])]
    {:fx/type :stage
     :showing true
     :width 900
     :scene
     {:fx/type :scene
      :stylesheets [(::css/url help-ui-css)]
      :root {:fx/type :grid-pane
             :style {:-fx-background-color "#ccc"}
             :column-constraints [{:fx/type :column-constraints
                                   :min-width 150
                                   :max-width 150}
                                  {:fx/type :column-constraints
                                   :hgrow :always}]
             :row-constraints [{:fx/type :row-constraints
                                :min-height 100
                                :max-height 100}
                               {:fx/type :row-constraints
                                :vgrow :always}]
             :children [(assoc filtered-type-map
                          :grid-pane/column 0
                          :grid-pane/row 0
                          :grid-pane/row-span 2
                          :fx/type help-list-view
                          :key :type)
                        {:fx/type help-ui-syntax-view
                         :grid-pane/column 1
                         :grid-pane/row 0
                         :key :type-hover
                         :hover type-hover
                         :syntax (if selected-type
                                   (let [type-map (get (:types registry) selected-type)]
                                     (str "Cljfx type: " selected-type
                                          (when (symbol? (:of type-map))
                                            (str "\nInstance class: " (:of type-map)))
                                          (when (:req type-map)
                                            (if (set? (:req type-map))
                                              (str "\nRequired props, either:\n"
                                                   (str/join "\n" (for [req (:req type-map)]
                                                                    (str "- " (str/join ", " (sort req))))))))
                                          (when (and (not selected-props) (:spec type-map))
                                            (str "\nSpec: " (pr-str (s/describe (:spec type-map)))))))
                                   "")}
                        {:fx/type :tab-pane
                         :grid-pane/row 1
                         :grid-pane/column 1
                         :tabs
                         [{:fx/type :tab
                           :text "Props"
                           :closable false
                           :content {:fx/type :h-box
                                     :children [{:fx/type ext-recreate-on-key-changed
                                                 :key selected-type
                                                 :desc (assoc filtered-prop-map :fx/type help-list-view :key :prop)}
                                                {:fx/type help-ui-syntax-view
                                                 :h-box/hgrow :always
                                                 :key :prop-hover
                                                 :hover prop-hover
                                                 :syntax (if selected-prop
                                                           (long-prop-help-syntax selected-prop)
                                                           "")}]}}
                          {:fx/type :tab
                           :text "Javadoc"
                           :disable (nil? javadoc)
                           :closable false
                           :content {:fx/type :web-view
                                     :url (str javadoc-prefix javadoc)}}]}]}}}))

(defn- launch-help-ui! []
  (let [state (atom {:registry @registry
                     :type {:selection nil
                            :filter-term ""}
                     :prop {:selection nil
                            :filter-term ""}})
        render (fx/create-renderer
                 :opts {:fx.opt/type->lifecycle type->lifecycle
                        :fx.opt/map-event-handler #(swap! state (:fn %) %)}
                 :middleware (fx/wrap-map-desc #(assoc % :fx/type help-ui-view)))]
    (fx/mount-renderer state render)))
