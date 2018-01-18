(ns cem.import.core
  (:require [cem.db.core :as db]
            [cem.nlp.core :as nlp]
            [cem.corpus.core :as corpus]
            [cem.helpers.wikipedia :as wiki]))

(defn import-docs!
  [docs]
  (let [graphs (pmap (fn [[title text]]
                       (println (str "Computing concept graph for " title "."))
                       [title (nlp/concept-graph text)])
                     docs)]
    (future (doall graphs)) ; Force realization of all graphs in case ES or Neo4j are a bottleneck.
    (db/insert-titled-graphs! graphs)))

(defn import-doc!
  [title text]
  (if-not (db/title-inserted? title)
    (import-docs! [[title text]])))

(defn import-article!
  [title]
  (if-not (db/title-inserted? title)
    (if-let [summary (wiki/fetch-summary title)]
      (import-docs! [[title summary]]))))

(defn import-corpus!
  []
  (import-docs! (->> @corpus/corpus
                     (filter (comp (complement db/title-inserted?) first))
                     (take 50))))
