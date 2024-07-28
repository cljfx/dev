(ns build
  (:require [clojure.tools.build.api :as b]
            [cemerick.pomegranate.aether :as aether]))

(def lib 'io.github.cljfx/dev)
(def version (format "1.0.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def pom-path (format "%s/META-INF/maven/%s/pom.xml" class-dir lib))

(defn deploy [_]
  (b/delete {:path "target"})
  (b/write-pom
    {:basis basis
     :class-dir class-dir
     :lib lib
     :version version
     :src-dirs ["src"]
     :scm {:url "https://github.com/cljfx/dev"
           :tag (b/git-process {:git-args ["rev-parse" "HEAD"]})}
     :pom-data [[:licenses
                 [:license
                  [:name "MIT"]
                  [:url "https://opensource.org/license/mit"]]]]})
  (b/copy-dir
    {:src-dirs ["src"]
     :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file})
  (b/git-process {:git-args ["tag" (str "v" version)]})
  (aether/deploy
    :coordinates [lib version]
    :jar-file jar-file
    :pom-file pom-path
    :repository (-> basis
                    :mvn/repos
                    (select-keys ["clojars"])
                    (update "clojars" assoc
                            :username "vlaaad"
                            :password (if-let [console (System/console)]
                                        (-> console
                                            (.readPassword "Clojars token:" (object-array 0))
                                            String/valueOf)
                                        (do (print "Clojars token:")
                                            (flush)
                                            (read-line))))))
  (b/git-process {:git-args ["push" "origin" (str "v" version)]}))
