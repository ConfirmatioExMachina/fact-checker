(ns cem.import.core
  (:require [cem.db.core :as db]
            [cem.nlp.core :as nlp]
            [cem.corpus.core :as corpus]
            [cem.helpers.wikipedia :as wiki]))

(defn import-docs!
  [docs]
  (let [graphs (pmap (fn [[title text]]
                       (println (str "Computing concept graph for " title "."))
                       (nlp/concept-graph text))
                     docs)]
    (future (doall graphs))
    (db/insert-graphs! graphs)))

(defn import-doc!
  [doc]
  (import-docs! [doc]))

(defn import-article!
  [title]
  (import-doc! [title (wiki/fetch-summary title)]))

(defn import-corpus!
  []
  (import-docs! (take 50 @corpus/corpus)))
