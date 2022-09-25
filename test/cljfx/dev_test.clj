(ns cljfx.dev-test
  (:require [clojure.test :refer :all]
            [cljfx.api :as fx]
            [cljfx.dev :as fx.dev]
            [clojure.string :as str])
  (:import [java.io PrintStream ByteArrayOutputStream]))

(defn- validated [desc]
  @(fx/on-fx-thread
     (-> desc
         (fx/create-component {:fx.opt/type->lifecycle fx.dev/validating-type->lifecycle})
         fx/instance)))

(deftest create-test
  (is (some? (validated {:fx/type :label})))
  (is (thrown-with-msg? Exception #"Cljfx component stack" (validated {:fx/type :not-a-valid-id})))
  (is (thrown-with-msg? Exception #"Cljfx component stack" (validated {:fx/type :label
                                                                       :text :not-a-string})))
  (is (validated {:fx/type :label
                  :text "a string"}))
  (is (validated {:fx/type :v-box
                  :children [{:fx/type :label
                              :v-box/vgrow :always
                              :text "a string"}]}))
  (is (thrown-with-msg? Exception #"Cljfx component stack"
                        (validated {:fx/type :v-box
                                    :children [{:fx/type :label
                                                :v-box/vgrow true
                                                :text "a string"}]}))))

(deftest advance-test
  (let [opts {:fx.opt/type->lifecycle fx.dev/validating-type->lifecycle}
        c (fx/create-component
            {:fx/type :label
             :text "foo"}
            opts)
        _ (is (some? (fx/instance c)))
        _ (is (thrown-with-msg? Exception #"Cljfx component stack" (fx/advance-component
                                                                     c
                                                                     {:fx/type :label
                                                                      :text :not-a-string}
                                                                     opts)))]))

(deftest extensions-test
  (is (some? (validated {:fx/type :stage
                         :scene {:fx/type :scene
                                 :root {:fx/type :label}}})))
  (is (some? (validated {:fx/type :stage
                         :scene {:fx/type fx/ext-on-instance-lifecycle
                                 :desc {:fx/type :scene
                                        :root {:fx/type :label}}}})))
  (is (thrown-with-msg? Exception #"desc-of"
                        (validated {:fx/type :stage
                                    :scene {:fx/type fx/ext-on-instance-lifecycle
                                            :desc {:fx/type :label}}})))
  (is (some? (validated {:fx/type fx/ext-let-refs
                         :refs nil
                         :desc {:fx/type :label}}))))

(deftest cell-factories
  ;; can't catch since exceptions happen in JavaFX and end up printed to stderr,
  ;; but at least they are dev exceptions
  (let [baos (ByteArrayOutputStream.)
        err (PrintStream. baos)
        old-err System/err]
    (System/setErr err)
    @(fx/on-fx-thread
       (let [opts {:fx.opt/type->lifecycle fx.dev/validating-type->lifecycle}
             c (fx/create-component
                 {:fx/type :stage
                  :showing true
                  :scene {:fx/type :scene
                          :root {:fx/type :list-view
                                 :cell-factory {:fx/cell-type :list-cell
                                                :describe (fn [i]
                                                            {:graphic {:fx/type :label
                                                                       :text :not-a-string}})}
                                 :items (range 1)}}}
                 opts)]
         (fx/delete-component c opts)))
    (is (str/includes? (.toString baos) "Cljfx component stack"))
    (System/setErr old-err)))

