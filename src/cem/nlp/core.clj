(ns cem.nlp.core
  "Handles creation of annotation graphs from text
   and concept graphs from text and Wikipedia infoboxes."
  (:require [cem.nlp.text-cleanup :as cleanup]
            [cem.nlp.annotations :as annotations]
            [cem.nlp.cg.core :as cg]))

(def annotation-graph annotations/annotation-graph)

(defn concept-graph
  [text & opts]
  (apply cg/concept-graph
         (-> text cleanup/cleanup annotation-graph)
         opts))

(def infobox-concept-graph cg/infobox-concept-graph)
