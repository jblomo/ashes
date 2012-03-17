(ns ashes.core
  (:use [clojure.set :only (union)])
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import [java.security MessageDigest]
           [java.math BigInteger]
           [java.io File]))

(defn -main [& args]
  (println (interpose " " (-> args
                            first
                            io/as-file
                            (.listFiles)))))

(def prefix "/mnt/us/documents/")

(def ordering ["read" "later" "new"])

(defn relative-name
  "Given a file and the directory it is rooted under, return just the relative
  path as a String."
  [file root]
  (.substring (str file) (count (str root))))

(defn hash-name
  "Given a file, return the string used to hash it for collections.json"
  [file]
  (str prefix file))

(defn str->sha1
  "Return the hex SHA1 digest of a string"
  [string]
  (->> (.getBytes string)
    (.digest (MessageDigest/getInstance "SHA-1"))
    (BigInteger. 1 )
    (format "%40x")))

(defn document->hash
  "Given a file, return the sha1 hash of the kindle filename. Note: kindle
  hashes start with asterix for some reason."
  [file]
  (str \*
       (str->sha1 (hash-name file))))

(defn readable?
  "Given a file, return whether it is readable on the kindle"
  [file]
  (and
    (.isFile file)
    (some #(.endsWith (.getName file) (str \. %)) ["pdf" "ps"])))

(defn computer-files
  "Given a directory, returns a map of levels to sets of documents.  Levels are
  implemented as subdirectories.  Currently, levels should be:
  new
  later
  read
  "
  [dir]
  (into {} (for [subdir (filter #(.isDirectory %) (.listFiles dir))]
             [(.getName subdir) (into #{} (map #(.getName %)
                                               (filter readable? (.listFiles subdir))))])))

(defn read-collections
  "Given a collections file, return a normalized version of it.
  collection-name: {items: [hashes], lastAccess: num}"
  [file]
  (let [collections (json/read-json (io/reader file) false)]
    (zipmap (map #(first (.split % "@")) (keys collections))
            (vals collections))))

(defn kindle-files
  "Given the root of a kindle directory, return a map of levels to sets of
  documents.  Levels are implemented in the collections.json file."
  [kindle-root]
  (let [doc-dir (File. kindle-root "documents")
        documents (filter readable? (file-seq doc-dir))
        collections (read-collections (File. kindle-root "system/collections.json"))
        sig-to-doc (into {} (for [doc documents]
                              [(document->hash (.getName doc)) doc]))]
    (into {} (for [[cname cinfo] collections]
               [cname (into #{} (map (comp #(.getName %) sig-to-doc)
                                     (cinfo "items")))]))))

(defn moved-files
  "For a given map of collections and collection name, return all the files that
  are in a higher priority"
  [collections cname]
  (let [higher (take-while (complement #{cname}) ordering)]
    (apply union (map collections higher))))

(defn merge-collections
  "Given the state of the computer and kindle as maps of collection_name=>files,
  return a merged state that respects the priority of the collection.  So if a
  document has been read anywhere, it is in the read collection."
  [kindle computer]
  (let [merged (merge-with union kindle computer)]
    (into {} (map #(vector (key %)
                           ; filter out all files in higher priority collections
                           (filter (complement (moved-files merged (key %)))
                                   (val %)))
                  merged))))

(defn collection-str
  "Return the JSON string used by Kindle to track collections"
  [collection]
  (json/json-str (zipmap (map #(str % "@en-US") (keys collection))
                         (map #(hash-map "items" (map document->hash %) "lastAccess" 0) (vals collection)))))

