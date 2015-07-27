(ns lein-datadoc.core
  (:require [clojure.java.io :as io]
            grimoire.doc
            [plumbing.core :refer [safe-get]]))

;;; Note: This probably has to evolve to a full plugin. Having to provide
;;;       entries from project.clj through :aliases is already tedious, but
;;;       still doable. For the deployment we'll need Leiningen functionality,
;;;       though.

;; TODO: Make these configurable in project.clj.
(def config {:grim-out "grimoire-data"})

(defn -main
  "

  Arguments up to & to be supplied directly by Leiningen."
  [group artifact version target-path & source-paths]
  (grimoire.doc/-main group
                      artifact
                      version
                      source-paths
                      "source"
                      "clj"
                      (str (io/file target-path
                                    (safe-get config :grim-out)))))
