(ns vis.nlp
  (:require [cem.nlp.core :refer [annotation-graph]]
            [vis.helpers :refer [display-loom-graph]]))

(defn show-annotation-graph
  [text]
  (display-loom-graph "Annotation Graph"
                      (annotation-graph text)
                      {:edges {:arrows "to"}}))
