(ns cem.corpus.core
  "Coordinates concept graph creation from concept labels.
   Performs batch wikipedia lookup and concept graph creation from summaries and infoboxes
   if they are missing from the local Neo4j knowledge graph.
   The computed concept graphs are then stored in Neo4j and Elasticsearch."
  (:require [clojure.core.async :refer [chan promise-chan go-loop thread
                                        <! >! <!! >!! poll! close!]]
            [cem.db.core :as db]
            [cem.nlp.core :as nlp]
            [cem.corpus.offline :as offline]
            [cem.helpers.wikipedia :as wiki]))

(defn- import-docs!
  [docs]
  (let [graphs (pmap (fn [[titles main-title summary infobox]]
                       (println (str "Computing concept graph for " titles "."))
                       {:titles titles
                        :graphs [(nlp/concept-graph summary)
                                 (nlp/infobox-concept-graph main-title infobox)]})
                     docs)]
    (future (doall graphs)) ; Force realization of all graphs in case ES or Neo4j are a bottleneck.
    (db/insert-titled-graphs! graphs)))

(defn- import-articles-now!
  [titles]
  (let [{:keys [title-map summaries infoboxes]} (->> titles
                                                     (distinct)
                                                     (remove db/title-inserted?)
                                                     wiki/fetch-data)
        reverse-title-map (->> title-map
                               (map (fn [[t1 t2]] {t2 #{t1}}))
                               (apply merge-with into))
        summaries (->> summaries
                       (map (fn [[t s]] (if (db/title-inserted? t) [t nil] [t s])))
                       (into {}))]
    (import-docs! (map (fn [[title queried-titles]]
                         (if (db/title-inserted? title)
                           [queried-titles nil nil nil]
                           [(conj queried-titles title)
                            title
                            (summaries title)
                            (infoboxes title)]))
                       reverse-title-map))))

(def ^:private article-cn (delay (let [titles-cn (chan)]
                                   (go-loop [[out titles] (<! titles-cn)]
                                     (let [ins (take-while (complement nil?)
                                                           (repeatedly #(poll! titles-cn)))
                                           outs (conj (map first ins) out)
                                           titles (concat titles (mapcat second ins))]
                                       (if (<! (thread (import-articles-now! titles) true))
                                         (doseq [out outs] (>! out true))
                                         (doseq [out outs] (close! out)))
                                       (recur (<! titles-cn))))
                                   titles-cn)))

(defn import-articles!
  [titles]
  (let [done (promise-chan)]
    (>!! @article-cn [done titles])
    (<!! done)))

(defn import-article!
  [title]
  (import-articles! [title]))

(defn import-offline-corpus!
  []
  (import-docs! (->> @offline/corpus
                     (filter (comp (complement db/title-inserted?) first))
                     (take 50))))
