(ns vis.nlp
  (:require [cem.nlp.core :refer [annotation-graph concept-graph
                                  infobox-concept-graph]]
            [cem.helpers.wikipedia :as wiki]
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

(defn show-infobox-concept-graph
  [subject]
  (let [infobox (wiki/fetch-infobox subject)]
    (display-loom-graph "Concept Graph"
                        (infobox-concept-graph subject infobox)
                        {:edges {:arrows "to"}})))
