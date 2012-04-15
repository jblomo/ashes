(ns ashes.test.util
  "Utilities for testing"
  (:require [clojure.java.io :as io])
  (:use [clojure.test]))

(def resource-file (comp io/as-file io/resource))

