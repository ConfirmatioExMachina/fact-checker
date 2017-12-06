(ns cem.nlp.cg.core
  (:require [cem.nlp.cg.simplify-annotations :refer [simplify-annotations]]
            [cem.nlp.cg.add-concepts :refer [add-concepts]]
            [cem.nlp.cg.simplify-concepts :refer [simplify-concepts]]))

(defn concept-graph
  [annotation-graph]
  (-> annotation-graph
      simplify-annotations
      add-concepts
      simplify-concepts))
