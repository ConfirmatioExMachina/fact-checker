(ns cem.train.core
  (:require [clojure.java.io :refer [reader resource]]
            [clojure.data.csv :refer [read-csv]]))

(defn read-data
  [path]
  (let [rows (read-csv (reader path) :separator \tab)]
    (->> rows
         (drop 1)
         (map (fn [[id fact truth]]
                {:id id
                 :fact fact
                 :truth (Double/parseDouble truth)})))))

(def data (delay (read-data (resource "train.tsv"))))

(println (take 10 @data))
