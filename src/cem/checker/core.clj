(ns cem.checker.core
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [loom.alg :as alg]
            [cem.helpers.graph :as hlp]
            [cem.nlp.core :as nlp]
            [cem.db.core :as db]
            [cem.corpus.core :as corpus]))

(def ^:private max-path-length 2)
(def ^:private considered-matches 5)
(def ^:private drop-off-coefficient 0.2)

(def ^:private match-query
  (str "match (n1:node {id: $n1}), (n2:node {id: $n2}),
              (n1)-[:inst*]->(m1:node)-[*0.." max-path-length "]-(m2:node)<-[:inst*]-(n2)
        with count(1) as paths
        return paths"))

(defn check-fact
  [fact]
  (let [cg (nlp/concept-graph fact :no-global-concepts)
        named-nodes (->> (graph/nodes cg)
                         (map (juxt identity (partial attr/attrs cg)))
                         (filter (comp :named second)))]
    (corpus/import-articles! (map (comp :label second) named-nodes))
    (let [start-ids (into {} (map (juxt first
                                        (comp (partial map-indexed vector)
                                              (partial take considered-matches)
                                              db/search-nodes
                                              :label second))
                                  named-nodes))
          cf-cg (graph/build-graph (graph/graph)
                                   (hlp/filter-nodes cg (comp (partial not= "context") :group
                                                              (partial attr/attrs cg))))
          node-pairs (set (for [i (keys start-ids), j (keys start-ids) :when (not= i j)] #{i j}))
          fact-paths (->> node-pairs
                          (map (juxt identity (partial apply alg/bf-path cf-cg)))
                          (filter (comp #(<= 1 % (inc max-path-length)) count second))
                          (into {}))
          kg-paths (for [[nodes fact-path] fact-paths]
                     (let [[n1 n2] (seq nodes)
                           n1-starts (start-ids n1)
                           n2-starts (start-ids n2)
                           kg-matches (for [[i1 n1-start] n1-starts, [i2 n2-start] n2-starts]
                                        (* (if (pos? (-> (db/cypher-query match-query
                                                           {:n1 n1-start, :n2 n2-start})
                                                         first (:paths 0)))
                                             1 -1)
                                           (Math/pow drop-off-coefficient (+ i1 i2))))
                           abs-match-sum (apply + (map #(Math/abs %) kg-matches))]
                       (println n1-starts n2-starts kg-matches)
                       (/ (apply + kg-matches) abs-match-sum)))]
      kg-paths)))

(time (check-fact "Ted Harbert is Stephen Dunham's better half."))
