(defproject org.clj-grenada/lein-datadoc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT license"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure-grimoire/lein-grim "0.3.8"]
                 [prismatic/plumbing "0.4.0"]]
  :aliases {"datadoc" ["run" "-m" "lein-datadoc.core"
                       :project/group :project/name :project/version
                       :project/target-path]})
