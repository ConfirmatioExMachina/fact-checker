(ns cem.nlp.cg.core
  "Creates concept graphs from annotation graphs and from infobox data."
  (:require [loom.graph :as graph]
            [cem.nlp.cg.simplify-annotations :refer [simplify-annotations]]
            [cem.nlp.cg.add-concepts :refer [add-concepts]]
            [cem.nlp.cg.simplify-concepts :refer [simplify-concepts]]
            [cem.nlp.cg.merge-keyword-compounds :refer [merge-keyword-compounds]]
            [cem.nlp.cg.add-global-concepts :refer [add-global-concepts]]
            [cem.nlp.cg.infobox :as infobox]))

(defn concept-graph
  [annotation-graph & opts]
  (let [{:keys [keyword-compounds no-global-concepts]} (set opts)]
    (if (and annotation-graph (seq (graph/nodes annotation-graph)))
      (-> annotation-graph
          simplify-annotations
          (add-concepts keyword-compounds)
          simplify-concepts
          ((if no-global-concepts identity add-global-concepts)))
      (graph/digraph))))

(defn infobox-concept-graph
  [subject infobox]
  (if (and subject infobox)
    (infobox/infobox-concept-graph subject infobox)
    (graph/digraph)))
