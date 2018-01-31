(ns cem.corpus.offline
  "Loads local wikipedia summary dump."
  (:require [clojure.java.io :refer [reader resource]]
            [cheshire.core :refer [parse-stream]]))

(def corpus (delay (parse-stream (-> "corpus/data/corpus.json" resource reader))))
