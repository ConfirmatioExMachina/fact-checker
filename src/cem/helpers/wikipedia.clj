(ns cem.helpers.wikipedia
  (:import (fastily.jwiki.core Wiki)))

(def wikipedia (delay (Wiki. "en.wikipedia.org")))

(defn fetch-summary
  [title]
  (.getTextExtract @wikipedia title))
