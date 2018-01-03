(ns cem.nlp.pipeline
  (:import (java.util Properties)
           (edu.stanford.nlp.pipeline StanfordCoreNLP)))

(defn create-pipeline
  "Creates a new CoreNLP annotator pipeline."
  []
  (let [properties (doto (Properties.)
                     (.setProperty "annotators"
                                   (clojure.string/join "," ["tokenize" "ssplit" "pos"
                                                             "lemma" "parse" "depparse"
                                                             "ner" "mention" "coref"]))
                     (.setProperty "threads"
                                   (str (.availableProcessors (Runtime/getRuntime)))))]
    (StanfordCoreNLP. properties)))

(def pipeline (delay (create-pipeline)))
