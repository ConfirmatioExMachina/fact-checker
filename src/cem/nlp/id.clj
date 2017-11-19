(ns cem.nlp.id
  (:require [cem.helpers.id :as id]))

(defn token
  [sentence token]
  (str sentence "_" (.index token)))

(defn mention
  [mention]
  (str (dec (.sentNum mention)) "_" (.startIndex mention)))
