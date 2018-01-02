(ns cem.helpers.wikipedia
  (:import (fastily.jwiki.core Wiki)))

(def wikipedia (delay (Wiki. "en.wikipedia.org")))
(def blacklist (transient #{}))

(defn fetch-summary
  [title]
  (if-not (blacklist title)
    (let [summary (.getTextExtract @wikipedia title)]
      (if (some? summary)
        summary
        (do
          (conj! blacklist title)
          nil)))))
