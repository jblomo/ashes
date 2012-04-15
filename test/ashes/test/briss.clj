(ns ashes.test.briss
  (:require [clojure.java.io :as io])
  (:use [ashes.briss]
        [clojure.test]
        [ashes.test.util :only (resource-file)])
  (:import [java.io File]))

(deftest split!-test
  (let [src (resource-file (apply str (interpose File/separator ["computer" "new" "cc-2c.pdf"])))
        dst (File/createTempFile "briss-test" ".pdf")
        result (split! src dst)]

    (is (= dst result))
    (is (.exists result))
    (is (pos? (.length result)))
    (is (not= (.length result)
              (.length src)))

    (is (.delete result)
        "Could not clean up test!")))

