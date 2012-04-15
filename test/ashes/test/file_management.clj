(ns ashes.test.file-management
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:use [ashes.file-management]
        [clojure.test]
        [ashes.test.util :only (resource-file)]))

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

(deftest merge-collection-test
  (let [cf (computer-files (resource-file "computer"))
        kf (kindle-files (resource-file "kindle"))
        merged (merge-collections cf kf)]
    (is (= merged
           {"new" #{"nw.pdf" "cc-2c.pdf"}
            "later" #{"ltr.pdf"}
            "read" #{"yay.pdf" "rd.pdf"}}))))

(deftest read-collections-test
  (let [collections (read-collections (resource-file "kindle/system/collections.json"))]
    (is (= #{"new" "later" "read"}
           (.keySet collections)))
    (is (= #{"*892b7daacf809077731e375c3ca6c84ef813a909" "*a821a177b3f54675a1247c7d4c947c8ae22bdb9a"}
           (set (get-in collections ["read" "items"]))))))

(deftest collection-str-test
  (let [cf (computer-files (resource-file "computer"))
        kf (kindle-files (resource-file "kindle"))
        merged (merge-collections cf kf)
        merged-collection (-> merged collection-str json/read-json)]
    (is (= (set (get-in merged-collection [(keyword "new@en-US") :items]))
           (set ["*6ae71f5edb8c14d0a77aa94fe4cca743097239cf"
                 "*7c37eba6d9e18a31412dbe2553cc2ed40f3a048b"])))))

(defn- cpr!
  "Recursive copy. src and dst are directories.  The *contents* of src will be
  copied *into* dst (just as if you included trainling slashes in the rsync
  command)."
  [src dst]
  (when (not (.exists dst))
    (.mkdir dst))

  (let [root-len (count (.getPath src))
        relative (fn [file]
                   (.. file getPath (substring (inc root-len))))]
    (doseq [path (rest (file-seq src))]
      (let [dst-file (io/file dst (relative path))]
        (if (.isDirectory path)
          (.mkdir dst-file)
          (io/copy path dst-file))))))

(defn- rmr!
  "Recursively delete a directory or file"
  [target]
  (every? boolean
          (for [file (reverse (file-seq target))]
            (.delete file))))

(deftest update-computer-test
  "Are we moving files to the right directories?"
  (let [src (resource-file "computer")
        testdir (io/file (.getParent src) "computer-update")
        _ (cpr! src testdir)
        cf (computer-files testdir)
        kf (kindle-files (resource-file "kindle"))
        merged (merge-collections cf kf)
        _ (update-computer! merged testdir)]

    (is (.exists (io/file testdir "read" "yay.pdf")))
    (is (not (.exists (io/file testdir "new" "yay.pdf"))))
    (is (.exists (io/file testdir "later" "ltr.pdf")))

    (is (rmr! testdir)
        "Could not clean up test!")))

(deftest computer->kindle!-test
  "Are we moving files into kindle documents correctly?"
  (let [c-root (resource-file "computer")
        k-root (resource-file "kindle")
        testfile (io/file k-root "documents" "cc-2c.pdf")
        srcfile  (io/file c-root "new" "cc-2c.pdf")
        cf (computer-files c-root)
        kf (kindle-files k-root)
        merged (merge-collections cf kf)
        _ (computer->kindle! merged c-root k-root)]
    
    (is (= 6
           (count (file-seq (io/file k-root "documents")))))
    (is (.exists testfile))
    (is (not= (.length testfile)
              (.length srcfile))
        "2 column file not translated")

    (is (.delete testfile)
        "Could not clean up test!")))

(deftest write-collections!-test
  "Are we writing a new collections file correctly?"
  (let [c-root (resource-file "computer")
        k-root (resource-file "kindle")
        cf (computer-files c-root)
        kf (kindle-files k-root)
        merged (merge-collections cf kf)
        testdir (io/file (.getParent k-root) "kindle-update")
        _ (io/make-parents (io/file testdir "system" "collections.json"))
        _ (write-collections! merged testdir)
        collections (read-collections (io/file testdir "system" "collections.json"))]

    (is (= #{"new" "later" "read"}
           (.keySet collections)))
    (is (= #{"*892b7daacf809077731e375c3ca6c84ef813a909" "*a821a177b3f54675a1247c7d4c947c8ae22bdb9a"}
           (set (get-in collections ["read" "items"]))))
    (is (= #{"*6ae71f5edb8c14d0a77aa94fe4cca743097239cf" "*7c37eba6d9e18a31412dbe2553cc2ed40f3a048b"}
           (set (get-in collections ["new" "items"]))))

    (is (rmr! testdir)
        "Could not cleanup test!")))


