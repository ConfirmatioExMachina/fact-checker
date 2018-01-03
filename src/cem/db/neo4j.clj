(ns cem.db.neo4j
  (:require [clojure.java.io :as io]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [clojurewerkz.propertied.properties :as props]
            [neo4j-clj.core :as db]))

(def ^:private db (delay (let [props (-> (io/resource "db.properties")
                                         (props/load-from)
                                         (props/properties->map true))
                               {host :neo4j.host, user :neo4j.user, pw :neo4j.password} props]
                           (db/connect host user pw))))

(db/defquery ^:private init-constraint
  "create constraint on (n:node) assert n.id is unique")

(db/defquery ^:private empty-db
  "match (n) detach delete n")

(db/defquery ^:private create-node
  "merge (n:node {id: $data.id})
   set n = $data")

(defn- create-edge-query
  [type]
  (str "match (from {id: $from}), (to {id: $to})
        merge (from)-[e:" type "]->(to)"))

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
      (println "Added constraints.")
      (catch Exception e))))

(defn- fix-node-types!
  []
  (db/with-retry [@db tx]
    (node-types tx)
    (println "Fixed node types.")))

(defn- insert-graph!
  ([g] (insert-graph! g identity))
  ([g names]
   (db/with-retry [@db tx]
     (insert-graph! g names tx)))
  ([g names tx]
   (doseq [node (graph/nodes g)]
     (create-node tx {:data (-> (attr/attrs g node)
                                (dissoc :annotation)
                                (assoc :id (names node)))}))
   (doseq [[from to :as edge] (graph/edges g)]
     (let [data (attr/attrs g edge)]
       (db/execute tx (create-edge-query (:type data))
                   {:from (names from), :to (names to)})))
   (println (str "Added graph: "
                 (count (graph/nodes g)) " nodes, "
                 (count (graph/edges g)) " edges."))))

(defn insert-graphs!
  [graphs]
  (init-constraint!)
  (dorun (pmap (partial apply insert-graph!)
               graphs))
  (fix-node-types!))
