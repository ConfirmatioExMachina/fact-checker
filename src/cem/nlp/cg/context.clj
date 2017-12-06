(ns cem.nlp.cg.context
  (:refer-clojure :exclude [merge])
  (:require [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.id :as id]
            [cem.helpers.graph :as hlp]
            [cem.helpers.alg :as alg]))

(defn label
  [root negative possibility]
  (str (if negative
         (if root
           "NEG ROOT"
           "NEGATIVE")
         (if root
           "ROOT"
           "POSITIVE"))
       "\n"
       (if possibility "POSSIB" "ACTUAL")))

(defn set-parent
  [context parent]
  (let [root (nil? parent)
        {:keys [possibility negative]} context]
    (into context {:label (label root negative possibility)
                   :root root
                   :absolute-negative (= negative (not (:absolute-negative parent)))
                   :path (if root [] (conj (:path parent) (:id parent)))})))

(defn nest
  ([]
   (nest nil :possibility))
  ([parent]
   (nest parent :possibility))
  ([parent & properties]
   (let [[possibility negative] (map some? ((juxt :possibility :negative) (set properties)))]
     (as-> {:group "context"
            :possibility possibility
            :negative negative} $
           (set-parent $ parent)
           (into $ {:id (if (:root $) "root" (id/random))})))))

(defn entry
  [context]
  [(:id context) context])

(def context (comp entry nest))

(defn merge
  [head child]
  {:pre [(= (:id head) (last (:path child)))]}
  (let [{:keys [root negative absolute-negative possibility]} head
        {child-negative :negative child-possibility :possibility} child
        new-negative (not= negative child-negative)
        new-absolute-negative (not= absolute-negative child-negative)
        new-possibility (or possibility child-possibility)]
    (into head {:label (label root new-negative new-possibility)
                :negative new-negative
                :absolute-negative new-absolute-negative
                :possibility new-possibility})))

(defn context?
  [context]
  (= (:group context) "context"))

(defn nests?
  [parent-context child-context]
  (some (partial = (:id parent-context))
        (conj (:path child-context) (:id child-context))))

(defn context-of
  [g node]
  (if-let [[context _] (first (filter #(= "nest" (attr/attr g % :type))
                                      (graph/in-edges g node)))]
    (entry (attr/attrs g context))))

(def nested (hlp/successors (comp (partial = "nest") :type)))

(defn set-context-parent-recursive
  ([g context-id]
   (set-context-parent-recursive g context-id context-id false))
  ([g context-id parent-id]
   (set-context-parent-recursive g context-id parent-id true))
  ([g context-id parent-id traverse-root]
   (alg/traverse-paths context-id g (attr/attrs g parent-id)
                       (nested g)
                       (fn [g parent-context current-context-id path]
                         (let [current-context (attr/attrs g current-context-id)
                               new-current-context (if (context? current-context)
                                                     (set-parent current-context
                                                                         parent-context)
                                                     current-context)]
                           [(hlp/reset-attrs g
                                             current-context-id
                                             new-current-context)
                            new-current-context]))
                       (and traverse-root :traverse-root))))
