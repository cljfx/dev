(in-ns 'cljfx.dev)
(import '[javafx.scene.input KeyEvent]
        '[javafx.stage Popup]
        '[javafx.scene Node])
(require '[cljfx.ext.list-view :as fx.ext.list-view]
         '[cljfx.prop :as prop]
         '[cljfx.mutator :as mutator]
         '[cljfx.component :as component]
         '[cljfx.css :as css])

(def ^:private help-ui-css
  (css/register ::css
    {".list-view"
       {:-fx-background-color :transparent
        :-fx-border-width [0 1 0 0]
        :-fx-border-color "#aaa"
        ":focused > .virtual-flow > .clipped-container > .sheet > .list-cell:focused"
          {:-fx-background-color "#4E84E0"}}
     ".popup-root" {:-fx-background-color "#ddd"
                    :-fx-effect "dropshadow(gaussian, #0006, 8, 0, 0, 2)"}
     ".filter-term" {:-fx-background-color "#42B300"
                     :-fx-background-radius 2
                     :-fx-effect "dropshadow(gaussian, #0006, 4, 0, 0, 2)"
                     :-fx-padding [2 4]}
     ".list-cell" {:-fx-background-color :transparent
                   :-fx-text-fill "#000"
                   :-fx-font-size 14
                   :-fx-padding [2 4]
                   ":selected" {:-fx-background-color "#4E84E033"}}
     ".text-area" {:-fx-background-color :transparent
                   :-fx-focus-traversable false
                   :-fx-text-fill "#000"
                   " .content" {:-fx-background-color :transparent}}
     ".scroll-pane" {:-fx-background-color :transparent
                     :-fx-padding 0
                     "> .viewport" {:-fx-background-color :transparent}}
     ".scroll-bar" {:-fx-background-color :transparent
                    "> .thumb" {:-fx-background-color "#999"
                                :-fx-background-insets 0
                                :-fx-background-radius 4
                                ":hover" {:-fx-background-color "#9c9c9c"}
                                ":pressed" {:-fx-background-color "#aaa"}}
                    ":horizontal" {"> .increment-button > .increment-arrow" {:-fx-pref-height 7}
                                   "> .decrement-button > .decrement-arrow" {:-fx-pref-height 7}}
                    ":vertical" {"> .increment-button > .increment-arrow" {:-fx-pref-width 7}
                                 "> .decrement-button > .decrement-arrow" {:-fx-pref-width 7}}
                    "> .decrement-button" {:-fx-padding 0
                                           "> .decrement-arrow" {:-fx-shape nil
                                                                 :-fx-padding 0}}
                    "> .increment-button" {:-fx-padding 0
                                           "> .increment-arrow" {:-fx-shape nil
                                                                 :-fx-padding 0}}}
     ".corner" {:-fx-background-color :transparent}}))

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
        selected-prop (get selected-props selected-prop-id)]
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
                         :style {:-fx-border-width [0 0 1 0]
                                 :-fx-border-color "#aaa"}
                         :grid-pane/column 1
                         :grid-pane/column-span 2
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
                        {:fx/type ext-recreate-on-key-changed
                         :grid-pane/row 1
                         :grid-pane/column 1
                         :key selected-type
                         :desc (assoc filtered-prop-map :fx/type help-list-view :key :prop)}
                        {:fx/type help-ui-syntax-view
                         :grid-pane/row 1
                         :grid-pane/column 2
                         :key :prop-hover
                         :hover prop-hover
                         :syntax (if selected-prop
                                   (long-prop-help-syntax selected-prop)
                                   "")}]}}}))

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
