(ns cem.nlp.cg.add-global-concepts
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.graph :as hlp]
            [cem.nlp.cg.context :as context]
            [cem.nlp.edge :as edge]
            [cem.helpers.id :as id]))

(defn add-global-concepts
  [g]
  (let [concepts (->> (graph/nodes g)
                      (map (juxt identity (partial attr/attrs g)))
                      (remove (comp context/context? second)))]
    (reduce (fn [g [id {:keys [label], {:keys [type]} :annotation}]]
              (let [concept-id (id/concept label)
                    type-id (when type (id/common-concept type))]
                (if-not (id/concept? id)
                  (cond-> g
                          ; Add global concept node if not already present:
                          (not (graph/has-node? g concept-id))
                          (hlp/add-nodes-with-attrs {concept-id
                                                     {:label label
                                                      :named true
                                                      :global true
                                                      :group "concept"}})
                          ; Always add an inst-edge: global->nested
                          true
                          (hlp/add-edges-with-attrs {[concept-id id] edge/inst})
                          ; If the nested concept is inst of a type, add it to global:
                          type-id
                          (hlp/add-edges-with-attrs {[type-id concept-id] edge/inst}))
                  g)))
            g concepts)))
