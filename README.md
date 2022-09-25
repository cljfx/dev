[![Clojars Project](https://img.shields.io/clojars/v/io.github.cljfx/dev.svg)](https://clojars.org/io.github.cljfx/dev)

# Cljfx dev tools

[Cljfx](https://github.com/cljfx/cljfx) is a declarative, functional and extensible wrapper of JavaFX inspired by better parts of react and re-frame. Cljfx dev tools are a set of tools that help with developing cljfx applications but should not be included into the production distribution of the cljfx app.

## Rationale

The default developer experience of cljfx has some issues:
- what are the allowed props for different JavaFX types is not clear and requires looking it up in the source code;
- what are the allowed JavaFX type keywords requires looking it up in the source code;
- errors when using non-existent props are unhelpful;
- generally, errors that happen during cljfx lifecycle are unhelpful because the stack traces have mostly cljfx internals instead of user code.

Cljfx dev tools solve these issues by providing:
- reference for cljfx types and props;
- specs for cljfx descriptions, so they can be validated;
- dev-time lifecycles that perform validation and add cljfx component stacks to exceptions to help with debugging;

## Installation

See latest version on [Clojars](https://clojars.org/io.github.cljfx/dev).

## Requirements

Cljfx dev tools require Java 11 or later.

## Tools

### Props and types reference

If you don't remember the props required by some cljfx type, or if you don't know what are the available types, you can use `cljfx.dev/help` to look up this information:

```clojure
(require 'cljfx.dev)

;; look up available types:
(cljfx.dev/help)
;; Available cljfx types:
;; Cljfx type                             Instance class
;; :accordion                             javafx.scene.control.Accordion
;; :affine                                javafx.scene.transform.Affine
;; ...etc



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
;; ...etc



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
;;  ...etc
```

### Improved error messages with spec

You can set validating type->lifecycle opt that will validate all cljfx component descriptions using spec and properly describe the errors:

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
    ;; optional: print errors in REPL's *err* output
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
If you already use custom type->lifecycle opt, instead of using `cljfx.dev/type->lifecycle` you can use `cljfx.dev/wrap-type->lifecycle` to wrap your type->lifecycle with validations. 

Additionally, you can validate individual descriptions while developing:
```clojure
(cljfx.dev/explain-desc
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :text-formatter
                  :value-converter :int}}})
;; :int - failed: #{:local-date-time :long :double :short :date-time :number :local-time :default :float :integer :byte :local-date :big-integer :boolean :character :big-decimal} in [:scene :root :value-converter]
;; :int - failed: (instance-of javafx.util.StringConverter) in [:scene :root :value-converter]

(cljfx.dev/explain-desc
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :text-formatter
                  :value-converter :integer}}})
;; {:fx/type :text-formatter, :value-converter :integer} - failed: (desc-of (quote javafx.scene.Parent)) in [:scene :root]

(cljfx.dev/explain-desc
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :text-field
                  :text-formatter {:fx/type :text-formatter
                                   :value-converter :integer}}}})
;; Success!
```