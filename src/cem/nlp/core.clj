(ns cem.nlp.core
  (:require [cem.nlp.annotations :as annotations]
            [cem.nlp.cg.core :as cg]))

(def annotation-graph annotations/annotation-graph)

(def concept-graph (comp cg/concept-graph
                         annotation-graph))
