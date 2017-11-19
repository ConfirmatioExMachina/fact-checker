(ns cem.helpers.id)

(defn concept
  [name]
  (str "concept_" name))

(defn common-concept
  [name]
  (concept (clojure.string/lower-case name)))

(defn random
  []
  (str (java.util.UUID/randomUUID)))

(defn concept?
  [id]
  (clojure.string/starts-with? id "concept_"))
