(ns cem.helpers.alg
  (:import (clojure.lang PersistentQueue)))

(defn map-values [f m]
  "Maps over the values of hashmaps."
  (into {} (for [[k v] m] [k (f v)])))

(defn longest-prefix
  "Returns the longest common prefix of the sequences a and b."
  [a b]
  (loop [result []
         a a
         b b]
    (let [firstA (first a)]
      (if (and (some? firstA)
               (= firstA (first b)))
        (recur (conj result (first a))
               (rest a) (rest b))
        result))))

(defn traverse-paths
  "Simple generic BFS implementation."
  [root initial-global-data initial-path-data succ step & args]
  (let [{:keys [traverse-root]} (set args)]
    (loop [queue (conj (PersistentQueue/EMPTY)
                       [root [] initial-path-data])
           global-data initial-global-data]
      (if-let [[node path path-data] (peek queue)]
        ; Queue not empty:
        (let [new-path (conj path node)
              [new-global-data new-path-data] (if (or traverse-root (not= node root))
                                                (step global-data path-data node path)
                                                [global-data path-data])]
          (recur (-> queue pop
                     (into (map #(vector % new-path new-path-data))
                           (filter #(not-any? #{%} new-path) (succ node))))
                 new-global-data))
        ; Queue empty:
        global-data))))
