(ns ashes.core
  (:use [ashes.file-management]
        [clojure.tools.cli :only (cli)]
        [clojure.tools.logging :only (info error)])
  (:require [clojure.java.io :as io])
  (:import [java.io File]))

(defn -main [& argv]
  (let [[options args banner]
           (cli argv
                ["-h" "--help" "Show help" :default false :flag true]
                ["-k" "--kindle-root" "Path to mount point of kindle device" :parse-fn #(File. %)]
                ["-c" "--computer-root" "Path to documents stored on computer" :parse-fn #(File. %)])]
    (when (or (:help options)
              (not (and (:kindle-root options) (:computer-root options))))
      (println banner)
      (System/exit 0))

    (info "Syncing" (:computer-root options) "=>"
          (:kindle-root options))

    (let [cf (computer-files (:computer-root options))
          kf (kindle-files   (:kindle-root options))
          merged (merge-collections cf kf)
          merged-collection-str (collection-str merged)]
      (info "Updated collections:" merged)

      (update-computer! merged (:computer-root options))
      (info "Computer updated")

      (computer->kindle! merged
                         (:computer-root options)
                         (:kindle-root options))
      (info "New files copied")

      (write-collections! merged (:kindle-root options))
      (info "Kindle updated"))))
