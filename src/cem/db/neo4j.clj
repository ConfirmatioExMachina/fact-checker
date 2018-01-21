(ns cem.db.neo4j
  (:require [clojure.java.io :as io]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [neo4j-clj.core :as db]
            [cem.helpers.id :as id]
            [cem.db.props :refer [props]]))

(def ^:private db (delay (let [{host :neo4j.host, user :neo4j.user, pw :neo4j.password} @props]
                           (db/connect host user pw))))

(db/defquery ^:private init-constraint
  "create constraint on (n:node) assert n.id is unique")

(db/defquery ^:private empty-db
  "match (n) detach delete n")

(db/defquery ^:private create-node
  "create (n:node {id: $data.id})
   set n = $data")

(db/defquery ^:private merge-node
  "merge (n:node {id: $data.id})
   on create set n = $data")

(defn- create-create-edge-query
  [type]
  (str "match (from:node {id: $from}), (to:node {id: $to})
        create (from)-[: " type "]->(to)"))

(defn- create-merge-edge-query
  [type]
  (str "match (from:node {id: $from}), (to:node {id: $to})
        merge (from)-[: " type "]->(to)"))

(db/defquery ^:private node-types
  "match (n {group: 'concept'})
   set n:concept
   with collect(1) as x
   match (n {group: 'relation'})
   set n:relation
   with collect(1) as x
   match (n {group: 'context'})
   set n:context")

(defn empty-db!
  []
  (db/with-transaction @db tx
    (empty-db tx)))

(defn- init-constraint!
  []
  (db/with-transaction @db tx
    (try
      (init-constraint tx)
      (catch Exception e))))

(defn fix-node-types!
  []
  (db/with-retry [@db tx]
    (node-types tx)
    (println "Fixed node types.")))

(defn- insert-graph!
  ([g] (insert-graph! g identity))
  ([g names]
   (db/with-retry [@db tx]
     (insert-graph! tx g names)))
  ([tx g names]
   ; Add nodes:
   (doseq [node (graph/nodes g)
           :let [id (names node)]]
     ((if (id/concept? id) merge-node create-node)
      tx {:data (-> (attr/attrs g node)
                    (select-keys [:id :group :label :named
                                  :negative :possibility
                                  :absolute-negative
                                  :root :global])
                    (assoc :id id))}))
   ; Add edges:
   (doseq [[from to :as edge] (graph/edges g)
           :let [{:keys [type]} (attr/attrs g edge)
                 from (names from)
                 to (names to)]]
     (db/execute tx
                 ((if (and (id/concept? from) (id/concept? to))
                    create-merge-edge-query
                    create-create-edge-query)
                  type)
                 {:from (names from), :to (names to)}))
   (println (str "Added graph: "
                 (count (graph/nodes g)) " nodes, "
                 (count (graph/edges g)) " edges."))))

(defn insert-graphs!
  [graphs]
  (when (seq graphs)
    (init-constraint!)
    (dorun (pmap (partial apply insert-graph!)
                 graphs))
    (fix-node-types!)))
