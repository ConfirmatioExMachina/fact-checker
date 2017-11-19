(ns cem.helpers.graph
  (:require [loom.graph :as graph]
            [loom.attr :as attr]))

(defn add-attrs
  [g node-or-edge attrs]
  (reduce-kv #(attr/add-attr %1 node-or-edge %2 %3) g attrs))

(defn remove-attrs
  [g node-or-edge]
  (reduce #(attr/remove-attr %1 node-or-edge %2) g (keys (attr/attrs g node-or-edge))))

(defn reset-attrs
  [g node-or-edge attrs]
  (-> g
      (remove-attrs node-or-edge)
      (add-attrs node-or-edge attrs)))

(def add-attrs-from-map (partial reduce-kv add-attrs))

(def reset-attrs-from-map (partial reduce-kv reset-attrs))

(defn- add-nodes-or-edges-with-attrs
  [adder g attr-map & flags]
  (let [attr-adder (if (some #(= :no-reset %) flags)
                    add-attrs-from-map
                    reset-attrs-from-map)]
    (-> g
        (adder (keys attr-map))
        (attr-adder attr-map))))

(def add-nodes-with-attrs (partial add-nodes-or-edges-with-attrs graph/add-nodes*))

(def add-edges-with-attrs (partial add-nodes-or-edges-with-attrs graph/add-edges*))

(defn copy-edges
  "Copies all edges and their attributes of the given source nodes to the target node."
  [g target & sources]
  (reduce (fn [g node]
            (let [in-edges (into {} (map (fn [[from to :as edge]]
                                           [[from target]
                                            (attr/attrs g edge)])
                                         (graph/in-edges g node)))
                  out-edges (into {} (map (fn [[from to :as edge]]
                                            [[target to]
                                             (attr/attrs g edge)])
                                          (graph/out-edges g node)))]
              (-> g (add-edges-with-attrs in-edges)
                  (add-edges-with-attrs out-edges))))
          g sources))

(defn merge-nodes
  "Copies all edges and their attributes of the given nodes to head and removes the nodes afterwards."
  [g head & nodes]
  (let [nodes-without-head (remove (partial = head) nodes)]
    (graph/remove-nodes* (apply copy-edges g head nodes-without-head)
                         nodes-without-head)))

(defn filter-nodes
  [g f]
  (graph/remove-nodes* g (remove f (graph/nodes g))))

(defn filter-edges
  [g f]
  (graph/remove-edges* g (remove f (graph/edges g))))

(defn remove-edges-of-nodes
  [g & nodes]
  (graph/remove-edges* g (reduce (fn [edges node]
                                   (-> edges
                                       (into (graph/in-edges g node))
                                       (into (graph/out-edges g node))))
                                 #{} nodes)))

(defn- neighbors
  ([direction selector edge-filter]
   (partial neighbors direction selector edge-filter))
  ([direction selector edge-filter g]
   (partial neighbors direction selector edge-filter g))
  ([direction selector edge-filter g node]
   (->> (direction g node)
        (filter #(edge-filter (attr/attrs g %)))
        (map selector))))

(def predecessors (partial neighbors graph/in-edges first))
(def successors (partial neighbors graph/out-edges second))
