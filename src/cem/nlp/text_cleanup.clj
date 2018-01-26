(ns cem.nlp.text-cleanup
  "Performs simple rule based natural language text transformations.
   The purpose of which are to compensate for common errors made by CoreNLP."
  (:require [clojure.string :as str]))

(defn cleanup
  [text]
  (str/replace text #"\s*,\s*Inc\." " Inc."))
