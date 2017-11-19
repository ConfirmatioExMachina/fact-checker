(ns vis.helpers
  (:require [proto-repl-charts.graph :refer [graph]]
            [loom.graph :as lg]
            [loom.attr :as la]
            [loom.io :as io]))

(def ^:dynamic *viewer* :proto-repl)
(def ^:dynamic *dot-path* (str (System/getProperty "user.home")
                               "/text2kg/graph.dot"))

(defn display-loom-graph
  [title g options]
  (case *viewer*
    :proto-repl-and-dot (do (binding [*viewer* :dot]
                              (display-loom-graph title g options))
                            (binding [*viewer* :proto-repl]
                              (display-loom-graph title g options)))
    :proto-repl (graph title
                       {:nodes (map #(into {:id %}
                                           (la/attrs g %))
                                    (lg/nodes g))
                        :edges (map #(into {:from (first %) :to (second %)}
                                           (la/attrs g %))
                                    (lg/edges g))}
                       options)
    :dot (io/dot g *dot-path* :graph-name title :node-label identity)
    :dot-str (io/dot-str g :graph-name title :node-label identity)
    :graph-viz (io/view g)
    :none nil))

(defn change-viewer!
  [viewer]
  {:pre [(#{:proto-repl-and-dot :proto-repl :dot :dot-str :graph-viz :none} viewer)]}
  (def ^:dynamic *viewer* viewer)
  *viewer*)

(defn change-dot-path!
  [path]
  (def ^:dynamic *dot-path* path)
  (change-viewer! :dot)
  *dot-path*)
