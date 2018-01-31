(ns cem.db.preprocess
  "Renames graph nodes to prevent id collisions when inserting graphs."
  (:require [loom.graph :as graph]
            [cem.helpers.id :as id]))

(defn- rename-node
  [node]
  (if (id/concept? node)
    node
    (id/random)))

(defn unique-node-ids
  "Returns a mapping of all node ids in g to new unique node ids.
   This allows merging multiple concept graphs in the database."
  [g]
  (->> (graph/nodes g)
       (map (juxt identity rename-node))
       (into {})))

(defn uniqueify-graph
  [g]
  [g (unique-node-ids g)])
