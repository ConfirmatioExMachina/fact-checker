(ns cem.import.core
  (:require [cem.db.core :as db]
            [cem.nlp.core :as nlp]
            [cem.corpus.core :as corpus]
            [cem.helpers.wikipedia :as wiki]))

(defn import-docs!
  [docs]
  (db/insert-graphs! (map (comp nlp/concept-graph second)
                          docs)))

(defn import-doc!
  [doc]
  (import-docs! [doc]))

(defn import-article!
  [title]
  (import-doc! [title (wiki/fetch-summary title)]))

(defn import-corpus!
  []
  (import-docs! @corpus/corpus))
