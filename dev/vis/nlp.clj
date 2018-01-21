(ns vis.nlp
  (:require [cem.nlp.core :refer [annotation-graph concept-graph]]
            [vis.helpers :refer [display-loom-graph]]))

(defn show-annotation-graph
  [text]
  (display-loom-graph "Annotation Graph"
                      (annotation-graph text)
                      {:edges {:arrows "to"}}))

(defn show-concept-graph
  [text & opts]
  (display-loom-graph "Concept Graph"
                      (apply concept-graph text opts)
                      {:edges {:arrows "to"}}))
