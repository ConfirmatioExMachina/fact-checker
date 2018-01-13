(ns cem.db.core
  (:require [cem.db.neo4j :as neo4j]
            [cem.db.preprocess :as pre]))

(defn insert-graphs!
  [graphs]
  (neo4j/insert-graphs! (map pre/uniqueify-graph graphs)))

(defn insert-graph!
  [g]
  (insert-graphs! [g]))
