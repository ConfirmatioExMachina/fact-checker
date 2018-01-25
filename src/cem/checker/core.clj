(ns cem.checker.core
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [loom.alg :as alg]
            [cem.helpers.graph :as hlp]
            [cem.nlp.core :as nlp]
            [cem.db.core :as db]
            [cem.corpus.core :as corpus]))

(def ^:private max-path-length 2)
(def ^:private considered-matches 2)
(def ^:private drop-off-coefficient 0.2)

(def ^:private match-query
  (str "match (n1:node {id: $n1}), (n2:node {id: $n2}),
              (n1)-[:inst*]->(m1:node), (m2:node)<-[:inst*]-(n2),
              p = shortestPath((m1)-[*0.." max-path-length "]-(m2))
        where (not exists(m1.global) or m1.global = false or n1.global = m1.global)
         	and (not exists(m2.global) or m2.global = false or n1.global = m2.global)
         	and none(x in nodes(p) where x.group = 'context' or (exists(x.global) and x <> m1 and x <> m2))
        with count(1) as paths
        return paths"))

(defn- binarify-result
  [result]
  (/ (inc result) 2))

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
                           kg-matches (for [[i1 [n1-start n1-score]] n1-starts
                                            [i2 [n2-start n2-score]] n2-starts]
                                        (* (if (pos? (-> (db/cypher-query match-query
                                                           {:n1 n1-start, :n2 n2-start})
                                                         first (:paths 0)))
                                             1 -1)
                                           n1-score n2-score
                                           (Math/pow drop-off-coefficient (+ i1 i2))))
                           abs-match-sum (apply + (map #(Math/abs %) kg-matches))]
                       (if (zero? abs-match-sum) 0 (/ (apply + kg-matches) abs-match-sum))))]
      (binarify-result (if (empty? kg-paths) 0 (apply min kg-paths))))))

(time (check-fact "Portsmouth is Charles Dickens' nascence place."))
; (time (check-fact "Ted Harbert is Stephen Dunham's better half."))
