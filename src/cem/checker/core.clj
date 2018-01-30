(ns cem.checker.core
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [loom.alg :as alg]
            [cem.helpers.graph :as hlp]
            [cem.nlp.core :as nlp]
            [cem.db.core :as db]
            [cem.corpus.core :as corpus]))

(def ^:private cfg {:max-path-length 2
                    :considered-matches 2
                    :direct-match-factor 10
                    :context-match-factor 1
                    :dropoff-factor 0.2})

(def ^:private match-query
  (str "match (n1:node {id: $n1}), (n2:node {id: $n2}),
              (n1)-[:inst*]->(m1:node), (m2:node)<-[:inst*]-(n2),
              p = (m1)-[*0.." (cfg :max-path-length) "]-(m2)
        where (not exists(m1.global) or m1.global = false or n1.global = m1.global)
         	and (not exists(m2.global) or m2.global = false or n1.global = m2.global)
          and none(x in nodes(p) where exists(x.global) and x <> m1 and x <> m2)
        with case
          when none(x in nodes(p) where x.group = 'context')  then " (cfg :direct-match-factor) "
          else " (cfg :context-match-factor) "
        end as score
        with max(score) as score
        return case
          when score is null then -1
          else score
        end as score"))

(defn- node-attrs->article-title
  [text {:keys [label named global] {[start end] :position} :annotation}]
  (let [[_ followup-parens] (when end (re-find #"^\s*(\([^)]+\))"
                                               (subs text end)))]
    (cond
      followup-parens (str label " " followup-parens)
      (and named (not global)) label)))

(defn check-fact
  [fact]
  (let [cg (nlp/concept-graph fact :no-global-concepts)
        nodes (->> (graph/nodes cg)
                   (map (juxt identity (partial attr/attrs cg)))
                   (into {}))
        nodes->articles (->> nodes
                             (map (juxt first (comp (partial node-attrs->article-title fact)
                                                    second)))
                             (remove (comp nil? second))
                             (into {}))]
    (corpus/import-articles! (vals nodes->articles))
    (let [start-ids (into {} (map (juxt first
                                        (comp (partial map-indexed vector)
                                              (partial take (cfg :considered-matches))
                                              db/search-nodes
                                              :label nodes first))
                                  nodes->articles))
          cf-cg (graph/build-graph (graph/graph)
                                   (hlp/filter-nodes cg (comp (partial not= "context") :group
                                                              (partial attr/attrs cg))))
          node-pairs (set (for [i (keys start-ids), j (keys start-ids) :when (not= i j)] #{i j}))
          fact-paths (->> node-pairs
                          (map (juxt identity (partial apply alg/bf-path cf-cg)))
                          (filter (comp #(<= 1 % (inc (cfg :max-path-length))) count second))
                          (into {}))
          kg-paths (for [[nodes fact-path] fact-paths]
                     (let [[n1 n2] (seq nodes)
                           n1-starts (start-ids n1)
                           n2-starts (start-ids n2)
                           kg-matches (for [[i1 [n1-start n1-score]] n1-starts
                                            [i2 [n2-start n2-score]] n2-starts]
                                        (try
                                          (* (-> (db/cypher-query match-query
                                                  {:n1 n1-start, :n2 n2-start})
                                                first :score)
                                             n1-score n2-score
                                             (Math/pow (cfg :dropoff-factor) (+ i1 i2)))
                                          (catch Exception e 0)))
                           abs-match-sum (apply + (map #(Math/abs %) kg-matches))]
                       (if (zero? abs-match-sum) 0 (/ (apply + kg-matches) abs-match-sum))))
          kg-paths (remove zero? kg-paths)]
      (if (empty? kg-paths) 0 (apply min kg-paths)))))
