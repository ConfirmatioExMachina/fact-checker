(ns cem.helpers.id
  (:require [clojure.string :as str]))

(defn concept
  [name]
  (str "concept_" name))

(defn common-concept
  [name]
  (concept (str/lower-case name)))

(defn random
  []
  (str (java.util.UUID/randomUUID)))

(defn concept?
  [id]
  (and (string? id)
       (str/starts-with? id "concept_")))
