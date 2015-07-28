(ns leiningen.datadoc
  (:require [clojure.java.io :as io]
            [grenada
             [core :as gren-core]
             [exporters :as exporters]]
            [grimoire.api.fs :as api.fs]
            grimoire.doc
            [jolly.core :as jolly]
            [leiningen.core.main :as lein]
            [plumbing
             [core :refer [fnk safe-get <-]]
             [graph :as graph]]))

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


;;;; Subtasks

;; REVIEW: Before each extraction we have to run clean. (RM 2015-07-28)
(defn extract []
  (lein/info "Extracting data from sources.")
  (lein/info "But not really right now."))

;; REVIEW: This step should incorporate manually supplied data. (RM 2015-07-28)
(defn jar []
  (lein/info "Creating JAR from extracted data.")
  (lein/info "But not really right now.")))

(defn deploy []
  (lein/info "Deploying Datadoc JAR.")
  (lein/info "But not really right now."))))

;; REVIEW: Separate cleanly between files that are extracted-only and files that
;;         are edited by the user. If we want to be extra safe, we could write a
;;         checksum of the extracted data and warn the user if the checksum
;;         changed. But people shouldn't mess with stuff in :target-path anyway.
;;         (RM 2015-07-28)
(defn clean []
  (lein/info "Removing Grenada files.")
  (lein/info "But not really right now."))


;;;; Main task

(def ^:private subtasks [#'extract #'jar #'deploy #'clean])
(def ^:private subtask-names (set  (map var->name subtasks)))

;; TODO: Add some progress messages. (RM 2015-07-28)
(defn datadoc
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
