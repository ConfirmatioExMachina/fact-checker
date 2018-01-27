(ns cem.corpus.core
  (:require [cem.db.core :as db]
            [cem.nlp.core :as nlp]
            [cem.corpus.offline :as offline]
            [cem.helpers.wikipedia :as wiki]))

(defn import-docs!
  [docs]
  (let [graphs (pmap (fn [[titles summary]]
                       (println (str "Computing concept graph for " titles "."))
                       [titles (nlp/concept-graph summary)])
                     docs)]
    (future (doall graphs)) ; Force realization of all graphs in case ES or Neo4j are a bottleneck.
    (db/insert-titled-graphs! graphs)))

(defn import-doc!
  [title text]
  (if-not (db/title-inserted? title)
    (import-docs! [[[title] text]])))

(defn import-articles!
  [titles]
  (let [[title-map summaries] (->> titles
                                   (distinct)
                                   (remove db/title-inserted?)
                                   wiki/fetch-summaries)
        reverse-title-map (->> title-map
                               (map (fn [[t1 t2]] {t2 #{t1}}))
                               (apply merge-with into))
        summaries (->> summaries
                       (map (fn [[t s]] (if (db/title-inserted? t) [t nil] [t s])))
                       (into {}))]
    (import-docs! (map (fn [[title summary]]
                         [(conj (reverse-title-map title) title) summary])
                       summaries))))

(defn import-article!
  [title]
  (import-articles! [title]))

(defn import-offline-corpus!
  []
  (import-docs! (->> @offline/corpus
                     (filter (comp (complement db/title-inserted?) first))
                     (take 50))))
