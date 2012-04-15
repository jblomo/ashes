(ns ashes.test.file-management
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:use [ashes.file-management])
  (:use [clojure.test]))

(def resource-file (comp io/as-file io/resource))

(deftest computer-files-test
  (is (= (computer-files (resource-file "computer"))
         {"new" #{"yay.pdf" "nw.pdf" "cc-2c.pdf"}
          "later" #{"ltr.pdf"}
          "read" #{"rd.pdf"}})))

(deftest kindle-files-test
  (is (= (kindle-files (resource-file "kindle"))
         {"new" #{"nw.pdf"}
          "later" #{"ltr.pdf"}
          "read" #{"yay.pdf" "rd.pdf"}})))

(deftest merge-files-test
  (let [cf (computer-files (resource-file "computer"))
        kf (kindle-files (resource-file "kindle"))
        merged (merge-collections cf kf)]
    (is (= merged
           {"new" #{"nw.pdf" "cc-2c.pdf"}
            "later" #{"ltr.pdf"}
            "read" #{"yay.pdf" "rd.pdf"}}))))

(deftest merge-collections-test
  (let [cf (computer-files (resource-file "computer"))
        kf (kindle-files (resource-file "kindle"))
        merged (merge-collections cf kf)
        merged-collection (-> merged collection-str json/read-json)]
    (is (set (get-in merged-collection ["new@en-US" "items"]))
        (set ["*6ae71f5edb8c14d0a77aa94fe4cca743097239cf"
              "*7c37eba6d9e18a31412dbe2553cc2ed40f3a048b"]))))


