(in-ns 'cljfx.dev)

(require '[clojure.string :as str])

(import
  '[javafx.scene.layout
    Background BackgroundImage BackgroundPosition BackgroundSize BackgroundFill
    CornerRadii Border BorderStroke BorderStrokeStyle BorderWidths BorderImage]
  '[javafx.scene.image Image]
  '[javafx.animation Interpolator]
  '[javafx.scene.control ButtonType TextFormatter ToggleGroup]
  '[java.io InputStream]
  '[java.time.chrono Chronology]
  '[java.util.function UnaryOperator]
  '[javafx.util Callback Duration StringConverter]
  '[javafx.scene.paint Paint Color]
  '[javafx.scene.text Font]
  '[javafx.scene.input KeyCombination KeyCode]
  '[javafx.scene.effect FloatMap]
  '[javafx.scene.shape VertexFormat]
  '[javafx.geometry Insets Point3D Side BoundingBox]
  '[javafx.scene Cursor])

(defmethod keyword-prop->spec-form :pref-or-computed-size-double [_]
  `(s/or :enum #{:use-computed-size :use-pref-size}
         :number number?))
(defmethod keyword-prop->spec-form :computed-size-double [_]
  `(s/or :enum #{:use-computed-size}
         :number number?))
(defmethod keyword-prop->spec-form :boolean [_] `boolean?)
(defmethod keyword-prop->spec-form :number [_] `number?)
(defmethod keyword-prop->spec-form :event-handler [{:keys [of]}]
  `(s/or :map-event-handler map?
         :fn fn?
         :instance (instance-of ~of)))
(defn- enum-sym->spec-form [of]
  `(s/or :keyword ~(into #{}
                         (map #(keyword (str/replace (str/lower-case (.name ^Enum %)) #"_" "-")))
                         (.getEnumConstants (resolve of)))
         :instance (instance-of ~of)))
(defmethod keyword-prop->spec-form :enum [{:keys [of]}]
  (enum-sym->spec-form of))
(defmethod keyword-prop->spec-form :desc [{:keys [of]}]
  `(s/and (s/nonconforming :cljfx/desc) (desc-of '~of)))
(defmethod keyword-prop->spec-form :string [_] `(s/spec string?))
(defmethod keyword-prop->spec-form :coll [{:keys [item]}]
  `(s/coll-of ~(prop->spec-form item)))
(s/def :cljfx.image/url string?)
(s/def :cljfx.image/is (instance-of InputStream))
(s/def :cljfx.image/requested-width number?)
(s/def :cljfx.image/requested-height number?)
(s/def :cljfx.image/preserve-ratio boolean?)
(s/def :cljfx.image/smooth boolean?)
(s/def :cljfx.image/background-loading boolean?)
(s/def :cljfx/image
  (s/or :instance (instance-of Image)
        :string string?
        :map (s/keys :req-un [(or :cljfx.image/url :cljfx.image/is)]
                     :opt-un [:cljfx.image/requested-width
                              :cljfx.image/requested-height
                              :cljfx.image/preserve-ratio
                              :cljfx.image/smooth
                              :cljfx.image/background-loading])))
(s/def :cljfx.background-image/image :cljfx/image)
(defmacro defenumspec [kw enum]
  `(s/def ~kw ~(enum-sym->spec-form enum)))
(defenumspec :cljfx.background-image/repeat javafx.scene.layout.BackgroundRepeat)
(s/def :cljfx.background-image/repeat-x :cljfx.background-image/repeat)
(s/def :cljfx.background-image/repeat-y :cljfx.background-image/repeat)
(s/def :cljfx.background-image.position.axis/position number?)
(s/def :cljfx.background-image.position.axis/as-percentage boolean?)
(defenumspec :cljfx.background-image.position.axis/side javafx.geometry.Side)
(s/def :cljfx.background-image.position/axis
  (s/keys :req-un [:cljfx.background-image.position.axis/position]
          :opt-un [:cljfx.background-image.position.axis/side
                   :cljfx.background-image.position.axis/as-percentage]))
(s/def :cljfx.background-image.position/horizontal :cljfx.background-image.position/axis)
(s/def :cljfx.background-image.position/vertical :cljfx.background-image.position/axis)
(s/def :cljfx.background-image/position
  (s/or :instance (instance-of BackgroundPosition)
        :const #{:center :default}
        :map (s/keys :req-un [:cljfx.background-image.position/horizontal
                              :cljfx.background-image.position/vertical])))
(s/def :cljfx.background-image.size/width number?)
(s/def :cljfx.background-image.size/height number?)
(s/def :cljfx.background-image.size/width-as-percentage boolean?)
(s/def :cljfx.background-image.size/height-as-percentage boolean?)
(s/def :cljfx.background-image.size/contain boolean?)
(s/def :cljfx.background-image.size/cover boolean?)
(s/def :cljfx.background-image/size
  (s/or :instance (instance-of BackgroundSize)
        :const #{:auto :default}
        :map (s/keys :req-un [:cljfx.background-image.size/width
                              :cljfx.background-image.size/height]
                     :opt-un [:cljfx.background-image.size/width-as-percentage
                              :cljfx.background-image.size/height-as-percentage
                              :cljfx.background-image.size/contain
                              :cljfx.background-image.size/cover])))
(s/def :cljfx/background-image
  (s/or :instance (instance-of BackgroundImage)
        :string string?
        :map (s/keys :opt-un [:cljfx.background-image/image
                              :cljfx.background-image/repeat-x
                              :cljfx.background-image/repeat-y
                              :cljfx.background-image/position
                              :cljfx.background-image/size])))
(s/def :cljfx.background/images
  (s/coll-of :cljfx/background-image))
(s/def :cljfx.paint.linear-gradient/start-x double?)
(s/def :cljfx.paint.linear-gradient/start-y double?)
(s/def :cljfx.paint.linear-gradient/end-x double?)
(s/def :cljfx.paint.linear-gradient/end-y double?)
(s/def :cljfx.paint/proportional boolean?)
(defenumspec :cljfx.paint.gradient/cycle-method javafx.scene.paint.CycleMethod)
(s/def :cljfx/color
  (s/or :instance (instance-of Color)
        :keyword keyword?
        :string string?))
(s/def :cljfx.paint.gradient/stops
  (s/coll-of (s/tuple double? :cljfx/color)))
(s/def :cljfx.paint.radial-gradient/focus-angle double?)
(s/def :cljfx.paint.radial-gradient/focus-distance double?)
(s/def :cljfx.paint.radial-gradient/center-x double?)
(s/def :cljfx.paint.radial-gradient/center-y double?)
(s/def :cljfx.paint.radial-gradient/radius double?)
(s/def :cljfx.paint.image-pattern/image :cljfx/image)
(s/def :cljfx.paint.image-pattern/x double?)
(s/def :cljfx.paint.image-pattern/y double?)
(s/def :cljfx.paint.image-pattern/width double?)
(s/def :cljfx.paint.image-pattern/height double?)
(s/def :cljfx/paint
  (s/or :instance (instance-of Paint)
        :keyword keyword?
        :string string?
        :linear-gradient (s/tuple #{:linear-gradient}
                                  (s/keys :req-un [:cljfx.paint.linear-gradient/start-x
                                                   :cljfx.paint.linear-gradient/start-y
                                                   :cljfx.paint.linear-gradient/end-x
                                                   :cljfx.paint.linear-gradient/end-y
                                                   :cljfx.paint/proportional
                                                   :cljfx.paint.gradient/cycle-method
                                                   :cljfx.paint.gradient/stops]))
        :radial-gradient (s/tuple #{:radial-gradient}
                                  (s/keys :req-un [:cljfx.paint.radial-gradient/focus-angle
                                                   :cljfx.paint.radial-gradient/focus-distance
                                                   :cljfx.paint.radial-gradient/center-x
                                                   :cljfx.paint.radial-gradient/center-y
                                                   :cljfx.paint.radial-gradient/radius
                                                   :cljfx.paint/proportional
                                                   :cljfx.paint.gradient/cycle-method
                                                   :cljfx.paint.gradient/stops]))
        :image-pattern (s/tuple #{:image-pattern}
                                (s/keys :req-un [:cljfx.paint.image-pattern/image]
                                        :opt-un [:cljfx.paint.image-pattern/x
                                                 :cljfx.paint.image-pattern/y
                                                 :cljfx.paint.image-pattern/width
                                                 :cljfx.paint.image-pattern/height
                                                 :cljfx.paint/proportional]))))
(s/def :cljfx.background-fill/fill :cljfx/paint)
(s/def :cljfx.corner-radii/radius number?)
(s/def :cljfx.corner-radii/top-left number?)
(s/def :cljfx.corner-radii/top-right number?)
(s/def :cljfx.corner-radii/bottom-right number?)
(s/def :cljfx.corner-radii/bottom-left number?)
(s/def :cljfx.corner-radii/as-percent boolean?)
(s/def :cljfx/corner-radii
  (s/or :instance (instance-of CornerRadii)
        :empty #{:empty}
        :number number?
        :map (s/keys :req-un [(or :cljfx.corner-radii/radius
                                  (and :cljfx.corner-radii/top-left
                                       :cljfx.corner-radii/top-right
                                       :cljfx.corner-radii/bottom-right
                                       :cljfx.corner-radii/bottom-left))]
                     :opt-un [:cljfx.corner-radii/as-percent])))
(s/def :cljfx.background-fill/radii :cljfx/corner-radii)
(s/def :cljfx.insets/top number?)
(s/def :cljfx.insets/right number?)
(s/def :cljfx.insets/bottom number?)
(s/def :cljfx.insets/left number?)
(s/def :cljfx/insets
  (s/or :instance (instance-of Insets)
        :empty #{:empty}
        :number number?
        :map (s/keys :opt-un [:cljfx.insets/top
                              :cljfx.insets/right
                              :cljfx.insets/bottom
                              :cljfx.insets/left])))
(s/def :cljfx.background-fill/insets :cljfx/insets)
(s/def :cljfx/background-fill
  (s/or :instance (instance-of BackgroundFill)
        :map (s/keys :opt-un [:cljfx.background-fill/fill
                              :cljfx.background-fill/radii
                              :cljfx.background-fill/insets])))
(s/def :cljfx.background/fills (s/coll-of :cljfx/background-fill))
(s/def :cljfx/background
  (s/or :instance (instance-of Background)
        :map (s/keys :opt-un [:cljfx.background/fills
                              :cljfx.background/images])))
(defmethod keyword-prop->spec-form :background [_] :cljfx/background)
(s/def :cljfx/cursor
  (s/or :instance (instance-of Cursor)
        :keyword keyword?
        :string string?))
(defmethod keyword-prop->spec-form :cursor [_] :cljfx/cursor)
(s/def :cljfx/style
  (s/or :string string?
        :map map?))
(defmethod keyword-prop->spec-form :style [_] :cljfx/style)
(defmethod keyword-prop->spec-form :pseudo-classes [_] `set?)
(s/def :cljfx.point-3d/x number?)
(s/def :cljfx.point-3d/y number?)
(s/def :cljfx.point-3d/z number?)
(s/def :cljfx/point-3d
  (s/or :instance (instance-of Point3D)
        :map (s/keys :req-un [:cljfx.point-3d/x
                              :cljfx.point-3d/y
                              :cljfx.point-3d/z])
        :const #{:zero :x-axis :y-axis :z-axis}))
(defmethod keyword-prop->spec-form :point-3d [_] :cljfx/point-3d)
(defmethod keyword-prop->spec-form :insets [_] :cljfx/insets)
(s/def :cljfx/style-class
  (s/or :string string?
        :strings (s/coll-of string?)))
(defmethod keyword-prop->spec-form :style-class [_] :cljfx/style-class)
(s/def :cljfx.border.stroke/stroke :cljfx/paint)
(defenumspec :cljfx.border.stroke.style/type javafx.scene.shape.StrokeType)
(defenumspec :cljfx.border.stroke.style/line-join javafx.scene.shape.StrokeLineJoin)
(defenumspec :cljfx.border.stroke.style/line-cap javafx.scene.shape.StrokeLineCap)
(s/def :cljfx.border.stroke.style/miter-limit number?)
(s/def :cljfx.border.stroke.style/dash-offset number?)
(s/def :cljfx.border.stroke.style/dash-array (s/coll-of number?))
(s/def :cljfx.border.stroke/style
  (s/or :instance (instance-of BorderStrokeStyle)
        :const #{:dashed :dotted :none :solid}
        :map (s/keys :opt-un [:cljfx.border.stroke.style/type
                              :cljfx.border.stroke.style/line-join
                              :cljfx.border.stroke.style/line-cap
                              :cljfx.border.stroke.style/miter-limit
                              :cljfx.border.stroke.style/dash-offset
                              :cljfx.border.stroke.style/dash-array])))
(s/def :cljfx.border.stroke/radii :cljfx/corner-radii)
(s/def :cljfx.border-widths/top number?)
(s/def :cljfx.border-widths/right number?)
(s/def :cljfx.border-widths/bottom number?)
(s/def :cljfx.border-widths/left number?)
(s/def :cljfx.border-widths/as-percentage boolean?)
(s/def :cljfx.border-widths/top-as-percentage boolean?)
(s/def :cljfx.border-widths/right-as-percentage boolean?)
(s/def :cljfx.border-widths/bottom-as-percentage boolean?)
(s/def :cljfx.border-widths/left-as-percentage boolean?)
(s/def :cljfx/border-widths
  (s/or :instance (instance-of BorderWidths)
        :const #{:auto :default :empty :full}
        :number number?
        :map (s/keys :req-un [:cljfx.border-widths/top
                              :cljfx.border-widths/right
                              :cljfx.border-widths/bottom
                              :cljfx.border-widths/left]
                     :opt-un [:cljfx.border-widths/as-percentage
                              :cljfx.border-widths/top-as-percentage
                              :cljfx.border-widths/right-as-percentage
                              :cljfx.border-widths/bottom-as-percentage
                              :cljfx.border-widths/left-as-percentage])))
(s/def :cljfx.border.stroke/insets :cljfx/insets)
(s/def :cljfx.border.stroke/widths :cljfx/border-widths)
(s/def :cljfx.border/strokes
  (s/coll-of (s/or :instance (instance-of BorderStroke)
                   :map (s/keys :opt-un [:cljfx.border.stroke/stroke
                                         :cljfx.border.stroke/style
                                         :cljfx.border.stroke/radii
                                         :cljfx.border.stroke/widths
                                         :cljfx.border.stroke/insets]))))
(s/def :cljfx.border.image/image :cljfx/image)
(s/def :cljfx.border.image/widths :cljfx/border-widths)
(s/def :cljfx.border.image/insets :cljfx/insets)
(s/def :cljfx.border.image/slices :cljfx/border-widths)
(s/def :cljfx.border.image/filled boolean?)
(defenumspec :cljfx/border-repeat javafx.scene.layout.BorderRepeat)
(s/def :cljfx.border.image/repeat-x :cljfx/border-repeat)
(s/def :cljfx.border.image/repeat-y :cljfx/border-repeat)
(s/def :cljfx.border/images
  (s/coll-of (s/or :instance (instance-of BorderImage)
                   :map (s/keys :req-un [:cljfx.border.image/image]
                                :opt-un [:cljfx.border.image/widths
                                         :cljfx.border.image/insets
                                         :cljfx.border.image/slices
                                         :cljfx.border.image/filled
                                         :cljfx.border.image/repeat-x
                                         :cljfx.border.image/repeat-y]))))
(s/def :cljfx/border
  (s/or :instance (instance-of Border)
        :const #{:empty}
        :map (s/keys :opt-un [:cljfx.border/strokes
                              :cljfx.border/images])))
(defmethod keyword-prop->spec-form :border [_] :cljfx/border)

(defn- remove-keys [ks]
  (fn [m]
    (apply dissoc m ks)))

(s/def :fx/key any?)

(defn- keys-satisfy [m]
  (fn [x]
    (and (map? x)
         (every? (fn [[k spec]]
                   (let [v (get x k ::not-found)]
                     (or (identical? ::not-found v)
                         (s/valid? spec v))))
                 m))))
(defmethod keyword-prop->spec-form :add-props [{:keys [props to]}]
  `(s/and
     (keys-satisfy ~(into {} (map (juxt key (comp prop->spec-form val))) props))
     (s/conformer (remove-keys ~(vec (keys props))))
     ~(prop->spec-form to)))

(defmethod keyword-prop->spec-form :any [_] `(s/spec any?))

(register-props! :node
  '{:view-order {:type :number}
    :accessible-help {:type :string}
    :accessible-role-description {:type :string}
    :accessible-role {:type :enum :of javafx.scene.AccessibleRole}
    :blend-mode {:type :enum :of javafx.scene.effect.BlendMode}
    :cache-hint {:type :enum :of javafx.scene.CacheHint}
    :cache {:type :boolean}
    :clip {:type :desc :of javafx.scene.Node}
    :cursor {:type :cursor}
    :depth-test {:type :enum :of javafx.scene.DepthTest}
    :disable {:type :boolean}
    :effect {:type :desc :of javafx.scene.effect.Effect}
    :event-dispatcher {:type javafx.event.EventDispatcher}
    :event-filter {:type :event-handler :of javafx.event.EventHandler}
    :event-handler {:type :event-handler :of javafx.event.EventHandler}
    :focus-traversable {:type :boolean}
    :id {:type :string}
    :input-method-requests {:type javafx.scene.input.InputMethodRequests}
    :layout-x {:type :number}
    :layout-y {:type :number}
    :managed {:type :boolean}
    :mouse-transparent {:type :boolean}
    :node-orientation {:type :enum :of javafx.geometry.NodeOrientation}
    :on-context-menu-requested {:type :event-handler :of javafx.event.EventHandler}
    :on-drag-detected {:type :event-handler :of javafx.event.EventHandler}
    :on-drag-done {:type :event-handler :of javafx.event.EventHandler}
    :on-drag-dropped {:type :event-handler :of javafx.event.EventHandler}
    :on-drag-entered {:type :event-handler :of javafx.event.EventHandler}
    :on-drag-exited {:type :event-handler :of javafx.event.EventHandler}
    :on-drag-over {:type :event-handler :of javafx.event.EventHandler}
    :on-focused-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-input-method-text-changed {:type :event-handler :of javafx.event.EventHandler}
    :on-key-pressed {:type :event-handler :of javafx.event.EventHandler}
    :on-key-released {:type :event-handler :of javafx.event.EventHandler}
    :on-key-typed {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-clicked {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-drag-entered {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-drag-exited {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-drag-over {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-drag-released {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-dragged {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-entered {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-exited {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-moved {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-pressed {:type :event-handler :of javafx.event.EventHandler}
    :on-mouse-released {:type :event-handler :of javafx.event.EventHandler}
    :on-rotate {:type :event-handler :of javafx.event.EventHandler}
    :on-rotation-finished {:type :event-handler :of javafx.event.EventHandler}
    :on-rotation-started {:type :event-handler :of javafx.event.EventHandler}
    :on-scroll {:type :event-handler :of javafx.event.EventHandler}
    :on-scroll-finished {:type :event-handler :of javafx.event.EventHandler}
    :on-scroll-started {:type :event-handler :of javafx.event.EventHandler}
    :on-swipe-down {:type :event-handler :of javafx.event.EventHandler}
    :on-swipe-left {:type :event-handler :of javafx.event.EventHandler}
    :on-swipe-right {:type :event-handler :of javafx.event.EventHandler}
    :on-swipe-up {:type :event-handler :of javafx.event.EventHandler}
    :on-touch-moved {:type :event-handler :of javafx.event.EventHandler}
    :on-touch-pressed {:type :event-handler :of javafx.event.EventHandler}
    :on-touch-released {:type :event-handler :of javafx.event.EventHandler}
    :on-touch-stationary {:type :event-handler :of javafx.event.EventHandler}
    :on-zoom {:type :event-handler :of javafx.event.EventHandler}
    :on-zoom-finished {:type :event-handler :of javafx.event.EventHandler}
    :on-zoom-started {:type :event-handler :of javafx.event.EventHandler}
    :opacity {:type :number}
    :pick-on-bounds {:type :boolean}
    :pseudo-classes {:type :pseudo-classes}
    :rotate {:type :number}
    :rotation-axis {:type :point-3d}
    :scale-x {:type :number}
    :scale-y {:type :number}
    :scale-z {:type :number}
    :style {:type :style}
    :style-class {:type :style-class}
    :transforms {:type :coll
                 :item {:type :add-props
                        :props {:fx/key {:type :any}}
                        :to {:type :desc :of javafx.scene.transform.Transform}}}
    :translate-x {:type :number}
    :translate-y {:type :number}
    :translate-z {:type :number}
    :visible {:type :boolean}})

(register-props! :parent :node
  '{:stylesheets {:type :coll :item {:type :string}}})

(register-props! :region :parent
  '{:background {:type :background}
    :border {:type :border}
    :cache-shape {:type :boolean}
    :center-shape {:type :boolean}
    :max-height {:type :pref-or-computed-size-double}
    :max-width {:type :pref-or-computed-size-double}
    :min-height {:type :pref-or-computed-size-double}
    :min-width {:type :pref-or-computed-size-double}
    :opaque-insets {:type :insets}
    :padding {:type :insets}
    :pref-height {:type :computed-size-double}
    :pref-width {:type :computed-size-double}
    :scale-shape {:type :boolean}
    :shape {:type :desc :of javafx.scene.shape.Shape}
    :snap-to-pixel {:type :boolean}})

(register-composite! :region
  :parent :parent
  :props '{:background {:type :background}
           :border {:type :border}
           :cache-shape {:type :boolean}
           :center-shape {:type :boolean}
           :max-height {:type :pref-or-computed-size-double}
           :max-width {:type :pref-or-computed-size-double}
           :min-height {:type :pref-or-computed-size-double}
           :min-width {:type :pref-or-computed-size-double}
           :opaque-insets {:type :insets}
           :padding {:type :insets}
           :pref-height {:type :computed-size-double}
           :pref-width {:type :computed-size-double}
           :scale-shape {:type :boolean}
           :shape {:type :desc :of javafx.scene.shape.Shape}
           :snap-to-pixel {:type :boolean}}
  :of 'javafx.scene.layout.Region)

(register-props! :window
  '{:force-integer-render-scale {:type :boolean}
    :render-scale-x {:type :number}
    :render-scale-y {:type :number}
    :event-dispatcher {:type javafx.event.EventDispatcher}
    :event-filter {:type :event-handler :of javafx.event.EventHandler}
    :event-handler {:type :event-handler :of javafx.event.EventHandler}
    :height {:type :number}
    :on-close-request {:type :event-handler :of javafx.event.EventHandler}
    :on-focused-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-height-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-hidden {:type :event-handler :of javafx.event.EventHandler}
    :on-hiding {:type :event-handler :of javafx.event.EventHandler}
    :on-showing {:type :event-handler :of javafx.event.EventHandler}
    :on-shown {:type :event-handler :of javafx.event.EventHandler}
    :on-width-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-x-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-y-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :opacity {:type :number}
    :user-data {:type :any}
    :width {:type :number}
    :x {:type :number}
    :y {:type :number}})

(defenumspec :cljfx/key-code javafx.scene.input.KeyCode)
(s/def :cljfx/key-combination
  (s/or
    :vector (s/and
              (s/nonconforming
                (s/cat :modifiers (s/* #{:ctrl :shift :alt :meta :shortcut})
                       :end (s/or :enum :cljfx/key-code
                                  :string string?)))
              vector?)
    :string string?
    :instance (instance-of KeyCombination)))
(defmethod keyword-prop->spec-form :key-combination [_] :cljfx/key-combination)
(defmethod keyword-prop->spec-form :image [_] :cljfx/image)

(register-composite! :stage
  :parent :window
  :props '{:always-on-top {:type :boolean}
           :full-screen {:type :boolean}
           :full-screen-exit-hint {:type java.lang.String}
           :full-screen-exit-key-combination {:type :key-combination}
           :iconified {:type :boolean}
           :icons {:type :coll :item {:type :image}}
           :max-height {:type :number}
           :max-width {:type :number}
           :maximized {:type :boolean}
           :min-height {:type :number}
           :min-width {:type :number}
           :modality {:type :enum :of javafx.stage.Modality}
           :on-iconified-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-maximized-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :owner {:type :desc :of javafx.stage.Window}
           :resizable {:type :boolean}
           :scene {:type :desc :of javafx.scene.Scene}
           :showing {:type :boolean}
           :style {:type :enum :of javafx.stage.StageStyle}
           :title {:type java.lang.String}}
  :of 'javafx.stage.Stage)

(register-props! :control :region
  '{:context-menu {:type :desc :of javafx.scene.control.ContextMenu}
    :tooltip {:type :desc :of javafx.scene.control.Tooltip}})

(register-composite! :accordion
  :parent :control
  :props '{:panes {:type :coll
                   :item {:type :add-props
                          :props {:fx/key {:type :any}}
                          :to {:type :desc :of javafx.scene.control.TitledPane}}}}
  :of 'javafx.scene.control.Accordion)

(register-props! :transform
  '{:on-transform-changed {:type :event-handler :of javafx.event.EventHandler}})

(register-composite! :affine
  :parent :transform
  :props '{:mxx {:type :number}
           :mxy {:type :number}
           :mxz {:type :number}
           :myx {:type :number}
           :myy {:type :number}
           :myz {:type :number}
           :mzx {:type :number}
           :mzy {:type :number}
           :mzz {:type :number}
           :tx {:type :number}
           :ty {:type :number}
           :tz {:type :number}}
  :of 'javafx.scene.transform.Affine)

(defmethod keyword-prop->spec-form :result-converter [_]
  `(s/or :ifn ifn?
         :instance (instance-of Callback)))
(register-composite! :dialog
  :props '{:content-text {:type java.lang.String}
           :dialog-pane {:type :desc :of javafx.scene.control.DialogPane}
           :graphic {:type :desc :of javafx.scene.Node}
           :header-text {:type java.lang.String}
           :height {:type :number}
           :modality {:type :enum :of javafx.stage.Modality}
           :on-close-request {:type :event-handler :of javafx.event.EventHandler}
           :on-hidden {:type :event-handler :of javafx.event.EventHandler}
           :on-hiding {:type :event-handler :of javafx.event.EventHandler}
           :on-showing {:type :event-handler :of javafx.event.EventHandler}
           :on-shown {:type :event-handler :of javafx.event.EventHandler}
           :owner {:type :desc :of javafx.stage.Window}
           :resizable {:type :boolean}
           :result {:type :any}
           :result-converter {:type :result-converter}
           :showing {:type :boolean}
           :style {:type :enum :of javafx.stage.StageStyle}
           :title {:type java.lang.String}
           :width {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.control.Dialog)

(s/def :cljfx.button-type/text string?)
(defenumspec :cljfx.button-type/button-data javafx.scene.control.ButtonBar$ButtonData)
(defmethod keyword-prop->spec-form :button-type [_]
  `(s/or :enum #{:apply :cancel :close :finish :next :no :ok :previous :yes}
         :map (s/keys :opt-un [:cljfx.button-type/text
                               :cljfx.button-type/button-data])
         :string string?
         :instance (instance-of ButtonType)))
(register-composite! :alert
  :parent :dialog
  :req [:alert-type]
  :props '{:alert-type {:type :enum :of javafx.scene.control.Alert$AlertType}
           :button-types {:type :coll :item {:type :button-type}}}
  :of 'javafx.scene.control.Alert)

(defmethod keyword-prop->spec-form :color [_] :cljfx/color)

(register-props! :light-base :node
  '{:color {:type :color}
    :light-on {:type :boolean}})

(register-composite! :ambient-light
  :parent :light-base
  :of 'javafx.scene.AmbientLight)

(register-composite! :pane
  :parent :region
  :props '{:children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}}
                             :to {:type :desc :of javafx.scene.Node}}}}
  :of 'javafx.scene.layout.Pane)

(s/def :anchor-pane/top number?)
(s/def :anchor-pane/left number?)
(s/def :anchor-pane/bottom number?)
(s/def :anchor-pane/right number?)
(register-composite! :anchor-pane
  :parent :pane
  :props '{:children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}
                                     :anchor-pane/top {:type :number}
                                     :anchor-pane/left {:type :number}
                                     :anchor-pane/bottom {:type :number}
                                     :anchor-pane/right {:type :number}}
                             :to {:type :desc :of javafx.scene.Node}}}}
  :of 'javafx.scene.layout.AnchorPane)

(defmethod keyword-prop->spec-form :animation [_]
  `(s/or :enum #{:indefinite}
         :number number?))
(s/def :cljfx/duration
  (s/or :enum #{:zero :one :indefinite :unknown}
        :number number?
        :string string?
        :tuple (s/tuple number? #{:ms :s :m :h})
        :instance (instance-of Duration)))
(defmethod keyword-prop->spec-form :duration [_] :cljfx/duration)
(defmethod keyword-prop->spec-form :animation-status [_] `(s/spec #{:running :paused :stopped}))
(register-props! :animation
  '{:auto-reverse {:type :boolean}
    :cycle-count {:type :animation}
    :delay {:type :duration}
    :jump-to {:type :duration}
    :on-auto-reverse-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-current-time-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-cycle-count-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-cycle-duration-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-delay-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-finished {:type :event-handler :of javafx.event.EventHandler}
    :on-on-finished-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-rate-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :on-status-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :rate {:type :number}
    :status {:type :animation-status}})
(defmethod keyword-prop->spec-form :paint [_] :cljfx/paint)
(register-props! :shape :node
  '{:fill {:type :paint}
    :smooth {:type :boolean}
    :stroke {:type :paint}
    :stroke-dash-array {:type :coll :item {:type :number}}
    :stroke-dash-offset {:type :number}
    :stroke-line-cap {:type :enum :of javafx.scene.shape.StrokeLineCap}
    :stroke-line-join {:type :enum :of javafx.scene.shape.StrokeLineJoin}
    :stroke-miter-limit {:type :number}
    :stroke-type {:type :enum :of javafx.scene.shape.StrokeType}
    :stroke-width {:type :number}})
(register-composite! :arc
  :parent :shape
  :props '{:center-x {:type :number}
           :center-y {:type :number}
           :length {:type :number}
           :radius-x {:type :number}
           :radius-y {:type :number}
           :start-angle {:type :number}
           :type {:type :enum :of javafx.scene.shape.ArcType}}
  :of 'javafx.scene.shape.Arc)
(register-props! :path-element
  '{:absolute {:type :boolean}})
(register-composite! :arc-to
  :parent :path-element
  :props '{:large-arc-flag {:type :boolean}
           :radius-x {:type :number}
           :radius-y {:type :number}
           :sweep-flag {:type :boolean}
           :x {:type :number}
           :x-axis-rotation {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.shape.ArcTo)
(register-props! :chart :region
  '{:animated {:type :boolean}
    :legend-side {:type :enum :of javafx.geometry.Side}
    :legend-visible {:type :boolean}
    :title {:type :string}
    :title-side {:type :enum :of javafx.geometry.Side}})
(register-props! :xy-chart :chart
  '{:alternative-column-fill-visible {:type :boolean}
    :alternative-row-fill-visible {:type :boolean}
    :data {:type :coll
           :item {:type :add-props
                  :props {:fx/key {:type :any}}
                  :to {:type :desc :of javafx.scene.chart.XYChart$Series}}}
    :horizontal-grid-lines-visible {:type :boolean}
    :horizontal-zero-line-visible {:type :boolean}
    :vertical-grid-lines-visible {:type :boolean}
    :vertical-zero-line-visible {:type :boolean}
    :x-axis {:type :desc :of javafx.scene.chart.Axis}
    :y-axis {:type :desc :of javafx.scene.chart.Axis}})
(register-composite! :area-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :props '{:create-symbols {:type :boolean}}
  :of 'javafx.scene.chart.AreaChart)
(s/def :cljfx.font/family string?)
(defenumspec :cljfx.font/weight javafx.scene.text.FontWeight)
(defenumspec :cljfx.font/posture javafx.scene.text.FontPosture)
(s/def :cljfx.font/size number?)
(defmethod keyword-prop->spec-form :font [_]
  `(s/or :enum #{:default}
         :number number?
         :string string?
         :map (s/keys :req-un [:cljfx.font/family]
                      :opt-un [:cljfx.font/weight
                               :cljfx.font/posture
                               :cljfx.font/size])
         :instance (instance-of Font)))
(register-props! :axis :region
  '{:animated {:type :boolean}
    :auto-ranging {:type :boolean}
    :label {:type :string}
    :side {:type :enum :of javafx.geometry.Side}
    :tick-label-fill {:type :paint}
    :tick-label-font {:type :font}
    :tick-label-gap {:type :number}
    :tick-label-rotation {:type :number}
    :tick-labels-visible {:type :boolean}
    :tick-length {:type :number}
    :tick-mark-visible {:type :boolean}})

(register-composite! :bar-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :props '{:bar-gap {:type :number}
           :category-gap {:type :number}}
  :of 'javafx.scene.chart.BarChart)

(register-composite! :blend
  :props '{:bottom-input {:type :desc :of javafx.scene.effect.Effect}
           :mode {:type :enum :of javafx.scene.effect.BlendMode}
           :opacity {:type :number}
           :top-input {:type :desc :of javafx.scene.effect.Effect}}
  :of 'javafx.scene.effect.Blend)

(register-composite! :bloom
  :props '{:input {:type :desc :of javafx.scene.effect.Effect}
           :threshold {:type :number}}
  :of 'javafx.scene.effect.Bloom)

(defenumspec :cljfx/pos javafx.geometry.Pos)
(s/def :border-pane/alignment :cljfx/pos)
(s/def :border-pane/margin :cljfx/insets)
(register-composite! :border-pane
  :parent :pane
  :props '{:bottom {:type :add-props
                    :props {:border-pane/alignment {:type :enum :of javafx.geometry.Pos}
                            :border-pane/margin {:type :insets}}
                    :to {:type :desc :of javafx.scene.Node}}
           :center {:type :add-props
                    :props {:border-pane/alignment {:type :enum :of javafx.geometry.Pos}
                            :border-pane/margin {:type :insets}}
                    :to {:type :desc :of javafx.scene.Node}}
           :left {:type :add-props
                  :props {:border-pane/alignment {:type :enum :of javafx.geometry.Pos}
                          :border-pane/margin {:type :insets}}
                  :to {:type :desc :of javafx.scene.Node}}
           :right {:type :add-props
                   :props {:border-pane/alignment {:type :enum :of javafx.geometry.Pos}
                           :border-pane/margin {:type :insets}}
                   :to {:type :desc :of javafx.scene.Node}}
           :top {:type :add-props
                 :props {:border-pane/alignment {:type :enum :of javafx.geometry.Pos}
                         :border-pane/margin {:type :insets}}
                 :to {:type :desc :of javafx.scene.Node}}}
  :of 'javafx.scene.layout.BorderPane)
(register-props! :shape3d :node
  '{:cull-face {:type :enum :of javafx.scene.shape.CullFace}
    :draw-mode {:type :enum :of javafx.scene.shape.DrawMode}
    :material {:type :desc :of javafx.scene.paint.Material}})
(register-composite! :box
  :parent :shape3d
  :props '{:depth {:type :number}
           :height {:type :number}
           :width {:type :number}}
  :of 'javafx.scene.shape.Box)
(register-composite! :box-blur
  :props '{:height {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :iterations {:type :number}
           :width {:type :number}}
  :of 'javafx.scene.effect.BoxBlur)
(register-composite! :bubble-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :of 'javafx.scene.chart.BubbleChart)
(register-props! :labeled :control
  '{:alignment {:type :enum :of javafx.geometry.Pos}
    :content-display {:type :enum :of javafx.scene.control.ContentDisplay}
    :ellipsis-string {:type :string}
    :font {:type :font}
    :graphic {:type :desc :of javafx.scene.Node}
    :graphic-text-gap {:type :number}
    :line-spacing {:type :number}
    :mnemonic-parsing {:type :boolean}
    :text {:type :string}
    :text-alignment {:type :enum :of javafx.scene.text.TextAlignment}
    :text-fill {:type :paint}
    :text-overrun {:type :enum :of javafx.scene.control.OverrunStyle}
    :underline {:type :boolean}
    :wrap-text {:type :boolean}})
(register-props! :button-base :labeled
  '{:on-action {:type :event-handler :of javafx.event.EventHandler}})
(register-composite! :button
  :parent :button-base
  :props '{:cancel-button {:type :boolean}
           :default-button {:type :boolean}}
  :of 'javafx.scene.control.Button)
(defenumspec :button-bar/button-data javafx.scene.control.ButtonBar$ButtonData)
(register-composite! :button-bar
  :parent :control
  :props '{:button-min-width {:type :number}
           :button-order {:type :string}
           :buttons {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}
                                    :button-bar/button-data {:type :enum :of javafx.scene.control.ButtonBar$ButtonData}}
                            :to {:type :desc :of javafx.scene.Node}}}}
  :of 'javafx.scene.control.ButtonBar)
(register-props! :camera
  '{:far-clip {:type :number}
    :near-clip {:type :number}})
(defmethod keyword-prop->spec-form :ifn [_] `(s/spec ifn?))
(register-composite! :canvas
  :parent :node
  :props '{:draw {:type :ifn}
           :height {:type :number}
           :width {:type :number}}
  :of 'javafx.scene.canvas.Canvas)
(register-composite! :category-axis
  :parent :axis
  :props '{:categories {:type :coll :item {:type :string}}
           :end-margin {:type :number}
           :gap-start-and-end {:type :boolean}
           :start-margin {:type :number}}
  :of 'javafx.scene.chart.CategoryAxis)
(register-composite! :cell
  :parent :labeled
  :props '{:editable {:type :boolean}}
  :of 'javafx.scene.control.Cell)
(register-composite! :check-box
  :parent :button-base
  :props '{:allow-indeterminate {:type :boolean}
           :indeterminate {:type :boolean}
           :on-selected-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :selected {:type :boolean}}
  :of 'javafx.scene.control.CheckBox)
(register-composite! :menu-item
  :props '{:accelerator {:type :key-combination}
           :disable {:type :boolean}
           :graphic {:type :desc :of javafx.scene.Node}
           :id {:type :string}
           :mnemonic-parsing {:type :boolean}
           :on-action {:type :event-handler :of javafx.event.EventHandler}
           :on-menu-validation {:type :event-handler :of javafx.event.EventHandler}
           :style {:type :string}
           :style-class {:type :string}
           :text {:type :string}
           :user-data {:type :any}
           :visible {:type :boolean}}
  :of 'javafx.scene.control.MenuItem)
(register-composite! :check-menu-item
  :parent :menu-item
  :props '{:selected {:type :boolean}}
  :of 'javafx.scene.control.CheckMenuItem)
(defmethod keyword-prop->spec-form :string-converter [_]
  `(s/or :enum #{:big-decimal
                 :big-integer
                 :boolean
                 :byte
                 :character
                 :date-time
                 :default
                 :double
                 :float
                 :integer
                 :local-date
                 :local-date-time
                 :local-time
                 :long
                 :number
                 :short}
         :instance (instance-of StringConverter)))
(register-composite! :choice-box
  :parent :control
  :props '{:converter {:type :string-converter}
           :items {:type :coll :item {:type :any}}
           :on-action {:type :event-handler :of javafx.event.EventHandler}
           :on-hidden {:type :event-handler :of javafx.event.EventHandler}
           :on-hiding {:type :event-handler :of javafx.event.EventHandler}
           :on-showing {:type :event-handler :of javafx.event.EventHandler}
           :on-shown {:type :event-handler :of javafx.event.EventHandler}
           :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :value {:type :any}}
  :of 'javafx.scene.control.ChoiceBox)

(register-composite!
  :choice-dialog
  :parent :dialog
  :props '{:items {:type :coll :item {:type :any}}
           :selected-item {:type :any}}
  :of 'javafx.scene.control.ChoiceDialog)

(register-composite! :circle
  :parent :shape
  :props '{:center-x {:type :number}
           :center-y {:type :number}
           :radius {:type :number}}
  :of 'javafx.scene.shape.Circle)

(register-composite! :close-path
  :parent :path-element
  :props '{}
  :of 'javafx.scene.shape.ClosePath)

(register-composite! :color-adjust
  :props '{:brightness {:type :number}
           :contrast {:type :number}
           :hue {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :saturation {:type :number}}
  :of 'javafx.scene.effect.ColorAdjust)

(register-composite! :color-input
  :props '{:height {:type :number}
           :paint {:type :paint}
           :width {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.effect.ColorInput)

(register-props! :combo-box-base :control
  '{:editable {:type :boolean}
    :on-action {:type :event-handler :of javafx.event.EventHandler}
    :on-hidden {:type :event-handler :of javafx.event.EventHandler}
    :on-hiding {:type :event-handler :of javafx.event.EventHandler}
    :on-showing {:type :event-handler :of javafx.event.EventHandler}
    :on-shown {:type :event-handler :of javafx.event.EventHandler}
    :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :prompt-text {:type :string}
    :value {:type :any}})

(register-composite! :color-picker
  :parent :combo-box-base
  :props '{:custom-colors {:type :coll :item {:type :color}}
           :value {:type :color}}
  :of 'javafx.scene.control.ColorPicker)

(register-composite! :column-constraints
  :props '{:fill-width {:type :boolean}
           :halignment {:type :enum :of javafx.geometry.HPos}
           :hgrow {:type :enum :of javafx.scene.layout.Priority}
           :max-width {:type :pref-or-computed-size-double}
           :min-width {:type :pref-or-computed-size-double}
           :percent-width {:type :pref-or-computed-size-double}
           :pref-width {:type :pref-or-computed-size-double}}
  :of 'javafx.scene.layout.ColumnConstraints)

(s/def :cljfx.cell-type/props map?)
(s/def :cljfx.cell-type/ctor ifn?)
(s/def :fx/cell-type (s/or :kw keyword?
                           :map (s/keys :req-un [:cljfx.cell-type/props
                                                 :cljfx.cell-type/ctor])))

(defmethod keyword-prop->spec-form :cell-factory [_]
  `(s/or :fn ifn?
         :map (s/keys :req [:fx/cell-type]
                      :req-un [:cljfx.cell-factory/describe])))

(register-composite! :combo-box
  :parent :combo-box-base
  :props '{:button-cell {:type :ifn}
           :cell-factory {:type :cell-factory}
           :converter {:type :string-converter}
           :items {:type :coll :item {:type :any}}
           :placeholder {:type :desc :of javafx.scene.Node}
           :visible-row-count {:type :number}}
  :of 'javafx.scene.control.ComboBox)

(register-composite! :indexed-cell
  :parent :cell
  :props '{}
  :of 'javafx.scene.control.IndexedCell)

(register-composite! :list-cell
  :parent :indexed-cell
  :props '{}
  :of 'javafx.scene.control.ListCell)

(register-composite! :combo-box-list-cell
  :parent :list-cell
  :props '{:combo-box-editable {:type :boolean}
           :converter {:type :string-converter}}
  :of 'javafx.scene.control.cell.ComboBoxListCell)

(register-props! :popup-window :window
  '{:anchor-location {:type :enum :of javafx.stage.PopupWindow$AnchorLocation}
    :anchor-x {:type :number}
    :anchor-y {:type :number}
    :auto-fix {:type :boolean}
    :auto-hide {:type :boolean}
    :consume-auto-hiding-events {:type :boolean}
    :hide-on-escape {:type :boolean}
    :on-auto-hide {:type :event-handler :of javafx.event.EventHandler}})

(register-composite! :popup-control
  :parent :popup-window
  :props '{:id {:type :string}
           :max-height {:type :pref-or-computed-size-double}
           :max-width {:type :pref-or-computed-size-double}
           :min-height {:type :pref-or-computed-size-double}
           :min-width {:type :pref-or-computed-size-double}
           :pref-height {:type :computed-size-double}
           :pref-width {:type :computed-size-double}
           :style {:type :string}
           :style-class {:type :style-class}}
  :of 'javafx.scene.control.PopupControl)

(register-composite! :context-menu
  :parent :popup-control
  :props '{:items {:type :coll
                   :item {:type :add-props
                          :props {:fx/key {:type :any}}
                          :to {:type :desc :of javafx.scene.control.MenuItem}}}
           :on-action {:type :event-handler :of javafx.event.EventHandler}}
  :of 'javafx.scene.control.ContextMenu)

(register-composite! :cubic-curve
  :parent :shape
  :props '{:control-x1 {:type :number}
           :control-x2 {:type :number}
           :control-y1 {:type :number}
           :control-y2 {:type :number}
           :end-x {:type :number}
           :end-y {:type :number}
           :start-x {:type :number}
           :start-y {:type :number}}
  :of 'javafx.scene.shape.CubicCurve)

(register-composite! :cubic-curve-to
  :parent :path-element
  :props '{:control-x1 {:type :number}
           :control-x2 {:type :number}
           :control-y1 {:type :number}
           :control-y2 {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.shape.CubicCurveTo)

(register-composite! :custom-menu-item
  :parent :menu-item
  :props '{:content {:type :desc :of javafx.scene.Node}
           :hide-on-click {:type :boolean}}
  :of 'javafx.scene.control.CustomMenuItem)

(register-composite! :cylinder
  :parent :shape3d
  :props '{:height {:type :number}
           :radius {:type :number}}
  :of 'javafx.scene.shape.Cylinder)

(register-composite! :date-cell
  :parent :cell
  :props '{}
  :of 'javafx.scene.control.DateCell)

(defmethod keyword-prop->spec-form :chronology [_]
  `(s/or :enum #{:iso :hijrah :japanese :minguo :thai-buddhist}
         :instance (instance-of Chronology)))

(register-composite! :date-picker
  :parent :combo-box-base
  :props '{:chronology {:type :chronology}
           :converter {:type :string-converter}
           :day-cell-factory {:type :cell-factory}
           :show-week-numbers {:type :boolean}}
  :of 'javafx.scene.control.DatePicker)

(register-composite! :dialog-pane
  :parent :pane
  :props '{:button-types {:type :coll :item {:type :button-type}}
           :content {:type :desc :of javafx.scene.Node}
           :content-text {:type :string}
           :expandable-content {:type :desc :of javafx.scene.Node}
           :expanded {:type :boolean}
           :graphic {:type :desc :of javafx.scene.Node}
           :header {:type :desc :of javafx.scene.Node}
           :header-text {:type :string}}
  :of 'javafx.scene.control.DialogPane)

(s/def :cljfx.float-map/width int?)
(s/def :cljfx.float-map/height int?)
(s/def :cljfx.float-map.sample/x int?)
(s/def :cljfx.float-map.sample/y int?)
(s/def :cljfx.float-map.sample/s (s/tuple number? number?))
(s/def :cljfx.float-map/samples
  (s/coll-of (s/keys :opt-un [:cljfx.float-map.sample/x
                              :cljfx.float-map.sample/y
                              :cljfx.float-map.sample/s])))

(defmethod keyword-prop->spec-form :float-map [_]
  `(s/or :instance (instance-of FloatMap)
         :map (s/keys :opt-un [:cljfx.float-map/width
                               :cljfx.float-map/height
                               :cljfx.float-map/samples])))

(register-composite! :displacement-map
  :props '{:input {:type :desc :of javafx.scene.effect.Effect}
           :map-data {:type :float-map}
           :offset-x {:type :number}
           :offset-y {:type :number}
           :scale-x {:type :number}
           :scale-y {:type :number}
           :wrap {:type :boolean}}
  :of 'javafx.scene.effect.DisplacementMap)

(register-props! :spinner-value-factory
  '{:converter {:type :string-converter}
    :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :value {:type :any}
    :wrap-around {:type :boolean}})

(register-composite! :double-spinner-value-factory
  :parent :spinner-value-factory
  :req [:min :max]
  :props '{:amount-to-step-by {:type :number}
           :max {:type :number}
           :min {:type :number}
           :value {:type :number}}
  :of 'javafx.scene.control.SpinnerValueFactory$DoubleSpinnerValueFactory)

(register-composite! :drop-shadow
  :props '{:blur-type {:type :enum :of javafx.scene.effect.BlurType}
           :color {:type :color}
           :height {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :offset-x {:type :number}
           :offset-y {:type :number}
           :radius {:type :number}
           :spread {:type :number}
           :width {:type :number}}
  :of 'javafx.scene.effect.DropShadow)

(register-composite! :ellipse
  :parent :shape
  :props '{:center-x {:type :number}
           :center-y {:type :number}
           :radius-x {:type :number}
           :radius-y {:type :number}}
  :of 'javafx.scene.shape.Ellipse)

(defmethod keyword-prop->spec-form :interpolator [_]
  `(s/or :enum #{:discrete :ease-both :ease-in :ease-out :linear}
         :spline (s/tuple #{:spline} number? number? number? number?)
         :tangent (s/or :arity-3 (s/tuple #{:tangent} :cljfx/duration number?)
                        :arity-5 (s/tuple #{:tangent} :cljfx/duration number? :cljfx/duration number?))
         :instance (instance-of Interpolator)))

(register-props! :transition :animation
  '{:interpolator {:type :interpolator}})

(register-composite! :fade-transition
  :parent :transition
  :props '{:by-value {:type :number}
           :duration {:type :duration}
           :from-value {:type :number}
           :node {:type :desc :of javafx.scene.Node}
           :to-value {:type :number}}
  :of 'javafx.animation.FadeTransition)

(register-composite! :fill-transition
  :parent :transition
  :props '{:duration {:type :duration}
           :from-value {:type :color}
           :shape {:type :desc :of javafx.scene.shape.Shape}
           :to-value {:type :color}}
  :of 'javafx.animation.FillTransition)

(s/def :flow-pane/margin :cljfx/insets)

(register-composite! :flow-pane
  :parent :pane
  :props '{:alignment {:type :enum :of javafx.geometry.Pos}
           :children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}
                                     :flow-pane/margin {:type :insets}}
                             :to {:type :desc :of javafx.scene.Node}}}
           :column-halignment {:type :enum :of javafx.geometry.HPos}
           :hgap {:type :number}
           :orientation {:type :enum :of javafx.geometry.Orientation}
           :pref-wrap-length {:type :number}
           :row-valignment {:type :enum :of javafx.geometry.VPos}
           :vgap {:type :number}}
  :of 'javafx.scene.layout.FlowPane)

(register-composite! :gaussian-blur
  :props '{:input {:type :desc :of javafx.scene.effect.Effect}
           :radius {:type :number}}
  :of 'javafx.scene.effect.GaussianBlur)

(register-composite! :glow
  :props '{:input {:type :desc :of javafx.scene.effect.Effect}
           :level {:type :number}}
  :of 'javafx.scene.effect.Glow)

(s/def :grid-pane/column int?)
(s/def :grid-pane/column-span int?)
(s/def :grid-pane/fill-height boolean?)
(s/def :grid-pane/fill-width boolean?)
(defenumspec :grid-pane/halignment javafx.geometry.HPos)
(defenumspec :cljfx/priority javafx.scene.layout.Priority)
(s/def :grid-pane/hgrow :cljfx/priority)
(s/def :grid-pane/margin :cljfx/insets)
(s/def :grid-pane/row int?)
(s/def :grid-pane/row-span int?)
(defenumspec :grid-pane/valignment javafx.geometry.VPos)
(s/def :grid-pane/vgrow :cljfx/priority)
(defmethod keyword-prop->spec-form :int [_] `(s/spec int?))
(register-composite! :grid-pane
  :parent :pane
  :props '{:alignment {:type :enum
                       :of javafx.geometry.Pos}
           :children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}
                                     :grid-pane/column {:type :int}
                                     :grid-pane/column-span {:type :int}
                                     :grid-pane/fill-height {:type :boolean}
                                     :grid-pane/fill-width {:type :boolean}
                                     :grid-pane/halignment {:type :enum :of javafx.geometry.HPos}
                                     :grid-pane/hgrow {:type :enum :of javafx.scene.layout.Priority}
                                     :grid-pane/margin {:type :insets}
                                     :grid-pane/row {:type :int}
                                     :grid-pane/row-span {:type :int}
                                     :grid-pane/valignment {:type :enum :of javafx.geometry.VPos}
                                     :grid-pane/vgrow {:type :enum :of javafx.scene.layout.Priority}}
                             :to {:type :desc :of javafx.scene.Node}}}
           :column-constraints {:type :coll
                                :item {:type :add-props
                                       :props {:fx/key {:type :any}}
                                       :to {:type :desc
                                            :of javafx.scene.layout.ColumnConstraints}}}
           :grid-lines-visible {:type :boolean}
           :hgap {:type :number}
           :row-constraints {:type :coll
                             :item {:type :add-props
                                    :props {:fx/key {:type :any}}
                                    :to {:type :desc
                                         :of javafx.scene.layout.RowConstraints}}}
           :vgap {:type :number}}
  :of 'javafx.scene.layout.GridPane)

(register-composite! :group
  :parent :parent
  :props '{:auto-size-children {:type :boolean}
           :children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}}
                             :to {:type :desc
                                  :of javafx.scene.Node}}}}
  :of 'javafx.scene.Group)

(s/def :h-box/margin :cljfx/insets)
(s/def :h-box/hgrow :cljfx/priority)

(register-composite! :h-box
  :parent :pane
  :props '{:alignment {:type :enum :of javafx.geometry.Pos}
           :children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}
                                     :h-box/margin {:type :insets}
                                     :h-box/hgrow {:type :enum :of javafx.scene.layout.Priority}}
                             :to {:type :desc :of javafx.scene.Node}}}
           :fill-height {:type :boolean}
           :spacing {:type :number}}
  :of 'javafx.scene.layout.HBox)

(register-composite! :h-line-to
  :parent :path-element
  :props '{:x {:type :number}}
  :of 'javafx.scene.shape.HLineTo)

(register-composite! :html-editor
  :parent :control
  :props '{:html-text {:type :string}}
  :of 'javafx.scene.web.HTMLEditor)

(register-composite! :hyperlink
  :parent :button-base
  :props '{:visited {:type :boolean}}
  :of 'javafx.scene.control.Hyperlink)

(register-composite! :image-input
  :props '{:source {:type :image}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.effect.ImageInput)

(s/def :cljfx.rectangle/min-x number?)
(s/def :cljfx.rectangle/min-y number?)
(s/def :cljfx.rectangle/width number?)
(s/def :cljfx.rectangle/height number?)

(defmethod keyword-prop->spec-form :rectangle [_]
  `(s/or :map (s/keys :req-un [:cljfx.rectangle/min-x
                               :cljfx.rectangle/min-y
                               :cljfx.rectangle/width
                               :cljfx.rectangle/height])))

(register-composite! :image-view
  :parent :node
  :props '{:fit-height {:type :number}
           :fit-width {:type :number}
           :image {:type :image}
           :preserve-ratio {:type :boolean}
           :smooth {:type :boolean}
           :viewport {:type :rectangle}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.image.ImageView)

(register-composite! :inner-shadow
  :props '{:blur-type {:type :enum :of javafx.scene.effect.BlurType}
           :choke {:type :number}
           :color {:type :color}
           :height {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :offset-x {:type :number}
           :offset-y {:type :number}
           :radius {:type :number}
           :width {:type :number}}
  :of 'javafx.scene.effect.InnerShadow)

(register-composite! :integer-spinner-value-factory
  :parent :spinner-value-factory
  :req [:min :max]
  :props '{:amount-to-step-by {:type :number}
           :max {:type :number}
           :min {:type :number}
           :value {:type :number}}
  :of 'javafx.scene.control.SpinnerValueFactory$IntegerSpinnerValueFactory)

(register-composite! :label
  :parent :labeled
  :props '{:label-for {:type :desc :of javafx.scene.Node}}
  :of 'javafx.scene.control.Label)

(register-props! :light '{:color {:type :color}})

(register-composite! :light-distant
  :parent :light
  :props '{:azimuth {:type :number}
           :elevation {:type :number}}
  :of 'javafx.scene.effect.Light$Distant)

(register-composite! :light-point
  :parent :light
  :props '{:x {:type :number}
           :y {:type :number}
           :z {:type :number}}
  :of 'javafx.scene.effect.Light$Point)

(register-composite! :light-spot
  :parent :light-point
  :props '{:points-at-x {:type :number}
           :points-at-y {:type :number}
           :points-at-z {:type :number}
           :specular-exponent {:type :number}}
  :of 'javafx.scene.effect.Light$Spot)

(register-composite! :lighting
  :props '{:bump-input {:type :desc :of javafx.scene.effect.Effect}
           :content-input {:type :desc :of javafx.scene.effect.Effect}
           :diffuse-constant {:type :number}
           :light {:type :desc :of javafx.scene.effect.Light}
           :specular-constant {:type :number}
           :specular-exponent {:type :number}
           :surface-scale {:type :number}}
  :of 'javafx.scene.effect.Lighting)

(register-composite! :line
  :parent :shape
  :props '{:end-x {:type :number}
           :end-y {:type :number}
           :fill {:type :paint}
           :start-x {:type :number}
           :start-y {:type :number}
           :stroke {:type :paint}}
  :of 'javafx.scene.shape.Line)

(register-composite! :line-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :props '{:axis-sorting-policy {:type :enum :of javafx.scene.chart.LineChart$SortingPolicy}
           :create-symbols {:type :boolean}}
  :of 'javafx.scene.chart.LineChart)

(register-composite! :line-to
  :parent :path-element
  :props '{:x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.shape.LineTo)

(register-composite! :list-spinner-value-factory
  :parent :spinner-value-factory
  :req [:items]
  :props '{:items {:type :coll :item {:type :any}}}
  :of 'javafx.scene.control.SpinnerValueFactory$ListSpinnerValueFactory)

(register-composite! :list-view
  :parent :control
  :props '{:cell-factory {:type :cell-factory}
           :editable {:type :boolean}
           :fixed-cell-size {:type :number}
           :items {:type :coll :item {:type :any}}
           :on-edit-cancel {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-commit {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-start {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll-to {:type :event-handler :of javafx.event.EventHandler}
           :on-selected-item-changed {:type :event-handler :of 'javafx.beans.value.ChangeListener}
           :orientation {:type :enum :of javafx.geometry.Orientation}
           :placeholder {:type :desc :of javafx.scene.Node}
           :selection-mode {:type :enum :of javafx.scene.control.SelectionMode}}
  :of 'javafx.scene.control.ListView)

(register-composite! :media
  :req [:source]
  :props '{:on-error {:type :event-handler :of java.lang.Runnable}
           :source {:type :string}}
  :of 'javafx.scene.media.Media)

(defmethod keyword-prop->spec-form :media-player-state [_]
  `(s/spec #{:playing :paused :stopped}))

(register-composite! :media-player
  :req [:media]
  :props '{:audio-spectrum-interval {:type :number}
           :audio-spectrum-listener {:type :event-handler :of javafx.scene.media.AudioSpectrumListener}
           :audio-spectrum-num-bands {:type :number}
           :audio-spectrum-threshold {:type :number}
           :auto-play {:type :boolean}
           :balance {:type :number}
           :cycle-count {:type :number}
           :media {:type :desc :of javafx.scene.media.Media}
           :mute {:type :boolean}
           :on-end-of-media {:type :event-handler :of java.lang.Runnable}
           :on-error {:type :event-handler :of java.lang.Runnable}
           :on-halted {:type :event-handler :of java.lang.Runnable}
           :on-marker {:type :event-handler :of javafx.event.EventHandler}
           :on-paused {:type :event-handler :of java.lang.Runnable}
           :on-playing {:type :event-handler :of java.lang.Runnable}
           :on-ready {:type :event-handler :of java.lang.Runnable}
           :on-repeat {:type :event-handler :of java.lang.Runnable}
           :on-stalled {:type :event-handler :of java.lang.Runnable}
           :on-stopped {:type :event-handler :of java.lang.Runnable}
           :rate {:type :number}
           :start-time {:type :duration}
           :state {:type :media-player-state}
           :stop-time {:type :duration}
           :volume {:type :number}}
  :of 'javafx.scene.media.MediaPlayer)

(register-composite! :media-view
  :parent :node
  :props '{:fit-height {:type :number}
           :fit-width {:type :number}
           :media-player {:type :desc :of javafx.scene.media.MediaPlayer}
           :on-error {:type :event-handler :of javafx.event.EventHandler}
           :preserve-ratio {:type :boolean}
           :smooth {:type :boolean}
           :viewport {:type :rectangle}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.media.MediaView)

(register-composite! :menu
  :parent :menu-item
  :props '{:items {:type :coll
                   :item {:type :add-props
                          :props {:fx/key {:type :any}}
                          :to {:type :desc :of javafx.scene.control.MenuItem}}}
           :on-hidden {:type :event-handler :of javafx.event.EventHandler}
           :on-hiding {:type :event-handler :of javafx.event.EventHandler}
           :on-showing {:type :event-handler :of javafx.event.EventHandler}
           :on-shown {:type :event-handler :of javafx.event.EventHandler}}
  :of 'javafx.scene.control.Menu)

(register-composite! :menu-bar
  :parent :control
  :props '{:menus {:type :coll
                   :item {:type :add-props
                          :props {:fx/key {:type :any}}
                          :to {:type :desc :of javafx.scene.control.Menu}}}
           :use-system-menu-bar {:type :boolean}}
  :of 'javafx.scene.control.MenuBar)

(register-composite! :menu-button
  :parent :button-base
  :props '{:on-hidden {:type :event-handler :of javafx.event.EventHandler}
           :on-hiding {:type :event-handler :of javafx.event.EventHandler}
           :on-showing {:type :event-handler :of javafx.event.EventHandler}
           :on-shown {:type :event-handler :of javafx.event.EventHandler}
           :items {:type :coll
                   :item {:type :add-props
                          :props {:fx/key {:type :any}}
                          :to {:type :desc :of javafx.scene.control.MenuItem}}}
           :popup-side {:type :enum :of javafx.geometry.Side}}
  :of 'javafx.scene.control.MenuButton)

(register-composite! :mesh-view
  :parent :shape3d
  :props '{:mesh {:type :desc :of javafx.scene.shape.Mesh}}
  :of 'javafx.scene.shape.MeshView)

(register-composite! :motion-blur
  :props '{:angle {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :radius {:type :number}}
  :of 'javafx.scene.effect.MotionBlur)

(register-composite! :move-to
  :parent :path-element
  :props '{:x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.shape.MoveTo)

(register-props! :value-axis :axis
  '{:lower-bound {:type :number}
    :minor-tick-count {:type :number}
    :minor-tick-length {:type :number}
    :minor-tick-visible {:type :boolean}
    :tick-label-formatter {:type :string-converter}
    :upper-bound {:type :number}})

(register-composite! :number-axis
  :parent :value-axis
  :props '{:force-zero-in-range {:type :boolean}
           :tick-unit {:type :number}}
  :of 'javafx.scene.chart.NumberAxis)

(defmethod keyword-prop->spec-form :page-factory [_]
  `(s/or :fn fn?
         :instance (instance-of Callback)))

(register-composite! :pagination
  :parent :control
  :props '{:current-page-index {:type :number}
           :max-page-indicator-count {:type :number}
           :on-current-page-index-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :page-count {:type :number}
           :page-factory {:type :page-factory}}
  :of 'javafx.scene.control.Pagination)

(register-composite! :parallel-camera
  :parent :camera
  :props '{}
  :of 'javafx.scene.ParallelCamera)

(register-composite! :parallel-transition
  :parent :transition
  :props '{:children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}}
                             :to {:type :desc :of javafx.animation.Animation}}}
           :node {:type :desc :of javafx.scene.Node}}
  :of 'javafx.animation.ParallelTransition)

(defmethod keyword-prop->spec-form :text-formatter [_]
  `(s/or :instance (instance-of TextFormatter)
         :desc ~(keyword-prop->spec-form {:type :desc :of 'javafx.scene.control.TextFormatter})))

(register-props! :text-input-control :control
  '{:editable {:type :boolean}
    :font {:type :font}
    :on-text-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
    :prompt-text {:type :string}
    :text {:type :string}
    :text-formatter {:type :text-formatter}})

(register-composite! :text-field
  :parent :text-input-control
  :props '{:alignment {:type :enum :of javafx.geometry.Pos}
           :on-action {:type :event-handler :of javafx.event.EventHandler}
           :pref-column-count {:type :number}}
  :of 'javafx.scene.control.TextField)

(register-composite! :password-field
  :parent :text-field
  :props '{}
  :of 'javafx.scene.control.PasswordField)

(register-composite! :path
  :parent :shape
  :props '{:elements {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}}
                             :to {:type :desc :of javafx.scene.shape.PathElement}}}
           :fill {:type :paint}
           :fill-rule {:type :enum :of javafx.scene.shape.FillRule}
           :stroke {:type :paint}}
  :of 'javafx.scene.shape.Path)

(register-composite! :path-transition
  :parent :transition
  :props '{:duration {:type :duration}
           :node {:type :desc :of javafx.scene.Node}
           :orientation {:type :enum :of javafx.animation.PathTransition$OrientationType}
           :path {:type :desc :of javafx.scene.shape.Shape}}
  :of 'javafx.animation.PathTransition)

(register-composite! :pause-transition
  :parent :transition
  :props '{:duration {:type :duration}}
  :of 'javafx.animation.PauseTransition)

(register-composite! :perspective-camera
  :parent :camera
  :props '{:field-of-view {:type :number}
           :vertical-field-of-view {:type :boolean}}
  :of 'javafx.scene.PerspectiveCamera)

(register-composite! :perspective-transform
  :props '{:input {:type :desc :of javafx.scene.effect.Effect}
           :llx {:type :number}
           :lly {:type :number}
           :lrx {:type :number}
           :lry {:type :number}
           :ulx {:type :number}
           :uly {:type :number}
           :urx {:type :number}
           :ury {:type :number}}
  :of 'javafx.scene.effect.PerspectiveTransform)

(register-composite! :phong-material
  :props '{:bump-map {:type :image}
           :diffuse-color {:type :color}
           :diffuse-map {:type :image}
           :self-illumination-map {:type :image}
           :specular-color {:type :color}
           :specular-map {:type :image}
           :specular-power {:type :number}}
  :of 'javafx.scene.paint.PhongMaterial)

(register-composite! :pie-chart
  :parent :chart
  :props '{:clockwise {:type :boolean}
           :data {:type :coll
                  :item {:type :add-props
                         :props {:fx/key {:type :any}}
                         :to {:type :desc :of javafx.scene.chart.PieChart$Data}}}
           :label-line-length {:type :number}
           :labels-visible {:type :boolean}
           :start-angle {:type :number}}
  :of 'javafx.scene.chart.PieChart)

(register-composite! :pie-chart-data
  :req [:name :pie-value]
  :props '{:name {:type :string}
           :pie-value {:type :number}}
  :of 'javafx.scene.chart.PieChart$Data)

(register-composite! :point-light
  :parent :light-base
  :props '{}
  :of 'javafx.scene.PointLight)

(register-composite! :polygon
  :parent :shape
  :props '{:points {:type :coll :item {:type :number}}}
  :of 'javafx.scene.shape.Polygon)

(register-composite! :polyline
  :parent :shape
  :props '{:fill {:type :paint}
           :points {:type :coll :item {:type :number}}
           :stroke {:type :paint}}
  :of 'javafx.scene.shape.Polyline)

(register-composite! :popup
  :parent :popup-window
  :props '{:content {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}}
                            :to {:type :desc :of javafx.scene.Node}}}}
  :of 'javafx.stage.Popup)

(register-composite! :progress-indicator
  :parent :control
  :props '{:progress {:type :number}}
  :of 'javafx.scene.control.ProgressIndicator)

(register-composite! :progress-bar
  :parent :progress-indicator
  :props '{}
  :of 'javafx.scene.control.ProgressBar)

(register-composite! :quad-curve
  :parent :shape
  :props '{:control-x {:type :number}
           :control-y {:type :number}
           :end-x {:type :number}
           :end-y {:type :number}
           :start-x {:type :number}
           :start-y {:type :number}}
  :of 'javafx.scene.shape.QuadCurve)

(register-composite! :quad-curve-to
  :parent :path-element
  :props '{:control-x {:type :number}
           :control-y {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.shape.QuadCurveTo)

(defmethod keyword-prop->spec-form :toggle-group [_]
  `(s/or :instance (instance-of ToggleGroup)
         :desc ~(keyword-prop->spec-form '{:type :desc :of javafx.scene.control.ToggleGroup})))

(register-composite! :toggle-button
  :parent :button-base
  :props '{:on-selected-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :selected {:type :boolean}
           :toggle-group {:type :toggle-group}}
  :of 'javafx.scene.control.ToggleButton)

(register-composite! :radio-button
  :parent :toggle-button
  :props '{:accessible-role {:type :enum :of javafx.scene.AccessibleRole}
           :style-class {:type :string}}
  :of 'javafx.scene.control.RadioButton)

(register-composite! :radio-menu-item
  :parent :menu-item
  :props '{:on-selected-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :selected {:type :boolean}
           :toggle-group {:type :desc :of javafx.scene.control.ToggleGroup}}
  :of 'javafx.scene.control.RadioMenuItem)

(register-composite! :rectangle
  :parent :shape
  :props '{:arc-height {:type :number}
           :arc-width {:type :number}
           :height {:type :number}
           :width {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.shape.Rectangle)

(register-composite! :reflection
  :props '{:bottom-opacity {:type :number}
           :fraction {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :top-offset {:type :number}
           :top-opacity {:type :number}}
  :of 'javafx.scene.effect.Reflection)

(register-composite! :rotate
  :parent :transform
  :props '{:angle {:type :number}
           :axis {:type :point-3d}
           :pivot-x {:type :number}
           :pivot-y {:type :number}
           :pivot-z {:type :number}}
  :of 'javafx.scene.transform.Rotate)

(register-composite! :rotate-transition
  :parent :transition
  :props '{:axis {:type :point-3d}
           :by-angle {:type :number}
           :duration {:type :duration}
           :from-angle {:type :number}
           :node {:type :desc :of javafx.scene.Node}
           :to-angle {:type :number}}
  :of 'javafx.animation.RotateTransition)

(register-composite! :row-constraints
  :props '{:fill-height {:type :boolean}
           :max-height {:type :number}
           :min-height {:type :number}
           :percent-height {:type :number}
           :pref-height {:type :number}
           :valignment {:type :enum :of javafx.geometry.VPos}
           :vgrow {:type :enum :of javafx.scene.layout.Priority}}
  :of 'javafx.scene.layout.RowConstraints)

(register-composite! :scale
  :parent :transform
  :props '{:pivot-x {:type :number}
           :pivot-y {:type :number}
           :pivot-z {:type :number}
           :x {:type :number}
           :y {:type :number}
           :z {:type :number}}
  :of 'javafx.scene.transform.Scale)

(register-composite! :scale-transition
  :parent :transition
  :props '{:by-x {:type :number}
           :by-y {:type :number}
           :by-z {:type :number}
           :duration {:type :duration}
           :from-x {:type :number}
           :from-y {:type :number}
           :from-z {:type :number}
           :node {:type :desc :of javafx.scene.Node}
           :to-x {:type :number}
           :to-y {:type :number}
           :to-z {:type :number}}
  :of 'javafx.animation.ScaleTransition)

(register-composite! :scatter-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :props '{}
  :of 'javafx.scene.chart.ScatterChart)

(defmethod keyword-prop->spec-form :map [{:keys [key value]}]
  `(s/map-of ~(prop->spec-form key)
             ~(prop->spec-form value)))

(register-composite! :scene
  :req [:root]
  :props '{:accelerators {:type :map
                          :key {:type :key-combination}
                          :value {:type :event-handler :of java.lang.Runnable}}
           :camera {:type :desc :of javafx.scene.ParallelCamera}
           :cursor {:type :cursor}
           :event-dispatcher {:type javafx.event.EventDispatcher}
           :fill {:type :paint}
           :node-orientation {:type :enum :of javafx.geometry.NodeOrientation}
           :on-context-menu-requested {:type :event-handler :of javafx.event.EventHandler}
           :on-drag-detected {:type :event-handler :of javafx.event.EventHandler}
           :on-drag-done {:type :event-handler :of javafx.event.EventHandler}
           :on-drag-dropped {:type :event-handler :of javafx.event.EventHandler}
           :on-drag-entered {:type :event-handler :of javafx.event.EventHandler}
           :on-drag-exited {:type :event-handler :of javafx.event.EventHandler}
           :on-drag-over {:type :event-handler :of javafx.event.EventHandler}
           :on-focus-owner-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-height-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-input-method-text-changed {:type :event-handler :of javafx.event.EventHandler}
           :on-key-pressed {:type :event-handler :of javafx.event.EventHandler}
           :on-key-released {:type :event-handler :of javafx.event.EventHandler}
           :on-key-typed {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-clicked {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-drag-entered {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-drag-exited {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-drag-over {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-drag-released {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-dragged {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-entered {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-exited {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-moved {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-pressed {:type :event-handler :of javafx.event.EventHandler}
           :on-mouse-released {:type :event-handler :of javafx.event.EventHandler}
           :on-rotate {:type :event-handler :of javafx.event.EventHandler}
           :on-rotation-finished {:type :event-handler :of javafx.event.EventHandler}
           :on-rotation-started {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll-finished {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll-started {:type :event-handler :of javafx.event.EventHandler}
           :on-swipe-down {:type :event-handler :of javafx.event.EventHandler}
           :on-swipe-left {:type :event-handler :of javafx.event.EventHandler}
           :on-swipe-right {:type :event-handler :of javafx.event.EventHandler}
           :on-swipe-up {:type :event-handler :of javafx.event.EventHandler}
           :on-touch-moved {:type :event-handler :of javafx.event.EventHandler}
           :on-touch-pressed {:type :event-handler :of javafx.event.EventHandler}
           :on-touch-released {:type :event-handler :of javafx.event.EventHandler}
           :on-touch-stationary {:type :event-handler :of javafx.event.EventHandler}
           :on-width-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-zoom {:type :event-handler :of javafx.event.EventHandler}
           :on-zoom-finished {:type :event-handler :of javafx.event.EventHandler}
           :on-zoom-started {:type :event-handler :of javafx.event.EventHandler}
           :root {:type :desc :of javafx.scene.Parent}
           :stylesheets {:type :coll :item {:type :string}}
           :user-agent-stylesheet {:type :string}
           :user-data {:type :any}}
  :of 'javafx.scene.Scene)

(register-composite! :scroll-bar
  :parent :control
  :props '{:block-increment {:type :number}
           :max {:type :number}
           :min {:type :number}
           :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :orientation {:type :enum :of javafx.geometry.Orientation}
           :unit-increment {:type :number}
           :value {:type :number}
           :visible-amount {:type :number}}
  :of 'javafx.scene.control.ScrollBar)

(defmethod keyword-prop->spec-form :bounding-box [_]
  `(s/or
     :zero zero?
     :arity-4 (s/tuple number? number? number? number?)
     :arity-6 (s/tuple number? number? number? number? number? number?)
     :instance (instance-of BoundingBox)))

(register-composite! :scroll-pane
  :parent :control
  :props '{:content {:type :desc :of javafx.scene.Node}
           :fit-to-height {:type :boolean}
           :fit-to-width {:type :boolean}
           :hbar-policy {:type :enum :of javafx.scene.control.ScrollPane$ScrollBarPolicy}
           :hmax {:type :number}
           :hmin {:type :number}
           :hvalue {:type :number}
           :min-viewport-height {:type :number}
           :min-viewport-width {:type :number}
           :on-hvalue-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-vvalue-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :pannable {:type :boolean}
           :pref-viewport-height {:type :number}
           :pref-viewport-width {:type :number}
           :vbar-policy {:type :enum :of javafx.scene.control.ScrollPane$ScrollBarPolicy}
           :viewport-bounds {:type :bounding-box}
           :vmax {:type :number}
           :vmin {:type :number}
           :vvalue {:type :number}}
  :of 'javafx.scene.control.ScrollPane)

(register-composite! :separator
  :parent :control
  :props '{:halignment {:type :enum :of javafx.geometry.HPos}
           :orientation {:type :enum :of javafx.geometry.Orientation}
           :valignment {:type :enum :of javafx.geometry.VPos}}
  :of 'javafx.scene.control.Separator)

(register-composite! :sepia-tone
  :props '{:input {:type :desc :of javafx.scene.effect.Effect}
           :level {:type :number}}
  :of 'javafx.scene.effect.SepiaTone)

(register-composite! :sequential-transition
  :parent :transition
  :props '{:children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}}
                             :to {:type :desc :of javafx.animation.Animation}}}
           :node {:type :desc :of javafx.scene.Node}}
  :of 'javafx.animation.SequentialTransition)

(register-composite! :shadow
  :props '{:blur-type {:type :enum :of javafx.scene.effect.BlurType}
           :color {:type :color}
           :height {:type :number}
           :input {:type :desc :of javafx.scene.effect.Effect}
           :radius {:type :number}
           :width {:type :number}}
  :of 'javafx.scene.effect.Shadow)

(register-composite! :shear
  :parent :transform
  :props '{:pivot-x {:type :number}
           :pivot-y {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.transform.Shear)

(register-composite! :slider
  :parent :control
  :props '{:block-increment {:type :number}
           :label-formatter {:type :string-converter}
           :major-tick-unit {:type :number}
           :max {:type :number}
           :min {:type :number}
           :minor-tick-count {:type :number}
           :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :orientation {:type :enum :of javafx.geometry.Orientation}
           :show-tick-labels {:type :boolean}
           :show-tick-marks {:type :boolean}
           :snap-to-ticks {:type :boolean}
           :value {:type :number}
           :value-changing {:type :boolean}}
  :of 'javafx.scene.control.Slider)

(register-composite! :sphere
  :parent :shape3d
  :props '{:radius {:type :number}}
  :of 'javafx.scene.shape.Sphere)

(register-composite! :spinner
  :parent :control
  :props '{:initial-delay {:type :duration}
           :prompt-text {:type :string}
           :repeat-delay {:type :duration}
           :editable {:type :boolean}
           :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :value-factory {:type :desc :of javafx.scene.control.SpinnerValueFactory}}
  :of 'javafx.scene.control.Spinner)

(register-composite! :split-menu-button
  :parent :menu-button
  :props '{:accessible-role {:type :enum :of javafx.scene.AccessibleRole}
           :mnemonic-parsing {:type :boolean}
           :style-class {:type :string}}
  :of 'javafx.scene.control.SplitMenuButton)

(s/def :split-pane/resizable-with-parent boolean?)

(register-composite! :split-pane
  :parent :control
  :props '{:divider-positions {:type :coll :item {:type :number}}
           :items {:type :coll :item {:type :add-props
                                      :props {:fx/key {:type :any}
                                              :split-pane/resizable-with-parent {:type :boolean}}
                                      :to {:type :desc :of javafx.scene.Node}}}
           :orientation {:type :enum :of javafx.geometry.Orientation}}
  :of 'javafx.scene.control.SplitPane)

(defenumspec :stack-pane/alignment javafx.geometry.Pos)
(s/def :stack-pane/margin :cljfx/insets)

(register-composite! :stack-pane
  :parent :pane
  :props '{:alignment {:type :enum
                       :of javafx.geometry.Pos}
           :children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}
                                     :stack-pane/alignment {:type :enum :of javafx.geometry.Pos}
                                     :stack-pane/margin {:type :insets}}
                             :to {:type :desc :of javafx.scene.Node}}}}
  :of 'javafx.scene.layout.StackPane)

(register-composite! :stacked-area-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :props '{:create-symbols {:type :boolean}}
  :of 'javafx.scene.chart.StackedAreaChart)

(register-composite! :stacked-bar-chart
  :parent :xy-chart
  :req [:x-axis :y-axis]
  :props '{:category-gap {:type :number}}
  :of 'javafx.scene.chart.StackedBarChart)

(register-composite! :stroke-transition
  :parent :transition
  :props '{:duration {:type :duration}
           :from-value {:type :color}
           :shape {:type :desc :of javafx.scene.shape.Shape}
           :to-value {:type :color}}
  :of 'javafx.animation.StrokeTransition)

(register-composite! :sub-scene
  :parent :node
  :req [:root :width :height]
  :props '{:camera {:type :desc :of javafx.scene.Camera}
           :fill {:type :paint}
           :height {:type :number}
           :root {:type :desc :of javafx.scene.Parent}
           :user-agent-stylesheet {:type :string}
           :width {:type :number}}
  :of 'javafx.scene.SubScene)

(register-composite! :svg-path
  :parent :shape
  :props '{:content {:type :string}
           :fill-rule {:type :enum :of javafx.scene.shape.FillRule}}
  :of 'javafx.scene.shape.SVGPath)

(register-composite! :tab
  :props '{:closable {:type :boolean}
           :content {:type :desc :of javafx.scene.Node}
           :context-menu {:type :desc :of javafx.scene.control.ContextMenu}
           :disable {:type :boolean}
           :graphic {:type :desc :of javafx.scene.Node}
           :id {:type :string}
           :on-close-request {:type :event-handler :of javafx.event.EventHandler}
           :on-closed {:type :event-handler :of javafx.event.EventHandler}
           :on-selection-changed {:type :event-handler :of javafx.event.EventHandler}
           :style {:type :style}
           :style-class {:type :string}
           :text {:type :string}
           :tooltip {:type :desc :of javafx.scene.control.Tooltip}
           :user-data {:type :any}}
  :of 'javafx.scene.control.Tab)

(register-composite! :tab-pane
 :parent :control
 :props '{:tab-closing-policy {:type :enum :of javafx.scene.control.TabPane$TabClosingPolicy}
          :tab-drag-policy {:type :enum :of javafx.scene.control.TabPane$TabDragPolicy}
          :on-tabs-changed {:type :event-handler :of javafx.collections.ListChangeListener}
          :rotate-graphic {:type :boolean}
          :side {:type :enum :of javafx.geometry.Side}
          :tab-max-height {:type :number}
          :tab-max-width {:type :number}
          :tab-min-height {:type :number}
          :tab-min-width {:type :number}
          :tabs {:type :coll
                 :item {:type :add-props
                        :props {:fx/key {:type :any}}
                        :to {:type :desc :of javafx.scene.control.Tab}}}}
 :of 'javafx.scene.control.TabPane)

(register-composite! :table-cell
  :parent :indexed-cell
  :props '{}
  :of 'javafx.scene.control.TableCell)

(register-props! :table-column-base
  '{:columns {:type :coll
              :item {:type :add-props
                     :props {:fx/key {:type :any}}
                     :to {:type :desc :of javafx.scene.control.TableColumnBase}}}
    :comparator {:type java.util.Comparator}
    :context-menu {:type :desc :of javafx.scene.control.ContextMenu}
    :editable {:type :boolean}
    :graphic {:type :desc :of javafx.scene.Node}
    :id {:type :string}
    :max-width {:type :number}
    :min-width {:type :number}
    :pref-width {:type :number}
    :reorderable {:type :boolean}
    :resizable {:type :boolean}
    :sort-node {:type :desc :of javafx.scene.Node}
    :sortable {:type :boolean}
    :style {:type :string}
    :style-class {:type :style-class}
    :text {:type :string}
    :user-data {:type :any}
    :visible {:type :boolean}})

(defmethod keyword-prop->spec-form :cell-value-factory [_]
  `(s/or :fn ifn?
         :instance Callback))

(register-composite! :table-column
  :parent :table-column-base
  :props '{:cell-factory {:type :cell-factory}
           :cell-value-factory {:type :cell-value-factory}
           :columns {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}}
                            :to {:type :desc :of javafx.scene.control.TableColumn}}}
           :on-edit-cancel {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-commit {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-start {:type :event-handler :of javafx.event.EventHandler}
           :sort-type {:type :enum :of javafx.scene.control.TableColumn$SortType}}
  :of 'javafx.scene.control.TableColumn)

(register-composite! :table-row
  :parent :indexed-cell
  :props '{}
  :of 'javafx.scene.control.TableRow)

(defmethod keyword-prop->spec-form :column-resize-policy [_]
  `(s/or
     :enum #{:unconstrained :constrained}
     :fn fn?
     :instance (instance-of Callback)))

(defmethod keyword-prop->spec-form :table-sort-policy [_]
  `(s/or
     :enum #{:default}
     :instance (instance-of Callback)))

(register-composite! :table-view
  :parent :control
  :props '{:column-resize-policy {:type :column-resize-policy}
           :columns {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}}
                            :to {:type :desc :of javafx.scene.control.TableColumn}}}
           :editable {:type :boolean}
           :fixed-cell-size {:type :number}
           :items {:type :coll :item {:type :any}}
           :on-scroll-to {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll-to-column {:type :event-handler :of javafx.event.EventHandler}
           :on-selected-item-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-sort {:type :event-handler :of javafx.event.EventHandler}
           :placeholder {:type :desc :of javafx.scene.Node}
           :row-factory {:type :cell-factory}
           :selection-mode {:type :enum :of javafx.scene.control.SelectionMode}
           :sort-order {:type :coll
                        :item {:type :add-props
                               :props {:fx/key {:type :any}}
                               :to {:type :desc
                                    :of javafx.scene.control.TableColumn}}}
           :sort-policy {:type :table-sort-policy}
           :table-menu-button-visible {:type :boolean}}
  :of 'javafx.scene.control.TableView)

(register-composite! :text
  :parent :shape
  :props '{:caret-bias {:type :boolean}
           :caret-position {:type :number}
           :selection-end {:type :number}
           :selection-fill {:type :paint}
           :selection-start {:type :number}
           :tab-size {:type :number}
           :bounds-type {:type :enum :of javafx.scene.text.TextBoundsType}
           :font {:type :font}
           :font-smoothing-type {:type :enum :of javafx.scene.text.FontSmoothingType}
           :line-spacing {:type :number}
           :strikethrough {:type :boolean}
           :text {:type :string}
           :text-alignment {:type :enum :of javafx.scene.text.TextAlignment}
           :text-origin {:type :enum :of javafx.geometry.VPos}
           :underline {:type :boolean}
           :wrapping-width {:type :number}
           :x {:type :number}
           :y {:type :number}}
  :of 'javafx.scene.text.Text)

(register-composite! :text-area
  :parent :text-input-control
  :props '{:pref-column-count {:type :number}
           :pref-row-count {:type :number}
           :scroll-left {:type :number}
           :scroll-top {:type :number}
           :wrap-text {:type :boolean}}
  :of 'javafx.scene.control.TextArea)

(register-composite! :text-field-list-cell
  :parent :list-cell
  :props '{:converter {:type :string-converter}}
  :of 'javafx.scene.control.cell.TextFieldListCell)

(register-composite! :text-flow
  :parent :pane
  :props '{:line-spacing {:type :number}
           :tab-size {:type :number}
           :text-alignment {:type :enum :of javafx.scene.text.TextAlignment}}
  :of 'javafx.scene.text.TextFlow)

(defmethod keyword-prop->spec-form :text-formatter-filter [_]
  (s/or
    :nil nil?
    :fn fn?
    :instance (instance-of UnaryOperator)))

(register-composite! :text-formatter
  :req #{[:value-converter]
         [:filter]}
  :props '{:filter {:type :text-formatter-filter}
           :on-value-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :value {:type :any}
           :value-converter {:type :string-converter}}
  :of 'javafx.scene.control.TextFormatter)

(register-composite! :text-input-dialog
  :parent :dialog
  :props '{}
  :of 'javafx.scene.control.TextInputDialog)

(s/def :tile-pane/margin :cljfx/insets)
(defenumspec :tile-pane/alignment javafx.geometry.Pos)

(register-composite! :tile-pane
 :parent :pane
 :props '{:alignment {:type :enum :of javafx.geometry.Pos}
          :children {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}
                                    :tile-pane/margin {:type :insets}
                                    :tile-pane/alignment {:type :enum :of javafx.geometry.Pos}}
                            :to {:type :desc :of javafx.scene.Node}}}
          :hgap {:type :number}
          :orientation {:type :enum :of javafx.geometry.Orientation}
          :pref-columns {:type :number}
          :pref-rows {:type :number}
          :pref-tile-height {:type :number}
          :pref-tile-width {:type :number}
          :tile-alignment {:type :enum :of javafx.geometry.Pos}
          :vgap {:type :number}}
 :of 'javafx.scene.layout.TilePane)

(register-composite! :titled-pane
  :parent :labeled
  :props '{:animated {:type :boolean}
           :collapsible {:type :boolean}
           :content {:type :desc :of javafx.scene.Node}
           :expanded {:type :boolean}}
  :of 'javafx.scene.control.TitledPane)

(register-composite! :toggle-group
  :props '{:toggles {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}}
                            :to {:type :desc :of javafx.scene.control.Toggle}}}
           :user-data {:type :any}}
  :of 'javafx.scene.control.ToggleGroup)

(register-composite! :tool-bar
  :parent :control
  :props '{:items {:type :coll
                   :item {:type :add-props
                          :props {:fx/key {:type :any}}
                          :to {:type :desc :of javafx.scene.Node}}}
           :orientation {:type :enum :of javafx.geometry.Orientation}}
  :of 'javafx.scene.control.ToolBar)

(register-composite! :tooltip
  :parent :popup-control
  :props '{:hide-delay {:type :duration}
           :show-delay {:type :duration}
           :show-duration {:type :duration}
           :content-display {:type :enum :of javafx.scene.control.ContentDisplay}
           :font {:type :font}
           :graphic {:type :desc :of javafx.scene.Node}
           :graphic-text-gap {:type :number}
           :install-to {:type :desc :of javafx.scene.Node}
           :text {:type :string}
           :text-alignment {:type :enum :of javafx.scene.text.TextAlignment}
           :text-overrun {:type :enum :of javafx.scene.control.OverrunStyle}
           :wrap-text {:type :boolean}}
  :of 'javafx.scene.control.Tooltip)

(register-composite! :translate
  :parent :transform
  :props '{:x {:type :number}
           :y {:type :number}
           :z {:type :number}}
  :of 'javafx.scene.transform.Translate)

(register-composite! :translate-transition
  :parent :transition
  :props '{:by-x {:type :number}
           :by-y {:type :number}
           :by-z {:type :number}
           :duration {:type :duration}
           :from-x {:type :number}
           :from-y {:type :number}
           :from-z {:type :number}
           :node {:type :desc :of javafx.scene.Node}
           :to-x {:type :number}
           :to-y {:type :number}
           :to-z {:type :number}}
  :of 'javafx.animation.TranslateTransition)

(register-composite! :tree-cell
  :parent :indexed-cell
  :props '{:disclosure-node {:type :desc :of javafx.scene.Node}}
  :of 'javafx.scene.control.TreeCell)

(register-composite! :tree-item
  :props '{:children {:type :coll
                      :item {:type :add-props
                             :props {:fx/key {:type :any}}
                             :to {:type :desc :of javafx.scene.control.TreeItem}}}
           :expanded {:type :boolean}
           :graphic {:type :desc :of javafx.scene.Node}
           :on-expanded-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :value {:type :any}}
  :of 'javafx.scene.control.TreeItem)

(register-composite! :tree-table-cell
  :parent :indexed-cell
  :props '{:accessible-role {:type :enum :of javafx.scene.AccessibleRole}
           :style-class {:type :style-class}}
  :of 'javafx.scene.control.TreeTableCell)

(register-composite! :tree-table-column
  :parent :table-column-base
  :props '{:cell-factory {:type :cell-factory}
           :cell-value-factory {:type :cell-value-factory}
           :columns {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}}
                            :to {:type :desc :of javafx.scene.control.TreeTableColumn}}}
           :on-edit-cancel {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-commit {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-start {:type :event-handler :of javafx.event.EventHandler}
           :sort-type {:type :enum :of javafx.scene.control.TreeTableColumn$SortType}}
  :of 'javafx.scene.control.TreeTableColumn)

(register-composite! :tree-table-row
  :parent :indexed-cell
  :props '{:disclosure-node {:type :desc :of javafx.scene.Node}}
  :of 'javafx.scene.control.TreeTableRow)

(register-composite! :tree-table-view
  :parent :control
  :props '{:column-resize-policy {:type :column-resize-policy}
           :columns {:type :coll
                     :item {:type :add-props
                            :props {:fx/key {:type :any}}
                            :to {:type :desc :of javafx.scene.control.TreeTableColumn}}}
           :editable {:type :boolean}
           :fixed-cell-size {:type :number}
           :on-scroll-to {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll-to-column {:type :event-handler :of javafx.event.EventHandler}
           :on-selected-item-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :on-sort {:type :event-handler :of javafx.event.EventHandler}
           :placeholder {:type :desc :of javafx.scene.Node}
           :root {:type :desc :of javafx.scene.control.TreeItem}
           :row-factory {:type :cell-factory}
           :selection-mode {:type :enum :of javafx.scene.control.SelectionMode}
           :show-root {:type :boolean}
           :sort-mode {:type :enum :of javafx.scene.control.TreeSortMode}
           :sort-order {:type :coll
                        :item {:type :add-props
                               :props {:fx/key {:type :any}}
                               :to {:type :desc :of javafx.scene.control.TreeTableColumn}}}
           :sort-policy {:type :table-sort-policy}
           :table-menu-button-visible {:type :boolean}
           :tree-column {:type :desc :of javafx.scene.control.TreeTableColumn}}
  :of 'javafx.scene.control.TreeTableView)

(register-composite! :tree-view
  :parent :control
  :props '{:cell-factory {:type :cell-factory}
           :editable {:type :boolean}
           :fixed-cell-size {:type :number}
           :on-edit-cancel {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-commit {:type :event-handler :of javafx.event.EventHandler}
           :on-edit-start {:type :event-handler :of javafx.event.EventHandler}
           :on-scroll-to {:type :event-handler :of javafx.event.EventHandler}
           :on-selected-item-changed {:type :event-handler :of javafx.beans.value.ChangeListener}
           :root {:type :desc :of javafx.scene.control.TreeItem}
           :selection-mode {:type :enum :of javafx.scene.control.SelectionMode}
           :show-root {:type :boolean}}
  :of 'javafx.scene.control.TreeView)

(defmethod keyword-prop->spec-form :vertex-format [_]
  `(s/or :enum #{:point-texcoord :point-normal-texcoord}
         :instance (instance-of VertexFormat)))

(register-composite! :triangle-mesh
  :props '{:face-smoothing-groups {:type :coll :item {:type :number}}
           :faces {:type :coll :item {:type :number}}
           :normals {:type :coll :item {:type :number}}
           :points {:type :coll :item {:type :number}}
           :tex-coords {:type :coll :item {:type :number}}
           :vertex-format {:type :vertex-format}}
  :of 'javafx.scene.shape.TriangleMesh)

(s/def :v-box/margin :cljfx/insets)
(s/def :v-box/vgrow :cljfx/priority)

(register-composite! :v-box
 :parent :pane
 :props '{:alignment {:type :enum :of javafx.geometry.Pos}
          :children {:type :coll :item {:type :add-props
                                        :props {:fx/key {:type :any}
                                                :v-box/margin {:type :insets}
                                                :v-box/vgrow {:type :enum :of javafx.scene.layout.Priority}}
                                        :to {:type :desc :of javafx.scene.Node}}}
          :fill-width {:type :boolean}
          :spacing {:type :number}}
 :of 'javafx.scene.layout.VBox)

(register-composite! :v-line-to
  :parent :path-element
  :props '{:y {:type :number}}
  :of 'javafx.scene.shape.VLineTo)

(register-composite! :web-view
 :parent :parent
 :props '{:context-menu-enabled {:type :boolean}
          :font-scale {:type :number}
          :font-smoothing-type {:type :enum :of javafx.scene.text.FontSmoothingType}
          :max-height {:type :number}
          :max-width {:type :number}
          :min-height {:type :number}
          :min-width {:type :number}
          :pref-height {:type :number}
          :pref-width {:type :number}
          :url {:type :string}
          :zoom {:type :number}}
 :of 'javafx.scene.web.WebView)

(register-composite! :xy-chart-data
  :props '{:extra-value {:type java.lang.Object}
           :node {:type :desc :of javafx.scene.Node}
           :x-value {:type :any}
           :y-value {:type :any}}
  :of 'javafx.scene.chart.XYChart$Data)

(register-composite! :xy-chart-series
  :props '{:data {:type :coll
                  :item {:type :add-props
                         :props {:fx/key {:type :any}}
                         :to {:type :desc :of javafx.scene.chart.XYChart$Data}}}
           :name {:type :string}
           :node {:type :desc :of javafx.scene.Node}}
  :of 'javafx.scene.chart.XYChart$Series)

(comment
  (require '[clojure.java.io :as io])
  (defn- prop-forms->descs [xs & {:keys [on-class imports]}]
    (let [conform-or-nil (fn [spec value]
                           (let [ret (s/conform spec value)]
                             (if (s/invalid? ret) nil ret)))
          resolve-class (fn [k]
                          (let [field-name (-> k
                                               name
                                               (str/replace #"-(\w)" (comp str/upper-case second)))]
                           (or
                             (when on-class
                               (try
                                 (-> ^Class on-class
                                     (.getDeclaredField field-name)
                                     (.getGenericType)
                                     (.getActualTypeArguments)
                                     ^Class first
                                     .getTypeName
                                     symbol)
                                 (catch Exception _ nil)))
                             (when on-class
                               (try
                                 (-> ^Class on-class
                                     (.getDeclaredField field-name)
                                     (.getType)
                                     (.getName)
                                     ({"javafx.beans.property.StringProperty" :string}))
                                 (catch Exception _ nil)))
                             '???)))
          ret (->> xs
                   (partition-all 2)
                   (map (fn [[k v]]
                          (let [type (condp conform-or-nil v
                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :enum (s/spec (s/cat :sym #{'coerce/enum}
                                                                   :cls symbol?))
                                              :suffix (s/* any?))
                                       :>> #(hash-map
                                              :type :enum
                                              :of
                                              (let [cls (:cls (:enum %))]
                                                (symbol (str (or
                                                               (some
                                                                 (fn [[p & clss]]
                                                                   (when (some #{cls} clss)
                                                                     p))
                                                                 imports)
                                                               "???")
                                                             "."
                                                             cls))))

                                       (s/cat :prefix (s/* any?)
                                              :default #{:default}
                                              :bool boolean?)
                                       {:type :boolean}

                                       (s/cat :prefix (s/* any?)
                                              :default #{:default}
                                              :str string?)
                                       {:type :string}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'double}
                                              :suffix (s/* any?))
                                       {:type :number}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'coerce/color}
                                              :suffix (s/* any?))
                                       {:type :color}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'coerce/duration}
                                              :suffix (s/* any?))
                                       {:type :duration}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'coerce/font}
                                              :suffix (s/* any?))
                                       {:type :font}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'coerce/pref-or-computed-size-double}
                                              :suffix (s/* any?))
                                       {:type :pref-or-computed-size-double}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'coerce/computed-size-double}
                                              :suffix (s/* any?))
                                       {:type :computed-size-double}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'coerce/image}
                                              :suffix (s/* any?))
                                       {:type :image}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :double #{'int}
                                              :suffix (s/* any?))
                                       {:type :number}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :eh #{'coerce/event-handler})
                                       {:type :event-handler :of 'javafx.event.EventHandler}

                                       #{'[:property-change-listener lifecycle/change-listener]}
                                       {:type :event-handler :of 'javafx.beans.value.ChangeListener}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :style #{'coerce/style}
                                              :suffix (s/* any?))
                                       {:type :style}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :style #{'coerce/paint}
                                              :suffix (s/* any?))
                                       {:type :paint}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :style #{'coerce/style-class}
                                              :suffix (s/* any?))
                                       {:type :style-class}

                                       (s/cat :prefix (s/* any?)
                                              :coerce #{:coerce}
                                              :style #{'coerce/string-converter}
                                              :suffix (s/* any?))
                                       {:type :string-converter}


                                       #{'[:setter lifecycle/scalar]}
                                       {:type (resolve-class k)}

                                       #{'[:setter lifecycle/dynamic]}
                                       {:type :desc :of (resolve-class k)}

                                       #{'[:list lifecycle/dynamics]}
                                       {:type :coll
                                        :item {:type :add-props
                                               :props {:fx/key {:type :any}}
                                               :to {:type :desc :of (resolve-class k)}}}

                                       {:type '??? :form v})]
                            [k type])))
                   (into (sorted-map)))]
      ret))

  (defn- infer-reg [id]
    (let [fname (str/replace id #"-" "_")
          fstr (slurp (io/file (System/getProperty "user.dir") "src" "cljfx" "fx" (str fname ".clj")))
          read-after (fn [s]
                       (when-let [i (str/index-of fstr s)]
                         (with-open [r (clojure.lang.LineNumberingPushbackReader.
                                         (java.io.StringReader. (subs fstr (+ i (count s)))))]
                           (loop [i []]
                             (let [f (try (read {:eof :eof} r) (catch Exception _ :eof))]
                               (if (= :eof f)
                                 i
                                 (recur (conj i f))))))))
          defs (or (read-after ";; definitions")
                   (next (read-after "composite/props")))
          cls (first (read-after "composite/describe"))
          imports (read-after ":import")
          fqcls (some (fn [[p & clss]]
                        (when (some #{cls} clss)
                          (str p "." cls)))
                      imports)
          parent-kw (when-let [p (first (read-after "(merge"))]
                      (keyword (subs (namespace p) 3)))
          ret (if cls
                `(~(vlaaad.reveal/horizontal
                     (vlaaad.reveal/stream 'register-composite!)
                     vlaaad.reveal/separator
                     (vlaaad.reveal/stream (keyword id)))
                   ~@(when-let [p (first (read-after "(merge"))]
                       [(vlaaad.reveal/horizontal
                          (vlaaad.reveal/stream :parent)
                          vlaaad.reveal/separator
                          (vlaaad.reveal/stream parent-kw))])
                   ~@(when-let [req (seq (first (read-after ":ctor")))]
                       [(vlaaad.reveal/horizontal
                          (vlaaad.reveal/stream :req)
                          vlaaad.reveal/separator
                          (vlaaad.reveal/stream (vec req)))])
                   ~(vlaaad.reveal/horizontal
                      (vlaaad.reveal/stream :props)
                      vlaaad.reveal/separator
                      (vlaaad.reveal/raw-string "'" {:fill :symbol})
                      (vlaaad.reveal/stream (prop-forms->descs defs
                                                               :on-class (resolve (symbol fqcls))
                                                               :imports imports)))
                   ~(let [imps (read-after ":import")]
                      (vlaaad.reveal/horizontal
                        (vlaaad.reveal/stream :of)
                        vlaaad.reveal/separator
                        (vlaaad.reveal/stream
                          (some (fn [[p & clss]]
                                  (when (some #{cls} clss)
                                    (vlaaad.reveal/raw-string (str "'" p "." cls) {:fill :symbol})))
                                (read-after ":import"))))))
                `(~'register-props! ~(keyword id)
                   ~@(when parent-kw [parent-kw])
                   ~(vlaaad.reveal/horizontal
                      (vlaaad.reveal/raw-string "'" {:fill :symbol})
                      (vlaaad.reveal/stream (prop-forms->descs defs)))))
          ret-str (->> ((vlaaad.reveal.stream/stream-xf conj) [] ret)
                       (map #(apply str (map :text (mapcat :segments %))))
                       (clojure.string/join "\n"))]
      (fx/on-fx-thread
        (.setContent (javafx.scene.input.Clipboard/getSystemClipboard)
                     (doto (javafx.scene.input.ClipboardContent.)
                       (.putString ret-str))))

      ret)))
