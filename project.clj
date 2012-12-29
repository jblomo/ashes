(defproject ashes/ashes "0.1.1" 
  :description "Sync PDF papers with kindle"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.3"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [at.laborg/briss "0.9"]]
  :profiles {:dev {:resource-paths ["test-resources"]}}
  :main ashes.core
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"})
