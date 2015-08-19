(ns datadoc-reader.t-primitive
  (:require [clojure.test :refer [deftest testing is]]
            [grenada
             [aspects :as a]
             [converters :as converters]
             [sources :as sources]
             [things :as t]]))

;;; Note: This is a very low baseline of tests. I don't consider this proper
;;;       testing.

(deftest things-from-schema
  (let [a-thing (-> (sources/from-depspec '[prismatic/schema "0.4.3"
                                            :classifier "datadoc"])
                    converters/to-mapping
                    (get ["prismatic" "schema" "0.4.3" "clj" "schema.core"
                          "either"]))]
    (is (t/thing?+ a-thing))
    (is (t/has-aspect? ::a/fn a-thing))))

(deftest things-from-lib-grenada
  (let [ts-map (-> (sources/from-depspec '[org.clj-grenada/lib-grenada
                                           "1.0.0-rc.2" :classifier "datadoc"])
                   converters/to-mapping)
        bars-t (get ts-map ["org.clj-grenada" "lib-grenada" "1.0.0-rc.2" "clj"
                            "grenada.bars"])
        deftag+-t (get ts-map ["org.clj-grenada" "lib-grenada" "1.0.0-rc.2"
                               "clj" "grenada.guten-tag.more" "deftag+"])]
    (is (t/thing?+ bars-t))
    (is (t/has-aspect? ::t/namespace bars-t))
    (is (= :common-mark (get-in bars-t [:bars :doro.bars/markup-all])))

    (is (t/thing?+ deftag+-t))
    (is (t/has-aspect? ::a/macro deftag+-t))
    (is (vector? (get-in deftag+-t [:bars :voyt.bars/requires])))))
