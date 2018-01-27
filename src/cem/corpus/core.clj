(ns cem.corpus.core
  (:require [cem.db.core :as db]
            [cem.nlp.core :as nlp]
            [cem.corpus.offline :as offline]
            [cem.helpers.wikipedia :as wiki]))

(defn import-docs!
  [docs]
  (let [graphs (pmap (fn [[raw-title {:keys [title summary]}]]
                       (println (str "Computing concept graph for " raw-title " (" title ")."))
                       [[raw-title title] (nlp/concept-graph summary)])
                     docs)]
    (future (doall graphs)) ; Force realization of all graphs in case ES or Neo4j are a bottleneck.
    (db/insert-titled-graphs! graphs)))

(defn import-doc!
  [title text]
  (if-not (db/title-inserted? title)
    (import-docs! [[title text]])))

(defn import-articles!
  [titles]
  (let [docs (->> titles
                  (distinct)
                  (remove db/title-inserted?)
                  wiki/fetch-summaries)]
    (import-docs! docs)))

(defn import-article!
  [title]
  (import-articles! [title]))

(defn import-offline-corpus!
  []
  (import-docs! (->> @offline/corpus
                     (filter (comp (complement db/title-inserted?) first))
                     (take 50))))
