(ns leiningen.datadoc
  (:require [clojure.java.io :as io]
            [grenada
             [core :as gren-core]
             [exporters :as exporters]]
            [grimoire.api.fs :as api.fs]
            grimoire.doc
            [jolly.core :as jolly]
            [plumbing
             [core :refer [fnk safe-get <-]]
             [graph :as graph]]))

;;; Note: This probably has to evolve to a full plugin. Having to provide
;;;       entries from project.clj through :aliases is already tedious, but
;;;       still doable. For the deployment we'll need Leiningen functionality,
;;;       though.

;; TODO: Make these configurable in project.clj. (RM 2015-07-27)
(defn get-config [target-path]
  ((graph/eager-compile
    {;; Name of directory in target-path where we put everything
     :grenada-dir (fnk [target-path] (io/file target-path "grenada"))

     ;; Where the Grimoire data should be stored
     :grim-out (fnk [grenada-dir] (io/file grenada-dir "grimoire-data"))

     ;; Configuration object for lib-grimoire calls
     :grim-config (fnk [grim-out]
                    (apply api.fs/->Config
                           (map str (repeat 3 grim-out))))

     ;; Where the raw Grenada data should be stored
     :grenada-out (fnk [grenada-dir] (io/file grenada-dir "grenada-data"))})
   {:target-path target-path}))


;; TODO: Add some progress messages. (RM 2015-07-28)
(defn -main
  "

  Arguments up to & to be supplied directly by Leiningen.

  Note that the JAR produced will have the same group, artifact and version as
  the main project JAR, but a different classifier."
  [group artifact version target-path & source-paths]
  (let [config (get-config target-path)]
    (grimoire.doc/-main group
                        artifact
                        version
                        source-paths
                        "--clobber"
                        "true"
                        "source"
                        "clj"
                        (str (safe-get config :grim-out)))

    (->> (jolly/read-all-things (safe-get config :grim-config))
         (jolly/grim-ts->gren-ts-with-bars (safe-get config :grim-config))
         (<- (exporters/fs-hier (safe-get config :grenada-out))))

    (gren-core/jar-from-files (safe-get config :grenada-out)
                              (safe-get config :grenada-dir)
                              {:group group
                               :artifact artifact
                               :version version})))
