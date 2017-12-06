(ns cem.nlp.cg.simplify-concepts
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.graph :as hlp]
            [cem.helpers.alg :as alg]
            [cem.nlp.edge :refer [edge] :as edge]
            [cem.nlp.cg.context :refer [context-of] :as context]))

(defn merge-coref-edge
  [g [n1 n2]]
  (let [[context-id1 context1] (context-of g n1)
        [context-id2 context2] (context-of g n2)
        common-context-id (when (and context1 context2)
                            (last (alg/longest-prefix (conj (:path context1) context-id1)
                                                      (conj (:path context2) context-id2))))]
    (cond-> g
            true (hlp/merge-nodes n2 n1)
            true (graph/remove-edges [n2 n2] [context-id1 n2] [context-id2 n2])
            common-context-id (hlp/add-edges-with-attrs {[common-context-id n2] edge/nest}))))

(defn merge-coref-edges
  [g]
  (if-let [coref-edge (first (filter #(= "coref" (attr/attr g % :type))
                                     (graph/edges g)))]
    (recur (merge-coref-edge g coref-edge))
    g))

(defn merge-context-chains
  [g]
  (let [contexts (->> (graph/nodes g)
                      (filter (comp context/context?
                                    (partial attr/attrs g))))
        context-chains (->> contexts
                            (map (juxt identity (context/nested g)))
                            (filter (comp (partial >= 1)
                                          count second)))]
    (loop [g g
           [[context-id [child-id]] & remaining-chains] context-chains
           context-map (->> contexts
                            (map (comp (juxt :id :id)
                                       (partial attr/attrs g)))
                            (into {}))]
      (if-let [context-id (context-map context-id)]
        ; There is a context chain:
        (if child-id
          ; The chain has two nodes:
          (let [child-id (context-map child-id)
                context (attr/attrs g context-id)
                child (when child-id (attr/attrs g child-id))]
            (if child-id
              ; The second chain node is a context:
              (recur (-> g
                         (hlp/reset-attrs context-id (context/merge context child))
                         (hlp/merge-nodes context-id child-id)
                         (graph/remove-edges [context-id context-id])
                         (context/set-context-parent-recursive context-id))
                     remaining-chains
                     (assoc context-map child-id context-id))
              ; The second chain node is no context:
              (recur g remaining-chains context-map)))
          ; The chain only has one node:
          (recur (graph/remove-nodes g context-id)
                 remaining-chains
                 (dissoc context-map context-id)))
        ; No context chains left:
        g))))

(defn simplify-concepts
  [g]
  (-> g
      merge-coref-edges
      merge-context-chains))
