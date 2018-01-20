(ns cem.corpus.offline
  (:require [clojure.java.io :refer [reader resource]]
            [cheshire.core :refer [parse-stream]]))

(def corpus (delay (parse-stream (-> "corpus/data/corpus.json" resource reader))))
