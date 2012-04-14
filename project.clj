(defproject ashes/ashes "0.0.1-SNAPSHOT" 
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.3"]
                 [at.laborg/briss "0.0.13"]]
  :profiles {:dev {:resource-paths ["test-resources"]}}
  :main ashes.core
  :min-lein-version "2.0.0"
  :description "Sync PDF papers with kindle")
