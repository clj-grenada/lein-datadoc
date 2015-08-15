(defproject org.clj-grenada/lein-datadoc "1.0.0-rc.1"
  :description "Leiningen plugin for creating and deploying Datadoc JARs"
  :url "https://github.com/clj-grenada/lein-datadoc"
  :license {:name "MIT license"
            :url "http://opensource.org/licenses/MIT"}
  :eval-in-leiningen true
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clj-grenada/jolly "1.0.0-rc.1"]
                 [org.clj-grenada/lib-grenada "1.0.0-rc.1"]
                 [org.clojars.rmoehn/lein-grim "0.3.10"]
                 [prismatic/plumbing "0.4.0"]])
