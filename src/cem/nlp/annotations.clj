(ns cem.nlp.annotations
  (:require [loom.graph :as lg]
            [loom.attr :as la]
            [cem.helpers.graph :as helpers]
            [cem.nlp.edge :as edge]
            [cem.nlp.pipeline :refer [pipeline]]
            [cem.nlp.id :as id]
            [cem.nlp.corenlp-symbols :refer [types]])
  (:import (edu.stanford.nlp.pipeline Annotation)
           (edu.stanford.nlp.ling CoreAnnotations$SentencesAnnotation
                                  CoreAnnotations$TokensAnnotation
                                  CoreAnnotations$DocDateAnnotation)
           (edu.stanford.nlp.semgraph SemanticGraphCoreAnnotations$EnhancedPlusPlusDependenciesAnnotation)
           (edu.stanford.nlp.coref CorefCoreAnnotations$CorefChainAnnotation)))

(defn- format-node
  [sentence token]
  [(id/token sentence token) {:label (str (.lemma token) "\n" (.ner token) ", " (.tag token))
                              :group (.tag token)
                              :lemma (.lemma token)
                              :sentence sentence
                              :index (.index token)
                              :position [(.beginPosition token) (.endPosition token)]
                              :type (types (.ner token))
                              :tag (.tag token)}])

(defn- format-edge
  [sentence edge]
  [[(id/token sentence (.getSource edge)) (id/token sentence (.getTarget edge))]
   (edge/edge (.getShortName (.getRelation edge)) true)])

(defn- sentence-annotations
  [graph index sentence]
  (let [graph-annotation (.get sentence SemanticGraphCoreAnnotations$EnhancedPlusPlusDependenciesAnnotation)
        nodes (seq (.vertexListSorted graph-annotation))
        edges (seq (.edgeListSorted graph-annotation))
        node-map (into {} (map (partial format-node index) nodes))
        edge-map (into {} (map (partial format-edge index) edges))]

    (-> graph
        (helpers/add-nodes-with-attrs node-map)
        (helpers/add-edges-with-attrs edge-map))))

(defn annotation-graph
  [text]
  (if text
    ; Compute CoreNLP annotations if a text was given:
    (let [doc (Annotation. text)]
      (.annotate @pipeline doc)
      (let [graph (reduce-kv sentence-annotations
                             (lg/digraph)
                             (vec (.get doc CoreAnnotations$SentencesAnnotation)))
            corefs (seq (.values (.get doc CorefCoreAnnotations$CorefChainAnnotation)))]
        (reduce (fn [g coref]
                  (let [representative-mention (.getRepresentativeMention coref)
                        representative-node (id/mention representative-mention)

                        mentions (seq (.getMentionsInTextualOrder coref))
                        nodes (map id/mention mentions)
                        coref-edges (into {} (map #(when (not= representative-node %)
                                                     (vector [% representative-node]
                                                             edge/coref))
                                                  nodes))]
                    (helpers/add-edges-with-attrs g coref-edges)))
                graph corefs)))
    ; Return an empty graph if no text was given:
    (lg/digraph)))
