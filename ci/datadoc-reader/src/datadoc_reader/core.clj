(ns datadoc-reader.core
  (:require [grenada
             [aspects :as a]
             [converters :as converters]
             [sources :as sources]
             [things :as t]]))

(defn -main []
  (let [things (sources/from-depspec '[prismatic/schema "0.4.3"
                                       :classifier "datadoc"])
        a-thing (-> things
                    converters/to-mapping
                    (get ["prismatic" "schema" "0.4.3" "clj" "schema.core"
                          "either"]))]
    (assert (t/thing?+ a-thing))
    (assert (contains? (:aspects a-thing) ::a/fn))))
