(ns cem.nlp.cg.core
  (:require [loom.graph :as graph]
            [cem.nlp.cg.simplify-annotations :refer [simplify-annotations]]
            [cem.nlp.cg.add-concepts :refer [add-concepts]]
            [cem.nlp.cg.simplify-concepts :refer [simplify-concepts]]
            [cem.nlp.cg.add-global-concepts :refer [add-global-concepts]]))

(defn concept-graph
  [annotation-graph & opts]
  (let [{:keys [no-global-concepts]} (set opts)]
    (if (graph/nodes annotation-graph)
      (-> annotation-graph
          simplify-annotations
          add-concepts
          simplify-concepts
          ((if no-global-concepts identity add-global-concepts)))
      annotation-graph)))
