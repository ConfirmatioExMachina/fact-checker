(ns cem.nlp.cg.simplify-annotations
  (:require [clojure.string :as str]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.graph :as hlp]
            [cem.nlp.corenlp-symbols :refer [fill-tags]]))

(defn remove-fill-words
  [g]
  (hlp/filter-nodes g #(not (contains? fill-tags (attr/attr g % :tag)))))

(defn remove-punct
  [g]
  (hlp/filter-nodes g (fn [node]
                          (not-any? #(= "punct" (attr/attr g % :dep-type))
                                    (graph/in-edges g node)))))

(defn remove-aux
  [g]
  (hlp/filter-nodes g (fn [node]
                          (not-any? #(some->> (attr/attr g % :dep-type)
                                              (re-matches #"aux|auxpass"))
                                    (graph/in-edges g node)))))

(defn- merge-compound
  [g [c1 c2]]
  (let [attrs1 (attr/attrs g c1)
        attrs2 (attr/attrs g c2)
        [parts1 parts2] (map #(get % :compound-parts (sorted-map (:index %) (:lemma %)))
                             [attrs1 attrs2])
        new-parts (merge parts1 parts2)
        new-lemma (str/join " " (vals new-parts))]
    (-> g
        (hlp/merge-nodes c1 c2)
        (hlp/add-attrs c1 {:compound-parts new-parts
                           :lemma new-lemma
                           :label new-lemma})
        (graph/remove-edges [c1 c1]))))

(defn merge-compounds
  [g]
  (if-let [compound (first (filter #(str/starts-with? (or (attr/attr g % :dep-type) "")
                                                      "compound")
                                   (graph/edges g)))]
    (recur (merge-compound g compound))
    g))

(defn simplify-annotations
  [g]
  (-> g
      remove-fill-words
      remove-punct
      remove-aux
      merge-compounds))
