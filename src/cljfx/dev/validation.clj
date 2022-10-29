(in-ns 'cljfx.dev)

(require '[cljfx.component :as component]
         '[cljfx.coerce :as coerce]
         '[cljfx.fx.parent :as fx.parent]
         '[cljfx.prop :as prop]
         '[cljfx.mutator :as mutator]
         '[cljfx.css :as css]
         '[cljfx.ext.tree-view :as ext.tree-view])
(import '[javafx.scene Scene Node]
        '[javafx.event EventDispatcher]
        '[javafx.stage Popup]
        '[javafx.scene.text Text]
        '[javafx.scene.input KeyEvent Clipboard ClipboardContent]
        '[javafx.scene.control Label TreeItem TableView TablePosition])

(def ^:private help-ui-css
  (let [primary-color "#4E84E0"]
    (css/register ::css
      {"*" {:-fx-font-size 13 :-fx-font-family "system"}
       ".list-view"
       {:-fx-background-color :transparent
        :-fx-border-width [0 1 0 0]
        :-fx-border-color "#aaa"
        ":focused > .virtual-flow > .clipped-container > .sheet > .list-cell:focused"
        {:-fx-background-color primary-color}}
       ".tab-pane:focused > .tab-header-area > .headers-region > .tab:selected .focus-indicator"
       {:-fx-border-color primary-color
        :-fx-border-width 2
        :-fx-border-insets [-4 -4 -15 -5]
        :-fx-border-radius "5"}
       ".tab" {:-fx-background-color "#aaa, #c2c2c2"
               :-fx-background-radius "6 6 0 0, 5 5 0 0"
               ":selected" {:-fx-background-color "#aaa, #ccc"}}
       ".tab-header-background" {:-fx-background-color "#aaa, #ccc, #ccc"}
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
       ".corner" {:-fx-background-color :transparent}
       ".tree-view" {:-fx-background-color :transparent
                     :-fx-fixed-cell-size 24
                     ":focused > .virtual-flow > .clipped-container > .sheet > .tree-cell:focused"
                     {:-fx-background-color primary-color}}
       ".tree-cell" {:-fx-background-color :transparent
                     :-fx-text-fill "#000"}
       ".inspector" {"-backdrop" {:-fx-background-color "#ccc"
                                  :-fx-effect "dropshadow(gaussian, #0006, 10, 0, 0, 5)"}
                     "-root" {:-fx-pref-width 300
                              :-fx-max-width 300
                              :-fx-pref-height 500
                              :-fx-max-height 500
                              :-fx-background-color :transparent}}
       ".table" {"-view" {:-fx-background-color :transparent
                          :-fx-fixed-cell-size 25
                          :-fx-border-color ["#aaa" :transparent :transparent :transparent]
                          "> .column-header-background"
                          {:-fx-background-color :transparent
                           ">.filler" {:-fx-background-color :transparent}}
                          ":focused > .virtual-flow > .clipped-container > .sheet > .table-row-cell > .table-cell:selected"
                          {:-fx-background-color primary-color}}
                 "-cell" {:-fx-border-color [:transparent "#aaa" :transparent :transparent]
                          :-fx-text-fill "#000"
                          ":focused" {:-fx-background-color "#4E84E033"}}
                 "-column" {:-fx-background-color :transparent
                            :-fx-border-color [:transparent "#aaa" :transparent :transparent]
                            " .label" {:-fx-alignment :center-left :-fx-text-fill "#000"}}
                 "-row-cell" {:-fx-background-color :transparent}}})))

(defn- type->string [type->id fx-type]
  (str (or (type->id fx-type) fx-type)))

(defn- re-throw-with-stack [type->id ^Throwable ex stack]
  (if (::cause (ex-data ex))
    (throw ex)
    (throw (doto (ex-info
                   (str (ex-message ex)
                        "\n\nCljfx component stack:\n  "
                        (->> stack
                             (map :type)
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

(def ^:private ext-value-lifecycle
  (reify lifecycle/Lifecycle
    (create [_ {:keys [value]} _] value)
    (advance [_ _ {:keys [value]} _] value)
    (delete [_ _ _] nil)))

(def ^:private ext-with-parent-props
  (fx/make-ext-with-props fx.parent/props))

(defn- inspector-handle-root-event [state {:keys [fx/event shortcut]}]
  (if (instance? KeyEvent event)
    (let [^KeyEvent event event]
      (if (= KeyEvent/KEY_PRESSED (.getEventType event))
        (if (.match ^KeyCombination shortcut event)
          (update state :showing not)
          state)
        state))
    state))

(defn- inspector-hide-popup [state _]
  (assoc state :showing false))

(def ^:private ext-with-popup-on-props
  (fx/make-ext-with-props
    {:on (prop/make
           (mutator/adder-remover
             (fn [^Popup popup ^Node node]
               (let [p (.localToScreen node 0.0 0.0)]
                 (.show popup node (.getX p) (- (.getY p) 5.0))))
             (fn [^Popup popup _]
               (.hide popup)))
           lifecycle/dynamic)}))

(defn- inspector-popup-view [{:keys [on desc] :as props}]
  {:fx/type fx/ext-let-refs
   :refs {::desc desc}
   :desc {:fx/type fx/ext-let-refs
          :refs {::popup {:fx/type ext-with-popup-on-props
                          :props (when on {:on on})
                          :desc (-> props
                                    (assoc :fx/type :popup)
                                    (dissoc :on :desc))}}
          :desc {:fx/type fx/ext-get-ref
                 :ref ::desc}}})

(defn- make-inspector-tree-item [path [{:keys [type] :as info} {:keys [children component]}] type->id]
  (let [path (conj path info)]
    {:fx/type :tree-item
     :expanded true
     :value {:type (or (type->id type) type)
             :component component
             :path path}
     :children (mapv #(make-inspector-tree-item path % type->id) children)}))

(defn- inspector-tree-cell [{:keys [type component]}]
  (let [instance (component/instance component)
        text (not-empty
               (condp instance? instance
                 Label (.getText ^Label instance)
                 Text (.getText ^Text instance)
                 nil))]
    {:text (str type (when text (str " - " (pr-str text))))}))

(defn on-inspector-tree-view-selection-changed [state {:keys [^TreeItem fx/event]}]
  (if event
    (assoc state :path (subvec (:path (.getValue event)) 1))
    (dissoc state :path)))

(defn- initialize-inspector-table! [^TableView table]
  (.selectFirst (.getSelectionModel table))
  (.addListener (.getItems table)
                (reify javafx.collections.ListChangeListener
                  (onChanged [_ _]
                    (.selectFirst (.getSelectionModel table)))))
  (.setCellSelectionEnabled (.getSelectionModel table) true))

(defn- inspector-cell-item->str [x]
  (let [x (if (-> x meta (contains? `component/instance))
            '...
            x)]
    (binding [*print-level* 5
              *print-length* 10]
      (pr-str x))))

(defn- on-inspector-table-key-pressed [^KeyEvent e]
  (when (and (.isShortcutDown e) (= KeyCode/C (.getCode e)))
    (.consume e)
    (let [^TableView table (.getTarget e)
          ^TablePosition pos (first (.getSelectedCells (.getSelectionModel table)))]
      (.setContent
        (Clipboard/getSystemClipboard)
        (doto (ClipboardContent.)
          (.putString (inspector-cell-item->str (.getCellData (.getTableColumn pos) (.getRow pos)))))))))

(defn- inspector-component-view [{:keys [component]}]
  (let [ext-with-props? (every-pred :child :props :props-lifecycle)
        component->props
        (fn [component]
          (cond
            ;; fn component?
            (and (:child-desc component) (:desc component) (:child component))
            (dissoc (:desc component) :fx/type)

            ;; composite component?
            (and (:props component) (:instance component))
            (:props component)

            ;; ext-let-refs?
            (and (:refs component) (:child component))
            (:refs component)

            ;; ext-with-props?
            (ext-with-props? component)
            (->> component
                 (iterate #(when (ext-with-props? %) (:child %)))
                 (take-while some?)
                 (map :props)
                 (apply merge))

            :else
            component))]
    {:fx/type fx/ext-on-instance-lifecycle
     :on-created initialize-inspector-table!
     :desc {:fx/type :table-view
            :on-key-pressed on-inspector-table-key-pressed
            :columns [{:fx/type :table-column
                       :text "prop"
                       :cell-value-factory key
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x]
                                                  {:text (str x)})}}
                      {:fx/type :table-column
                       :text "value"
                       :cell-value-factory val
                       :cell-factory {:fx/cell-type :table-cell
                                      :describe (fn [x]
                                                  {:text (inspector-cell-item->str x)})}}]
            :items (vec (component->props component))}}))

(defn- initialize-inspector-tree-view! [^Node tree-view]
  (let [dispatcher (.getEventDispatcher tree-view)]
    (.setEventDispatcher tree-view
                         (reify EventDispatcher
                           (dispatchEvent [_ e next]
                             (if (and (instance? KeyEvent e)
                                      (= KeyEvent/KEY_PRESSED (.getEventType e))
                                      (#{KeyCode/ESCAPE KeyCode/SPACE} (.getCode ^KeyEvent e)))
                               e
                               (.dispatchEvent dispatcher e next)))))))

(defn- inspector-root-view [{:keys [components showing type->id path inspector-shortcut]}]
  (let [^Scene scene (->> components
                          (tree-seq :children #(vals (:children %)))
                          (keep :component)
                          (map component/instance)
                          (some #(when (instance? Scene %) %)))
        root (some-> scene .getRoot)
        shortcut (coerce/key-combination inspector-shortcut)]
    (if root
      {:fx/type ext-with-parent-props
       :props {:event-filter {:fn #'inspector-handle-root-event
                              :shortcut shortcut}}
       :desc (cond-> {:fx/type inspector-popup-view
                      :auto-hide true
                      :hide-on-escape true
                      :consume-auto-hiding-events true
                      :on-auto-hide {:fn #'inspector-hide-popup}
                      :anchor-location :window-top-right
                      :desc {:fx/type ext-value-lifecycle
                             :value root}
                      :content
                        [{:fx/type :stack-pane
                          :style-class "inspector-root"
                          :stylesheets [(::css/url help-ui-css)]
                          :children
                            [{:fx/type :region
                              :style-class "inspector-backdrop"}
                             {:fx/type :grid-pane
                              :column-constraints [{:fx/type :column-constraints
                                                    :hgrow :always}]
                              :row-constraints [{:fx/type :row-constraints
                                                 :percent-height 70}
                                                {:fx/type :row-constraints
                                                 :percent-height 30}]
                              :children
                              [{:fx/type ext.tree-view/with-selection-props
                                :grid-pane/row 0
                                :props {:on-selected-item-changed {:fn #'on-inspector-tree-view-selection-changed}}
                                :desc {:fx/type fx/ext-on-instance-lifecycle
                                       :on-created initialize-inspector-tree-view!
                                       :desc {:fx/type :tree-view
                                              :cell-factory {:fx/cell-type :tree-cell
                                                             :describe #'inspector-tree-cell}
                                              :show-root false
                                              :root (make-inspector-tree-item
                                                      []
                                                      [{:type :root} components]
                                                      type->id)}}}
                               {:fx/type inspector-component-view
                                :grid-pane/row 1
                                :component (or (:component (get-in components (interleave (repeat :children) path)))
                                               (-> components :children vals first :component))}]}]}]}


               showing
               (assoc :on {:fx/type ext-value-lifecycle
                           :value root}))}
      {:fx/type :region})))

(defn- init-state! [state]
  (let [r (fx/create-renderer
            :opts {:fx.opt/map-event-handler #(swap! state (:fn %) %)}
            :middleware (fx/wrap-map-desc #(assoc % :fx/type inspector-root-view)))]
    (fx/mount-renderer state r)))

(defn- update-in-if-exists [m k f & args]
  (let [x (get-in m k ::not-found)]
    (if (identical? x ::not-found)
      m
      (apply update-in m k f args))))

(defn- wrap-lifecycle [fx-type type->lifecycle type->id inspector-shortcut]
  (let [lifecycle (or (type->lifecycle fx-type) fx-type)]
    (reify lifecycle/Lifecycle
      (create [_ desc opts]
        (let [component-info {:id (gensym "component") :type fx-type}
              old-stack (::stack opts)
              stack (conj old-stack component-info)
              state (or (::state opts) (doto (atom {:components {}
                                                    :inspector-shortcut inspector-shortcut
                                                    :type->id type->id}) init-state!))
              opts (assoc opts ::stack stack ::state state)]
          (try
            (ensure-valid-desc desc fx-type type->lifecycle type->id)
            (let [child (lifecycle/create lifecycle desc opts)]
              (swap! state
                     update-in (interpose :children (cons :components (reverse stack)))
                     assoc :component child)
              (with-meta {:child child :info component-info :state state}
                         {`component/instance #(-> % :child component/instance)}))
            (catch Exception ex (re-throw-with-stack type->id ex stack)))))
      (advance [_ component desc opts]
        (let [state (:state component)
              stack (conj (::stack opts) (:info component))
              opts (assoc opts ::stack stack ::state state)]
          (try
            (ensure-valid-desc desc fx-type type->lifecycle type->id)
            (let [child (lifecycle/advance lifecycle (:child component) desc opts)]
              (swap! state
                     update-in (interpose :children (cons :components (reverse stack)))
                     assoc :component child)
              (assoc component :child child))
            (catch Exception ex (re-throw-with-stack type->id ex stack)))))
      (delete [_ component opts]
        (let [state (:state component)
              old-stack (::stack opts)
              stack (conj old-stack (:info component))
              opts (assoc opts ::stack stack ::state state)]
          (swap! state
                 update-in-if-exists (interpose :children (cons :components (reverse old-stack)))
                 update :children dissoc (:info component))
          (try
            (lifecycle/delete lifecycle (:child component) opts)
            (catch Exception ex (re-throw-with-stack type->id ex stack))))))))
