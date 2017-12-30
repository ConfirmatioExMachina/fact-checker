(ns cem.db.core
  (:require [cem.db.neo4j :as neo4j]
            [cem.db.preprocess :as pre]))

(defn insert-graph!
  [g]
  (neo4j/insert-graph! g (pre/unique-node-ids g)))

(defn insert-graphs!
  [graphs]
  (neo4j/insert-graphs! (map pre/uniqueify-graph graphs)))
