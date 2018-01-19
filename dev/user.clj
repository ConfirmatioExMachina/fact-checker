(ns user
  (:require [clojure.tools.namespace.repl :as ctnr]
            [clojure.stacktrace :refer :all]
            [proto-repl.saved-values]))

(defn start
  []
  (require '[vis.helpers :refer [display-loom-graph change-viewer! change-dot-path!]]
           '[vis.nlp :refer :all]
           '[cem.core])
  (ctnr/disable-reload! (find-ns 'cem.nlp.pipeline))
  (println "REPL started."))

(defn refresh
  []
  (ctnr/refresh :after 'user/start))

(defn refresh-all
  []
  (ctnr/refresh-all :after 'user/start))
