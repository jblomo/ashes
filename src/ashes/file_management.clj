(ns ashes.file-management
  (:use [clojure.set :only (union)])
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [ashes.briss :as briss])
  (:import [java.security MessageDigest]
           [java.math BigInteger]
           [java.io File]))

(defn- mv
  "Moves a File to another File (by copying and deleting)"
  [src dst]
  (io/copy src dst)
  (io/delete-file src))

(defn- kindle-copy
  "Copies files to the kindle in the preferred format (eg. 2 column -> 1 column translation)"
  [src dst]
  (cond
    ; 2 column
    (.endsWith (.name src) "-2c.pdf")
    (briss/split! src dst)

    ;default
    true
    (io/copy src dst)))


(defn- find-name
  "Given a root path and a file name, find that file under any of the
  subdirectories of root"
  [root file]
  (first (filter #(= file (.getName %))
                 (file-seq root))))

(def ^:private prefix
  "Document path when mounted by Kindle OS"
  "/mnt/us/documents/")

(defn- hash-name
  "Given a file, return the string used to hash it for collections.json"
  [file]
  (str prefix file))

(defn- str->sha1
  "Return the hex SHA1 digest of a string"
  [string]
  (->> (.getBytes string)
    (.digest (MessageDigest/getInstance "SHA-1"))
    (BigInteger. 1 )
    (format "%40x")))

(defn- document->hash
  "Given a file, return the sha1 hash of the kindle filename. Note: kindle
  hashes start with asterix for some reason."
  [file]
  (str \*
       (str->sha1 (hash-name file))))

(defn- readable?
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

(defn- read-collections
  "Given a collections file, return a normalized version of it.
  collection-name: {items: [hashes], lastAccess: num}"
  [file]
  (let [collections (json/read-json (io/reader file) false)]
    (zipmap (map #(first (.split % "@")) (keys collections))
            (vals collections))))

(defn collections-file
  "File object where collections are stored relative to the given kindle-root"
  [kindle-root]
  (io/file kindle-root "system" "collections.json"))

(defn kindle-files
  "Given the root of a kindle directory, return a map of levels to sets of
  documents.  Levels are implemented in the collections.json file."
  [kindle-root]
  (let [doc-dir (File. kindle-root "documents")
        documents (filter readable? (file-seq doc-dir))
        collections (read-collections (collections-file kindle-root))
        sig-to-doc (into {} (for [doc documents]
                              [(document->hash (.getName doc)) doc]))]
    (into {} (for [[cname cinfo] collections]
               [cname (into #{} (map (comp #(.getName %) sig-to-doc)
                                     (cinfo "items")))]))))

(def ^:private ordering
  "Priority ordering: conflict resolution when collections don't match betwen computer and kindle."
  ["read" "later" "new"])

(defn- moved-files
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
                           (set (filter (complement (moved-files merged (key %)))
                                   (val %))))
                  merged))))

(defn collection-str
  "Return the JSON string used by Kindle to track collections"
  [collection]
  (json/json-str (zipmap (map #(str % "@en-US") (keys collection))
                         (map #(hash-map "items" (map document->hash %) "lastAccess" 0) (vals collection)))))

(defn update-computer!
  "Move files on computer to reflect the new state of the collections"
  [collection c-root]
  (doseq [[coll files] collection]
    (doseq [file files]
      (let [c-file (io/file c-root coll file)]
        (when (not (.exists c-file))
          (if-let [old-file (find-name c-root file)]
            (mv old-file c-file)
            (throw (java.io.IOException. (str "Can't find" file "to move into place!")))))))))

(defn computer->kindle!
  "Files from the computer to the kindle when the file is not yet on the kindle.
  'split' files are first pre-processed for optimal kindle viewing.
  
  collections is the merged collections map
  c-root is the root directory on the computer, which reflects the collections map
  k-root is the root directory on the kindle, which has not yet been updated
  "
  [collection c-root k-root]
  (doseq [[coll files] collection]
    (doseq [file files]
      (let [c-file (io/file c-root coll file)
            k-file (io/file k-root "documents" file)]
        (when (not (.exists k-file))
          (kindle-copy c-file k-file))))))

(defn write-collections!
  "Takes a collections datastructure and writes the JSON version, in kindle
  format, to the collections.json file in the kindle-root directory.
  "
  [collection kindle-root]
  (io/copy (collection-str collection) (collections-file kindle-root)))
