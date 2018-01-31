(ns cem.nlp.cg.merge-tokens
  (:require [clojure.string :as str]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.graph :as hlp]))

(defn merge-tokens
  [g [c1 c2 & cn]]
  (if (empty? cn)
    (let [attrs1 (attr/attrs g c1)
          attrs2 (attr/attrs g c2)
          [parts1 parts2] (map #(get % :compound-parts (sorted-map (:index %) (:lemma %)))
                               [attrs1 attrs2])
          new-parts (merge parts1 parts2)
          new-lemma (str/join " " (vals new-parts))
          [[s1 e1] [s2 e2]] (map :position [attrs1 attrs2])
          new-position [(min s1 s2) (max e1 e2)]]
      (-> g
          (hlp/merge-nodes c1 c2)
          (hlp/add-attrs c1 {:compound-parts new-parts
                             :lemma new-lemma
                             :label new-lemma
                             :position new-position})
          (graph/remove-edges [c1 c1])))
    (merge-tokens (merge-tokens g [c1 c2]) (cons c1 cn))))
