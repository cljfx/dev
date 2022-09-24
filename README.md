[![Clojars Project](https://img.shields.io/clojars/v/io.github.cljfx/dev.svg)](https://clojars.org/io.github.cljfx/dev)

# Cljfx dev tools

Cljfx is a declarative, functional and extensible wrapper of JavaFX inspired by better parts of react and re-frame. Cljfx dev tools are a set of tools that can help with developing cljfx applications that should not be included into production distribution of the cljfx app.

## Installation

See latest version in Clojars 

## Requirements

Cljfx dev tools require Java 11 or later.

## Tools

### Props and types reference

If you don't remember the props required by some cljfx type, or if you don't know what even are the available types, you can use `cljfx.dev/help` to look up this information:

```clojure
(require 'cljfx.dev)
;; look up information about fx type:
(cljfx.dev/help :label)
;; Cljfx type:
;; :label
;; 
;; Instance class:
;; javafx.scene.control.Label
;; 
;; Props                            Value type     
;; :accessible-help                 string
;; :accessible-role                 either of: :button, :check-box, :check-menu-item, :combo-box, :context-menu, :date-picker, :decrement-button, :hyperlink, :image-view, :increment-button, :list-item, :list-view, :menu, :menu-bar, :menu-button, :menu-item, :node, :page-item, :pagination, :parent, :password-field, :progress-indicator, :radio-button, :radio-menu-item, :scroll-bar, :scroll-pane, :slider, :spinner, :split-menu-button, :tab-item, :tab-pane, :table-cell, :table-column, :table-row, :table-view, :text, :text-area, :text-field, :thumb, :titled-pane, :toggle-button, :tool-bar, :tooltip, :tree-item, :tree-table-cell, :tree-table-row, :tree-table-view, :tree-view
;; :accessible-role-description     string
;; ...etc.

;; look up information about a prop:
(cljfx.dev/help :label :graphic)
;; Prop of :label - :graphic
;; 
;; Cljfx desc, a map with :fx/type key
;; 
;; Required instance class:
;; javafx.scene.Node¹
;; 
;; ---
;; ¹javafx.scene.Node - Fitting cljfx types:
;;  Cljfx type               Class
;;  :accordion               javafx.scene.control.Accordion
;;  :ambient-light           javafx.scene.AmbientLight
;;  :anchor-pane             javafx.scene.layout.AnchorPane
;;  :arc                     javafx.scene.shape.Arc
;;  ...
```

### Improved error messages with spec

You can set custom type->lifecycle opt that will validate all cljfx component descriptions using spec and properly describes the error:

```clojure
;; suppose you have a simple app:
(require '[cljfx.api :as fx])

(defn message-view [{:keys [text]}]
  {:fx/type :label
   :text text})

(defn root-view [{:keys [text]}]
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type message-view
                  :text text}}})

(def state (atom {:text "Hello world"}))

;; you will use custom logic here to determine if it's a prod or dev, 
;; e.g. by using a system property: (Boolean/getBoolean "my-app.dev")
(def in-development? true)

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc #(assoc % :fx/type root-view))
    ;; print errors in REPL's *err* output
    :error-handler (bound-fn [^Throwable ex]
                     (.printStackTrace ^Throwable ex *err*))
    :opts (cond-> {}
            ;; Validate descriptions in dev
            in-development?
            (assoc :fx.opt/type->lifecycle @(requiring-resolve 'cljfx.dev/type->lifecycle)))))

(defn -main []
  (fx/mount-renderer state renderer))

;; then start the app:
(-main)
;; invalid state change - making text prop of a label not a string:
(swap! state assoc :text :not-a-string)
;; prints to *err*:
;; clojure.lang.ExceptionInfo: Invalid cljfx description of :label type:
;; :not-a-string - failed: string? in [:text]
;; 
;; Cljfx component stack:
;;   :label
;;   user/message-view
;;   :scene
;;   :stage
;;   user/root-view
;;   
;;     at cljfx.dev$ensure_valid_desc.invokeStatic(validation.clj:62)
;;     at cljfx.dev$ensure_valid_desc.invoke(validation.clj:58)
;;     at cljfx.dev$wrap_lifecycle$reify__22150.advance(validation.clj:80)
;;     at ...
```