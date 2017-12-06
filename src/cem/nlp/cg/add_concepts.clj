(ns cem.nlp.cg.add-concepts
  (:require [clojure.string :as str]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.helpers.graph :as hlp]
            [cem.helpers.alg :as alg]
            [cem.helpers.id :as id]
            [cem.nlp.edge :refer [edge] :as edge]
            [cem.nlp.corenlp-symbols :refer [clauses agents patients
                                             verbs keyword-tags types]]
            [cem.nlp.cg.context :refer [context context-of] :as context]))

(def annotation-node-map {"PRP" {:group "concept"}
                          "PRP$" {:group "concept"}
                          "NN" {:group "concept"}
                          "NNS" {:group "concept"}
                          "NNP" {:group "concept", :named true}
                          "NNPS" {:group "concept", :named true}
                          "JJ" {:group "concept"}
                          "JJR" {:group "concept"}
                          "JJS" {:group "concept"}
                          "RB" {:group "concept"}
                          "RBR" {:group "concept"}
                          "RBS" {:group "concept"}
                          "CD" {:group "concept"}
                          "IN" {:group "concept"}
                          "VB" {:group "relation"}
                          "VBD" {:group "relation"}
                          "VBG" {:group "relation"}
                          "VBP" {:group "relation"}
                          "VBN" {:group "relation"}
                          "VBZ" {:group "relation"}})

(def annotation-edge-map {(:dep-type edge/subj) edge/agent
                          "nsubj" edge/agent
                          "nsubj:xsubj" edge/agent
                          "nsubjpass" edge/patient
                          (:dep-type edge/obj) edge/patient
                          "dobj" edge/patient})

(defn- fix-copula
  [g [patient verb :as copula]]
  (let [agent-edges (filter #(contains? agents (attr/attr g % :dep-type))
                            (graph/out-edges g patient))
        clause-parent-edges (filter #(contains? clauses (attr/attr g % :dep-type))
                                    (graph/in-edges g patient))
        clause-child-edges (filter #(contains? clauses (attr/attr g % :dep-type))
                                   (graph/out-edges g patient))
        patient-attrs (if (contains? verbs (attr/attr g patient :tag))
                        (edge "ccomp" true)
                        edge/obj)
        new-edges (merge {[verb patient] patient-attrs}
                         (->> agent-edges
                              (map (fn [[_ agent :as agent-edge]]
                                     [[verb agent] (attr/attrs g agent-edge)]))
                              (into {}))
                         (->> clause-parent-edges
                              (map (fn [[clause-parent _ :as clause-edge]]
                                     [[clause-parent verb] (attr/attrs g clause-edge)]))
                              (into {}))
                         (->> clause-child-edges
                              (map (fn [[_ clause-child :as clause-edge]]
                                     [[verb clause-child] (attr/attrs g clause-edge)]))
                              (into {})))]
    (-> g
        (hlp/add-edges-with-attrs new-edges)
        (graph/remove-edges* (conj (concat agent-edges
                                           clause-parent-edges
                                           clause-child-edges) copula)))))

(defn fix-copulas
  [g]
  (reduce fix-copula g (filter #(= "cop" (attr/attr g % :dep-type))
                               (graph/edges g))))

(defn add-possibility-contexts
  [g]
  (let [[root-context-id _ :as root-context] (context)
        allowed-dep-type #(and (some? %) (not= % "conj"))
        roots (filter (fn [node]
                        (empty? (filter #(allowed-dep-type (attr/attr g % :dep-type))
                                        (graph/in-edges g node))))
                      (graph/nodes g))
        root-context-map (conj {} root-context)
        traversal-graph (-> g
                            (hlp/add-nodes-with-attrs root-context-map)
                            (hlp/add-edges-with-attrs (->> roots
                                                           (map #(vector [root-context-id %]
                                                                         {:dep-type true}))
                                                           (into {}))))
        succ (hlp/successors (comp allowed-dep-type :dep-type) traversal-graph)
        clause-type (fn [n1 n2] (clauses (attr/attr traversal-graph n1 n2 :dep-type)))
        step (fn [{:keys [contexts context-initializer context-nestings]} current-context-id current path]
               (let [parent (last path)
                     current-context (contexts current-context-id)
                     clause (clause-type parent current)
                     [new-context-id _ :as new-context] (if clause
                                                          (context current-context)
                                                          [current-context-id current-context])]
                 [{:contexts (conj contexts new-context)
                   :context-initializer (conj context-initializer (when clause
                                                                    [new-context-id [parent clause]]))
                   :context-nestings (update context-nestings current #(conj (or % #{}) new-context-id))}
                  new-context-id]))

        ; Do a BFS on the traversal graph:
        ; succ is a function, that returns the successors of a given node.
        ; step is a function, that transitions from the current search state
        ; to the next at every traversed node, given the current state and search position.
        {:keys [contexts
                context-initializer
                context-nestings]} (alg/traverse-paths root-context-id
                                                       {:contexts root-context-map
                                                        :context-initializer {}
                                                        :context-nestings {}}
                                                       root-context-id
                                                       succ step)
        ; remove well-known concepts (like types or time) from nestings:
        context-nestings (select-keys context-nestings
                                      (remove id/concept? (keys context-nestings)))
        ; select the smallest common parent context for the remaining concepts:
        context-nestings (alg/map-values #(->> %
                                               (map (comp (partial apply conj)
                                                          (juxt :path :id)
                                                          contexts))
                                               (reduce alg/longest-prefix)
                                               last)
                                         context-nestings)]
    (-> g
        ; add context nodes:
        (hlp/add-nodes-with-attrs contexts)
        ; add nest edges between contexts and their nested concepts:
        (hlp/add-edges-with-attrs (zipmap (map (comp vec reverse) context-nestings)
                                          (repeat edge/nest)))
        ; add nest edges between contexts and their nested contexts:
        (hlp/add-edges-with-attrs (zipmap (->> contexts
                                               (map (fn [[id {:keys [root path]}]]
                                                      (when-not root [(last path) id])))
                                               (filter some?))
                                          (repeat edge/nest)))
        ; add subj/obj edges between an initializing concept and the context it initializes:
        (hlp/add-edges-with-attrs (->> context-initializer
                                       (map (fn [[context [initializer clause]]]
                                              [[initializer context]
                                               (if (= clause "csubj") edge/subj edge/obj)]))
                                       (into {}))))))

(defn add-type-nodes
  [g]
  (as-> g g
        (hlp/add-nodes-with-attrs g (->> types
                                         (map #(vector (id/common-concept %)
                                                       {:label (str/lower-case %)
                                                        :named true}))
                                         (into {})))
        (hlp/add-edges-with-attrs g (->> (graph/nodes g)
                                         (map (juxt #(attr/attr g % :type) identity))
                                         (filter (comp (partial contains? types)
                                                       first))
                                         (map #(vector (update % 0 id/common-concept)
                                                       edge/inst))
                                         (into {})))
        (graph/remove-nodes* g (->> types
                                    (map id/common-concept)
                                    (filter #(zero? (graph/out-degree g %)))))))

(defn add-keyword-node
  [g]
  (let [kw (id/common-concept "keyword")
        keywords (filter #(keyword-tags (attr/attr g % :tag))
                         (graph/nodes g))]
    (if (empty? keywords)
      g
      (-> g
          (hlp/add-nodes-with-attrs {kw {:label "keyword"}})
          (hlp/add-edges-with-attrs (->> keywords
                                         (map #(vector [kw %] edge/inst))
                                         (into {})))))))

(defn add-mod-edges
  [g]
  (->> (graph/edges g)
       (filter #(some->> (attr/attr g % :dep-type) (re-matches #"(n|num|a|adv)mod.*|case|appos")))
       (reduce (fn [g [node attr :as e]]
                 (let [edge-type (if (some->> (attr/attr g e :dep-type)
                                              (re-matches #"(n|num|adv)mod.*|case"))
                                   edge/patient
                                   edge/inst)]
                   (hlp/add-edges-with-attrs g {[attr node] edge-type})))
               g)))

(defn- negative-nodes
  [g]
  (->> (graph/edges g)
       (filter #(= "neg" (attr/attr g % :dep-type)))
       (map first)
       (frequencies)
       (filter (comp odd? second))
       (map first)
       (into #{})))

(defn add-negative-contexts
  [g]
  (->> (negative-nodes g)
       (map (fn [node]
              (let [[old-context-id old-context] (context-of g node)
                    [new-context-id new-context] (context old-context :negative)]
                {:node node, :old-context-id old-context-id
                 :new-context-id new-context-id, :new-context new-context})))
       (reduce (fn [g {:keys [node old-context-id new-context-id new-context]}]
                 (-> g
                     (hlp/add-nodes-with-attrs {new-context-id new-context})
                     (hlp/add-edges-with-attrs {[old-context-id new-context-id] edge/nest
                                                [new-context-id node] edge/nest})
                     (graph/remove-edges [old-context-id node])))
               g)))

(defn verb-neighbors
  [g verb]
  (->> (graph/out-edges g verb)
       (map second)
       (reduce (fn [m node]
                 (condp contains? (attr/attr g [verb node] :dep-type)
                   agents (update m :agents conj node)
                   patients (update m :patients conj node)
                   m))
               {:agents [], :patients []})))

(defn- add-to-context
  [g context node]
  (let [[old-context-id _] (context-of g node)
        new-graph (-> g
                      (graph/remove-edges [old-context-id node])
                      (hlp/add-edges-with-attrs {[context node] edge/nest}))]
    (if (context/context? (attr/attrs g node))
      ; node is a context and all subcontexts have to be notified:
      (context/set-context-parent-recursive new-graph node context)
      ; else:
      new-graph)))

(defn- ensure-nesting
  [g parent node]
  (let [[parent-context-id parent-context] (context-of g parent)
        [node-context-id node-context] (context-of g node)]
    (if (context/nests? parent-context node-context)
      ; node already nested inside parent:
      g
      ; node not nested inside parent:
      (if (context/nests? node-context parent-context)
        ; parent nested inside context:
        (add-to-context g parent-context-id node)
        ; parent and node in different branches:
        (let [common-context-path (alg/longest-prefix (:path parent-context)
                                                      (:path node-context))
              nestable-context-id (nth (conj (:path node-context) node-context-id)
                                       (count common-context-path))]
          (add-to-context g parent-context-id nestable-context-id))))))

(defn- nesting-requirements
  [g]
  (mapcat (fn [node]
            (if (= "relation" (annotation-node-map (attr/attr g node :tag)))
              (let [{:keys [agents patients]} (verb-neighbors g node)
                    multiple-agents (> (count agents) 1)
                    multiple-patients (> (count patients) 1)
                    from-edge #(vector node %)
                    to-edge #(vector % node)
                    edge-direction #(if % from-edge to-edge)]
                (concat (map (edge-direction multiple-patients) patients)
                        (map (edge-direction multiple-agents) agents)))
              []))
          (graph/nodes g)))

(defn fix-nesting-requirements
  [g]
  (reduce (partial apply ensure-nesting) g
          (nesting-requirements g)))

(defn be->inst
  [g]
  (let [be-pairs (for [be (graph/nodes g)
                       :when (= (attr/attr g be :lemma) "be")]
                   [be (verb-neighbors g be)])]
    (-> g
        (graph/remove-nodes* (->> be-pairs
                                  (filter #(some not-empty (vals (second %))))
                                  (map first)))
        (hlp/add-edges-with-attrs (zipmap (->> be-pairs
                                               (map second)
                                               (mapcat #(for [agent (:agents %)
                                                              patient (:patients %)]
                                                          [patient agent])))
                                          (repeat edge/inst))))))

(defn cleanup-annotation-nodes
  [g]
  (loop [g g, [node & rest] (seq (graph/nodes g))]
    (if node
      (let [{:keys [tag group] :as attrs} (attr/attrs g node)
            g (if tag
                (if-let [mapped-attrs (annotation-node-map tag)]
                  (hlp/reset-attrs g node (assoc mapped-attrs
                                                 :label (:lemma attrs)
                                                 :annotation attrs))
                  (graph/remove-nodes g node))
                (if group g (attr/add-attr g node :group "concept")))]
        (recur g rest))
      g)))

(defn cleanup-annotation-edges
  [g]
  (loop [g g, [edge & rest] (graph/edges g)]
    (if edge
      (let [{:keys [dep-type] :as attrs} (attr/attrs g edge)
            g (if dep-type
                (if-let [mapped-attrs (annotation-edge-map dep-type)]
                  (hlp/reset-attrs g edge (assoc mapped-attrs :annotation attrs))
                  (graph/remove-edges g edge))
                g)]
        (recur g rest))
      g)))

(defn add-concepts
  [g]
  (-> g
      fix-copulas
      add-possibility-contexts
      add-type-nodes
      add-keyword-node
      add-mod-edges
      add-negative-contexts
      fix-nesting-requirements
      be->inst
      cleanup-annotation-nodes
      cleanup-annotation-edges))
