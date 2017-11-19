(ns cem.nlp.edge)

(defn edge
  ([type]
   (edge type false))
  ([type use-dep-type]
   {:label type, (if use-dep-type :dep-type :type) type}))

(def coref (edge "coref"))
