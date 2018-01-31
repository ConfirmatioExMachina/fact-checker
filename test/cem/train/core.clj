(ns cem.train.core
  (:require [clojure.java.io :refer [reader resource]]
            [clojure.string :as str]
            [clojure.data.csv :refer [read-csv]]
            [cem.checker.core :as checker]))

(defn read-data
  [path]
  (let [rows (read-csv (reader path) :separator \tab)]
    (->> rows
         (drop 1)
         (map (fn [[id fact truth]]
                {:id id
                 :fact fact
                 :truth (when truth (Double/parseDouble truth))})))))

(defn check-data
  [data]
  (pmap (fn [{:keys [id fact truth]}]
          (println "Checking" id)
          (let [result (checker/check-fact fact)]
            (println "Checked" id "with truth" truth "and result" result)
            [id fact truth result]))
        data))

(defn results->ttl
  [results]
  (str/join (map (fn [[id fact truth result]]
                   (str "<http://swc2017.aksw.org/task2/dataset/" id "> "
                        "<http://swc2017.aksw.org/hasTruthValue> "
                        "\"" result "\"^^<http://www.w3.org/2001/XMLSchema#double> .\n"))
                 results)))

(defn write-results
  [results path]
  (spit path (results->ttl results)))

(defn binarify-result
  [threshold result]
  (if (<= threshold result) 1 0))

(defn ROC
  [threshold results]
  (let [binary-results (map (fn [[id fact truth result]]
                              [truth (binarify-result threshold result)])
                            results)
        p (remove (fn [[truth result]] (zero? truth)) binary-results)
        tp (remove (fn [[truth result]] (zero? result)) p)
        f (filter (fn [[truth result]] (zero? truth)) binary-results)
        fp (remove (fn [[truth result]] (zero? result)) f)
        tpr (/ (count tp) (count p))
        fpr (/ (count fp) (count f))]
    {:tpr tpr, :fpr fpr}))

(defn ROC-curve
  [results]
  (set (map #(ROC % results)
            (range -1 1.01 0.01))))

(def train-data (delay (read-data (resource "train.tsv"))))
(def test-data (delay (read-data (resource "test.tsv"))))
(def train-results (delay (check-data @train-data)))
(def test-results (delay (check-data @test-data)))
