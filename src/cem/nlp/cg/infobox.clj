(ns cem.nlp.cg.infobox
  "Creates concept graphs from infobox data."
  (:require [loom.graph :as graph]
            [cem.helpers.graph :as hlp]
            [cem.helpers.id :as id]
            [cem.nlp.edge :as edge]))

(defn- concept-attrs
  [label named]
  {:label label
   :named named
   :global named
   :infobox true
   :group "concept"})

(defn infobox-concept-graph
  [subject infobox]
  (let [subj-id (id/concept subject)
        g (hlp/add-nodes-with-attrs (graph/digraph)
                                    {subj-id
                                     (concept-attrs subject true)})]
    (loop [[prop & props] (seq infobox), g g]
      (if-let [[rel objs] prop]
        (let [rel-id (id/random)
              nodes (->> objs
                         (map (juxt id/concept #(concept-attrs % true)))
                         (into {rel-id (concept-attrs rel false)}))
              edges (->> objs
                         (map #(vector [(id/concept %) rel-id] edge/inst))
                         (into {[subj-id rel-id] edge/patient}))]
          (recur props
                 (-> g
                     (hlp/add-nodes-with-attrs nodes)
                     (hlp/add-edges-with-attrs edges))))
        g))))
