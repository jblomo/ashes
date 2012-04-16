(defproject ashes/ashes "0.1.0-SNAPSHOT" 
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.3"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [at.laborg/briss "0.0.13"]]
  :profiles {:dev {:resource-paths ["test-resources"]}}
  :main ashes.core
  :min-lein-version "2.0.0"
  :description "Sync PDF papers with kindle")
