(ns cem.nlp.cg.simplify-annotations
  (:require [clojure.string :as str]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.graph :as hlp]
            [cem.nlp.corenlp-symbols :refer [fill-tags]]
            [cem.nlp.cg.merge-tokens :refer [merge-tokens]]))

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

(defn merge-compounds
  [g]
  (if-let [compound (first (filter #(str/starts-with? (or (attr/attr g % :dep-type) "")
                                                      "compound")
                                   (graph/edges g)))]
    (recur (merge-tokens g compound))
    g))

(defn simplify-annotations
  [g]
  (-> g
      remove-fill-words
      remove-punct
      remove-aux
      merge-compounds))
