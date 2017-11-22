(ns cem.helpers.wikipedia
  (:import (fastily.jwiki.core Wiki)))

(def english-wikipedia (new Wiki "en.wikipedia.org"))

(defn fetch-summary
  [title]
  (.getTextExtract english-wikipedia title))
