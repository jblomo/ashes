(ns ashes.test.core
  (:require [clojure.java.io :as io])
  (:use [ashes.core])
  (:use [clojure.test]))

(def resource-file (comp io/as-file io/resource))

(deftest computer-files-test
  (is (= (computer-files (resource-file "computer"))
         {"new" #{"yay.pdf" "n-split.pdf" "my-new.pdf"}
          "later" #{"l-split.pdf"}
          "read" #{"r-split.pdf"}})))

(deftest kindle-files-test
  (is (= (kindle-files (resource-file "kindle"))
         {"new" #{"n-split.pdf"}
          "later" #{"l-split.pdf"}
          "read" #{"yay.pdf" "r-split.pdf"}})))

(deftest merge-files
  (let [cf (computer-files (resource-file "computer"))
        kf (kindle-files (resource-file "kindle"))
        merged (merge-collections cf kf)]
    (is (= merged
           {"new" #{"n-split.pdf" "my-new.pdf"}
            "later" #{"l-split.pdf"}
            "read" #{"yay.pdf" "r-split.pdf"}}))))
