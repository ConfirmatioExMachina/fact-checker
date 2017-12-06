(ns cem.nlp.edge
  (:refer-clojure :exclude [agent]))

(defn edge
  ([type]
   (edge type false))
  ([type use-dep-type]
   {:label type, (if use-dep-type :dep-type :type) type}))

(def nest (edge "nest"))
(def inst (edge "inst"))
(def coref (edge "coref"))
(def agent (edge "agent"))
(def patient (edge "patient"))

(def subj (edge "subj" true))
(def obj (edge "obj" true))
