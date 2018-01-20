(ns cem.nlp.cg.core
  (:require [loom.graph :as graph]
            [cem.nlp.cg.simplify-annotations :refer [simplify-annotations]]
            [cem.nlp.cg.add-concepts :refer [add-concepts]]
            [cem.nlp.cg.simplify-concepts :refer [simplify-concepts]]))

(defn concept-graph
  [annotation-graph]
  (if (graph/nodes annotation-graph)
    (-> annotation-graph
        simplify-annotations
        add-concepts
        simplify-concepts)
    annotation-graph))
