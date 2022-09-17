(in-ns 'cljfx.dev)

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
(defmethod short-keyword-prop-help-string :pseudo-classes [_]
  "a set of keywords or instances of javafx.css.PseudoClass")

(def ^:private superscript (zipmap "0123456789" "⁰¹²³⁴⁵⁶⁷⁸⁹"))

(defn- indent
  [s & {:keys [by start]
        :or {start true
             by 2}}]
  (let [indent-str (str/join (repeat by \space))]
    (cond->> (str/join (str "\n" indent-str)
                       (str/split-lines s))
             start (str indent-str))))

(defn- convert-help-syntax-to-string [syntax]
  (letfn [(apply-syntax [footnotes syntax]
            (cond
              (string? syntax)
              syntax

              (vector? syntax)
              (reduce (fn [acc syntax]
                        (cond
                          (string? syntax)
                          (str acc syntax)

                          (vector? syntax)
                          (let [[text] syntax]
                            (let [existing-footnote (first
                                                      (keep-indexed
                                                        (fn [i v]
                                                          (when (= syntax v)
                                                            i))
                                                        @footnotes))
                                  n (if existing-footnote
                                      (inc existing-footnote)
                                      (count (vswap! footnotes conj syntax)))]
                              (str acc text (str/join (map superscript (str n))))))

                          :else
                          (throw (ex-info "Invalid syntax" {:syntax syntax}))))
                      ""
                      syntax)

              :else
              (throw (ex-info "Invalid syntax" {:syntax syntax}))))]
    (let [footnotes (volatile! [])
          initial (cond-> (apply-syntax footnotes syntax)
                    (pos? (count @footnotes)) (str "\n\n---"))]
      (loop [i 0
             acc initial]
        (if (= i (count @footnotes))
          acc
          (let [[text syntax] (@footnotes i)]
            (recur
              (inc i)
              (str acc
                   "\n"
                   (str/join (map superscript (str (inc i))))
                   text
                   " - "
                   (indent (apply-syntax footnotes syntax) :by (count (str i)) :start false)
                   "\n"))))))))

(defmethod long-keyword-prop-help-syntax :enum [{:keys [of]}]
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
(defmethod long-keyword-prop-help-syntax :insets [_]
  "Insets, either:
- number
- map with optional keys - :top, :bottom, :left, :right - numbers
- literal - :empty
- instance of javafx.geometry.Insets")
(defmethod long-keyword-prop-help-syntax :image [_]
  "Image, either:
- instance of javafx.scene.image.Image
- string: either a resource path or URL string pointing to image
- map with required :url (url string) or :is (input stream) keys and optional :requested-width (number), :requested-height (number), :preserve-ratio (boolean), :smooth (boolean) and :background-loading (boolean) keys")
(defmethod long-keyword-prop-help-syntax :duration [_]
  "Duration, either:
- tuple of number and time unit (:ms, :s, :m, or :h), e.g. [10 :s]
- string in the format [number][ms|s|m|h], e.g. 10s
- number (ms)
- literal - :zero, :one (ms), :indefinite or :unknown
- instance of javafx.util.Duration")
(defmethod long-keyword-prop-help-syntax :font [_]
  ["Font, either:
- string, font family name
- number, font size (of a default font)
- literal - :default
- map with required :family key (string) and optional "
   [":weight" (long-prop-help-syntax '{:type :enum :of javafx.scene.text.FontWeight})]
   ", "
   [":posture" (long-prop-help-syntax '{:type :enum :of javafx.scene.text.FontPosture})]
   " and :size (number) keys
- instance of javafx.scene.text.Font"])

(defmethod long-keyword-prop-help-syntax :color [_]
  (str "A color, either:
- string in either format:
  - 0x0000FF - 0x-prefixed hex web value
  - 0x00F - 0x-prefixed short hex web value
  - #0000FF - #-prefixed hex web value
  - #00F - #-prefixed short hex web value
  - 0000FF - hex web value
  - 00F - short hex web value
  - rgba(0,0,255,1.0) - rgb web value, explicit alpha
  - rgb(0,0,255) - rgb web value, implicit alpha
  - rgba(0,0,100%,1.0) - rgb percent web value, explicit alpha
  - rgb(0,0,100%) - rgb percent web value, implicit alpha
  - hsla(270,100%,100%,1.0) - hsl web value, explicit alpha
  - hsl(270,100%,100%) - hsl web value, implicit alpha
- instance of javafx.scene.paint.Color
- keyword of a named color:
  - " (-> Color
          .getDeclaredClasses
          ^Class first
          (.getDeclaredField "NAMED_COLORS")
          (doto (.setAccessible true))
          (.get nil)
          (keys)
          (->> (map keyword)
               sort
               (str/join "\n  - ")))))

(defmethod long-keyword-prop-help-syntax :key-combination [_]
  ["Key combination, either:
- a vector of modifier keywords + key identifier, where:
  - modifier keyword is either :shift, :ctrl, :alt, :meta or :shortcut
  - key identifier is either "
   ["key code" (long-prop-help-syntax '{:type :enum :of javafx.scene.input.KeyCode})]
   " (creates KeyCodeCombination) or 1-character string (creates KeyCharacterCombination)
- string with +-interposed modifiers with key in the end, e.g. Shift+Ctrl+C
- instance of javafx.scene.input.KeyCombination"])

(defmethod long-keyword-prop-help-syntax :map [{:keys [key value]}]
  ["Map from "
   [(short-prop-help-string key)
    (long-prop-help-syntax key)]
   " to "
   [(short-prop-help-string value)
    (long-prop-help-syntax value)]])

(defmethod long-keyword-prop-help-syntax :event-handler [{:keys [of]}]
  (str "Event handler, either:
- a map event
- fn
- instance of " of))

(defmethod long-keyword-prop-help-syntax :float-map [_]
  "Float map, either:
- a map with optional keys :width (int), :height (int) and :samples (a coll of maps with keys :x - int, :y - int and :s - tuple of number and number)
- instance of javafx.scene.effect.FloatMap")

(defn- known-types-of [sym-or-cls]
  (let [cls (cond-> sym-or-cls (symbol? sym-or-cls) resolve)
        r @registry
        ts (->> r
                :types
                vals
                (filter (fn [{:keys [of]}]
                          (and (symbol? of)
                               (isa? (resolve of) cls))))
                (sort-by (comp str :id))
                seq)]
    ts))

(defn- str-table [items & columns]
  (let [columns (vec columns)
        column-strs (mapv #(into [(:label %)] (map (comp str (:fn %))) items) columns)
        max-lengths (mapv #(transduce (map count) max 0 %) column-strs)]
    (reduce
      (fn [acc i]
        (let [row (mapv #(% i) column-strs)]
          (str acc
               (str/join
                 "    "
                 (map (fn [max-length item]
                        (str item (str/join (repeat (- max-length (count item)) \space))))
                      max-lengths row))
               "\n")))
      ""
      (range (inc (count items))))))


(defn- splice-syntax [& syntaxes]
  (reduce #(into %1 (if (string? %2) [%2] %2)) [] syntaxes))

(defmethod long-keyword-prop-help-syntax :desc [{:keys [of]}]
  ["Cljfx desc, a map with :fx/type key

Required instance class:\n"
   (if-let [ts (known-types-of of)]
     [(str of) (str "Fitting cljfx types:\n" (str-table ts
                                                        {:label "Cljfx type" :fn :id}
                                                        {:label "Class" :fn :of}))]
     (str of))])

(defmethod long-keyword-prop-help-syntax :coll [{:keys [item]}]
  (splice-syntax
    (str "Coll of " (short-prop-help-string item) "\n\nItem:\n")
    (long-prop-help-syntax item)))

(defmethod long-keyword-prop-help-syntax :add-props [{:keys [props to]}]
  (splice-syntax
    (long-prop-help-syntax to)
    "\n\nExtra props:\n"
    (str-table
      (sort-by key props)
      {:label "Prop" :fn key}
      {:label "Value type" :fn (comp short-prop-help-string val)})))

(defmethod long-keyword-prop-help-syntax :point-3d [_]
  "Point3D, either:
- literal - :zero :x-axis :y-axis :z-axis
- map with required keys :x, :y and :z (numbers)
- instance of javafx.geometry.Point3D")

(defmethod long-keyword-prop-help-syntax :style-class [_]
  "Style class, either:
- a string
- a coll of strings")

(defmethod long-keyword-prop-help-syntax :style [_]
  "Style, either:
- a css-like style string, e.g. \"-fx-background-color: red; -fx-text-fill: green\"
- a map that will be converted to such string, where keys must by keywords and values can be either:
  - keywords (will use name)
  - vectors (will by interposed by spaces)
  - strings (will be used as is)
  Example: {:-fx-background-color :red :-fx-text-fill :green}")

(defmethod long-keyword-prop-help-syntax :cursor [_]
  "Cursor, either:
- string - either a resource path or URL string pointing to cursor image
- keyword - either:
  - :closed-hand
  - :crosshair
  - :default
  - :disappear
  - :e-resize
  - :h-resize
  - :hand
  - :move
  - :n-resize
  - :name
  - :ne-resize
  - :none
  - :nw-resize
  - :open-hand
  - :s-resize
  - :se-resize
  - :sw-resize
  - :text
  - :v-resize
  - :w-resize
  - :wait
- instance of javafx.scene.Cursor")

(defmethod long-keyword-prop-help-syntax :rectangle [_]
  "Rectangle, either:
- a map with required :min-x, :min-y, :width and :height keys (numbers)
- instance of javafx.geometry.Rectangle2D")

(defmethod long-keyword-prop-help-syntax :interpolator [_]
  ["Interpolator, either:
- literal - :discrete, :ease-both, :ease-in, :ease-out or :linear
- 3-element tuple of literal :tangent, " ["duration" (long-prop-help-syntax {:type :duration})]" and number
- 5-element tuple of literal :tangent, " ["duration" (long-prop-help-syntax {:type :duration})]", number, " ["duration" (long-prop-help-syntax {:type :duration})]" and number
- 5-element tuple of literal :spline and 4 numbers"])

(defmethod long-keyword-prop-help-syntax :chronology [_]
  "Chronology, either:
- literal - :iso, :hijrah, :japanese, :minguo or :thai-buddhist
- instance of java.time.chrono.Chronology")

(defmethod long-keyword-prop-help-syntax :vertex-format [_]
  "Vertex format, either:
- literal - :point-texcoord or :point-normal-texcoord
- instance of javafx.scene.shape.VertexFormat")

(defmethod long-keyword-prop-help-syntax :bounding-box [_]
  "Bounding box, either:
- literal - 0
- 4-element tuple of numbers (minX, minY, width, height)
- 6-element tuple of numbers (minX, minY, minZ, width, height, depth)
- instance of javafx.geometry.BoundingBox")

(defmethod long-keyword-prop-help-syntax :button-type [_]
  ["Button type, either:
- literal - :apply, :cancel, :close, :finish, :next, :no, :ok, :previous or :yes
- map with optional keys - :text (string) and "
   [":button-data" (long-keyword-prop-help-syntax '{:type :enum :of javafx.scene.control.ButtonBar$ButtonData})]
   "
- string - button type text
- instance of javafx.scene.control.ButtonType"])

(defmethod long-keyword-prop-help-syntax :table-sort-policy [_]
  "Table sort policy, either:
- literal - :default
- instance of javafx.util.Callback")

(defmethod long-keyword-prop-help-syntax :text-formatter-filter [_]
  "Text formatter filter, either:
- literal - nil
- fn from javafx.scene.control.TextFormatter$Change to javafx.scene.control.TextFormatter$Change
- instance of java.util.function.UnaryOperator<javafx.scene.control.TextFormatter$Change>")

(defmethod long-keyword-prop-help-syntax :text-formatter [_]
  ["Text formatter, either:
- instance of javafx.scene.control.TextFormatter
- " ["cljfx desc of javafx.scene.control.TextFormatter" (long-prop-help-syntax '{:type :desc :of javafx.scene.control.TextFormatter})]
   ""])

(defmethod long-keyword-prop-help-syntax :toggle-group [_]
  ["Toggle group, either:
- instance of javafx.scene.control.ToggleGroup
- " ["cljfx desc of javafx.scene.control.ToggleGroup" (long-prop-help-syntax '{:type :desc :of javafx.scene.control.ToggleGroup})]])

(defmethod long-keyword-prop-help-syntax :result-converter [_]
  "Result converter, either:
- fn from javafx.scene.control.ButtonType to any object
- instance of javafx.util.Callback<javafx.scene.control.ButtonType,?>")

(defmethod long-keyword-prop-help-syntax :column-resize-policy [_]
  "Column resize policy, either:
- literal - :unconstrained or :constrained
- predicate fn of javafx.scene.control.TableView$ResizeFeatures
- instance of javafx.util.Callback<javafx.scene.control.TableView$ResizeFeatures,boolean>")

(defmethod long-keyword-prop-help-syntax :string-converter [_]
  "String converter, either:
- literal - :big-decimal, :big-integer, :boolean, :byte, :character, :date-time, :default, :double, :float, :integer, :local-date, :local-date-time, :local-time, :long, :number or :short
- instance of javafx.util.StringConverter")

(defmethod long-keyword-prop-help-syntax :page-factory [_]
  ["Page factory, either:
- fn from page index to "
   ["cljfx desc of javafx.scene.Node" (long-keyword-prop-help-syntax '{:type :desc :of javafx.scene.Node})]
   "
- instance of javafx.util.Callback"])

(defmethod long-keyword-prop-help-syntax :cell-value-factory [_]
  "Cell value factory, either:
- fn that converts row value to cell value
- instance of javafx.util.Callback")

(defmethod long-keyword-prop-help-syntax :cell-factory [{:keys [of]}]
  (let [of-syntax (if-let [ts (known-types-of of)]
                    [(str of) (str "Fitting cljfx types:\n" (str-table ts
                                                                       {:label "Cljfx type" :fn :id}
                                                                       {:label "Class" :fn :of}))]
                    (str of))]
   ["Cell factory, either:
- a map with required :fx/cell-type and :describe keys, where:
  - :fx/cell-type is either a keyword or a composite lifecycle instance of " of-syntax "
  - :describe is fn from item to prop map (without :fx/type) of the :fx/cell-type type
- deprecated: fn from item to prop map (without :fx/type) of " of-syntax]))

(defmethod long-keyword-prop-help-syntax :paint [_]
  ["Paint, either:
- " ["color" (long-prop-help-syntax {:type :color})] "
- 2-element tuple of :linear-gradient and a map with required keys:
  - :start-x - double
  - :start-y - double
  - :end-x - double
  - :end-y - double
  - :proportional - boolean
  - :cycle method - " [(short-prop-help-string '{:type :enum :of javafx.scene.paint.CycleMethod})
                       (long-prop-help-syntax '{:type :enum :of javafx.scene.paint.CycleMethod})] "
  - :stops - a coll of 2-element tuples of double and " ["color" (long-prop-help-syntax {:type :color})] "
- 2-element tuple of :radial-gradient and a map with required keys:
  - :focus-angle - double
  - :focus-distance - double
  - :center-x - double
  - :center-y - double
  - :radius - double
  - :proportional - boolean
  - :cycle-method - "[(short-prop-help-string '{:type :enum :of javafx.scene.paint.CycleMethod})
                      (long-prop-help-syntax '{:type :enum :of javafx.scene.paint.CycleMethod})]"
- 2-element tuple of :image-pattern and a map with keys:
  - :image - " ["image" (long-prop-help-syntax '{:type :image})]", required
  - :x - double
  - :y - double
  - :width - double
  - :height - double
  - :proportional - boolean
- instance of javafx.scene.paint.Paint"])

(def ^:private radii-syntax
  ["Corner radii, either:
- literal :empty
- number - a corner radius
- a map with required either :radius (number) or both :top-left, :top-right, :bottom-right and :bottom-left keys (numbers), and optional :as-percent boolean key
- instance of javafx.scene.layout.CornerRadii"])

(def ^:private background-position-axis-syntax
  ["Background image position axis, a map with keys:
- :position - a number, required
- :side - " [(short-prop-help-string '{:type :enum :of javafx.geometry.Side}) (long-keyword-prop-help-syntax '{:type :enum :of javafx.geometry.Side})] "
- :as-percentage - boolean"])

(def ^:private background-position-syntax
  ["Background image position, either:
- literal - :center or :default
- a map with required :horizontal and :vertical keys (" ["background position axes" background-position-axis-syntax] ")
- instance of javafx.scene.layout.BackgroundPosition"])

(def ^:private background-size-syntax
  ["Background image size, either:
- literal - :auto or :default
- a map with keys:
  - :width - number, required
  - :height - number, required
  - :width-as-percentage - boolean
  - :height-as-percentage - boolean
  - :contain - boolean
  - :cover - boolean
- instance of javafx.scene.layout.BackgroundSize"])

(defmethod long-keyword-prop-help-syntax :background [_]
  (let [background-fills ["background fills" ["Background fill, either:
- map with optional " [":fill" (long-keyword-prop-help-syntax {:type :paint})] ", " [":radii" radii-syntax] " and " [":insets" (long-prop-help-syntax {:type :insets})] " keys
- instance of javafx.scene.layout.BackgroundFill"]]
        background-images ["background images" ["Background image is either:
- string: either a resource path or URL string pointing to image
- map with keys:
  - :image - " ["image" (long-prop-help-syntax {:type :image})] ", required
  - :repeat-x - "[(short-prop-help-string '{:type :enum :of javafx.scene.layout.BackgroundRepeat}) (long-prop-help-syntax '{:type :enum :of javafx.scene.layout.BackgroundRepeat})]"
  - :repeat-y - "[(short-prop-help-string '{:type :enum :of javafx.scene.layout.BackgroundRepeat}) (long-prop-help-syntax '{:type :enum :of javafx.scene.layout.BackgroundRepeat})]"
  - :position - " ["background position" background-position-syntax] "
  - :size - " ["background size" background-size-syntax] "
- instance of javafx.scene.layout.BackgroundImage"]]]
    ["Background, either:
- a map with optional keys:
  - :fills - a coll of " background-fills "
  - :images - a coll of " background-images "
- instance of javafx.scene.layout.Background"]))

(def ^:private border-stroke-style-syntax
  ["Border stroke style, either:
- literal - :dashed, :dotted, :none or :solid
- map with optional keys:
  - :type - " [(short-prop-help-string '{:type :enum :of javafx.scene.shape.StrokeType}) (long-prop-help-syntax '{:type :enum :of javafx.scene.shape.StrokeType})] "
  - :line-join - " [(short-prop-help-string '{:type :enum :of javafx.scene.shape.StrokeLineJoin}) (long-prop-help-syntax '{:type :enum :of javafx.scene.shape.StrokeLineJoin})] "
  - :line-cap - " [(short-prop-help-string '{:type :enum :of javafx.scene.shape.StrokeLineCap}) (long-prop-help-syntax '{:type :enum :of javafx.scene.shape.StrokeLineCap})] "
  - :miter-limit - number
  - :dash-offset - number
  - :dash-array - coll of numbers
- instance of javafx.scene.layout.BorderStrokeStyle"])

(def ^:private border-widths-syntax
  ["Border widths, either:
- literal - :auto, :default, :empty or :full
- number
- map with keys:
  - :top - number, required
  - :right - number, required
  - :bottom - number, required
  - :left - number, required
  - :as-percentage - boolean
  - :top-as-percentage - boolean
  - :right-as-percentage - boolean
  - :bottom-as-percentage - boolean
  - :left-as-percentage - boolean
- instance of javafx.scene.layout.BorderWidths"])

(def ^:private border-strokes-syntax
  ["A collection of border strokes, that are either:
- map with optional keys:
  - :stroke - " ["paint" (long-prop-help-syntax {:type :paint})] "
  - :style - " ["border stroke style" border-stroke-style-syntax] "
  - :radii - " ["corner radii" radii-syntax] "
  - :widths - " ["border widths" border-widths-syntax] "
  - :insets - " ["insets" (long-prop-help-syntax {:type :insets})] "
- instance of javafx.scene.layout.BorderStroke"])

(def ^:private border-images-syntax
  ["A collection of border images that are either:
- map with keys:
  - :image - " ["image" (long-prop-help-syntax {:type :image})] ", required
  - :widths - " ["border widths" border-widths-syntax] "
  - :insets - " ["insets" (long-prop-help-syntax {:type :insets})] "
  - :slices - " ["border widths" border-widths-syntax] "
  - :filled - boolean
  - :repeat-x - " [(short-prop-help-string '{:type :enum :of javafx.scene.layout.BorderRepeat}) (long-prop-help-syntax '{:type :enum :of javafx.scene.layout.BorderRepeat})] "
  - :repeat-y - " [(short-prop-help-string '{:type :enum :of javafx.scene.layout.BorderRepeat}) (long-prop-help-syntax '{:type :enum :of javafx.scene.layout.BorderRepeat})] "
- instance of javafx.scene.layout.BorderImage"])

(defmethod long-keyword-prop-help-syntax :border [_]
  ["Border, either:
- literal :empty
- a map with optional " [":strokes" border-strokes-syntax] " and " [":images" border-images-syntax] " keys
- instance of javafx.scene.layout.Border"])