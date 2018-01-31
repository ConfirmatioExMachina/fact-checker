(ns cem.nlp.cg.merge-keyword-compounds
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.id :as id]
            [cem.helpers.graph :as hlp]
            [cem.nlp.edge :as edge]
            [cem.nlp.cg.merge-tokens :refer [merge-tokens]]))

(def ^:private common-keyword (id/common-concept "keyword"))

(defn merge-keyword-compounds
  [g]
  (let [patients (hlp/successors (partial = edge/patient) g)
        keywords (hlp/successors (partial = edge/inst) g common-keyword)]
    (loop [g g, [keyword & keywords] keywords]
      (if keyword
        (let [[patient1 :as patients1] (patients keyword)
              new-g (when (= (count patients1) 1)
                      (let [[patient2 :as patients2] (patients patient1)]
                        (when (= (count patients2) 1)
                          (-> g
                              (graph/remove-edges [common-keyword keyword])
                              (merge-tokens [patient2 patient1 keyword])))))]
          (recur (or new-g g) keywords))
        g))))
