(ns cem.db.core
  (:require [cem.db.neo4j :as neo4j]
            [cem.db.elasticsearch :as es]
            [cem.db.preprocess :as pre]))

(defn title-inserted?
  [title]
  (es/contains-title? title))

(defn insert-titled-graphs!
  [titled-graphs]
  (let [titles (map first titled-graphs)
        graphs (map (comp pre/uniqueify-graph second) titled-graphs)
        titles-inserted (future (es/insert-titles! titles))
        graph-nodes-inserted (future (run! (partial apply es/insert-graph-nodes!)
                                           graphs))
        graphs-inserted (future (neo4j/insert-graphs! graphs))]
    @titles-inserted
    @graph-nodes-inserted
    @graphs-inserted))

(defn search-nodes
  [label]
  (es/search-nodes label))

(def cypher-query neo4j/query)

(defn empty-db!
  []
  (let [es (future (es/empty-db!))
        neo4j (future (neo4j/empty-db!))]
    @es @neo4j))
