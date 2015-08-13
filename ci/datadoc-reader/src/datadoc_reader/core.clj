(ns datadoc-reader.core
  (:require [grenada
             [aspects :as a]
             [converters :as converters]
             [reading :as reading]
             [things :as t]]))

(defn -main []
  (let [things (reading/from-depspec '[prismatic/plumbing])
        a-thing (-> things
                    converters/to-mapping
                    (get-in ["prismatic" "plumbing" "0.4.4" "clj"
                             "plumbing.core" "safe-get"]))]
    (assert (t/thing?+ a-thing))
    (assert (contains? (:aspects a-thing) ::a/fn))))
