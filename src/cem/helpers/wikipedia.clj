(ns cem.helpers.wikipedia
  (:import (fastily.jwiki.core Wiki)))

(def wikipedia (delay (Wiki. "en.wikipedia.org")))
(def blacklist (atom #{}))

(defn fetch-summary
  [title]
  (if-not (contains? @blacklist title)
    (let [summary (.getTextExtract @wikipedia title)]
      (if (some? summary)
        summary
        (do
          (swap! blacklist conj title)
          nil)))))
