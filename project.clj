(defproject org.clj-grenada/lein-datadoc "0.1.0-SNAPSHOT"
  :description "Leiningen plugin for creating and deploying Datadoc JARs"
  :url "https://github.com/clj-grenada/lein-datadoc"
  :license {:name "MIT license"
            :url "http://opensource.org/licenses/MIT"}
  :eval-in-leiningen true
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clj-grenada/jolly "0.1.0-SNAPSHOT"]
                 [org.clj-grenada/lib-grenada "0.3.3-SNAPSHOT"]

                 ;; This is from the local repo and not the actual 0.3.8.
                 [org.clojure-grimoire/lein-grim "0.3.8"]
                 [prismatic/plumbing "0.4.0"]])
