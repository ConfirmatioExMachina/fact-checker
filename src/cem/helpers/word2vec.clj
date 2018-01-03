(ns cem.helpers.word2vec
  (:require [clojure.java.io :as io])
  (:import (org.deeplearning4j.models.embeddings.loader WordVectorSerializer)
           (org.apache.commons.io FileUtils)))

(def model-url "https://s3.amazonaws.com/dl4j-distribution/GoogleNews-vectors-negative300.bin.gz")
(def model-location "./dev-resources/word2vec/model.bin.gz")

(defn load-model
  [url location]
  (when-not (.exists (io/as-file location))
    (println "Downloading Word2Vec Model")
    (FileUtils/copyURLToFile (io/as-url url) (io/as-file location)))
  (println "Found Word2Vec Model")
  (io/as-file location))

(def vectors (WordVectorSerializer/readWord2VecModel (load-model model-url model-location)))

(defn similarity
  [w1 w2]
  (if (and (.hasWord vectors w1) (.hasWord vectors w2))
    (.similarity vectors w1 w2)))

(defn n-nearast-words
  [w n]
  (if (.hasWord vectors w)
    (.wordsNearest vectors w n)))

(defn find-n-best-analogies
  [w-analogy-from w-analogy-to w-goal-from n]
  (if (and (.hasWord vectors w-analogy-from)
           (.hasWord vectors w-analogy-to)
           (.hasWord vectors w-goal-from))
    (.wordsNearest vectors
                   (java.util.ArrayList. [w-analogy-from w-goal-from])
                   (java.util.ArrayList. [w-analogy-to]))))
