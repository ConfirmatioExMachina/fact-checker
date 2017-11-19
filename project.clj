(defproject cem "0.1.0-SNAPSHOT"
  :description "A NLP-based fact checker."
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [aysylu/loom "1.0.0"]
                 [cheshire "5.8.0"]

                 [edu.stanford.nlp/stanford-corenlp "3.8.0"]
                 [edu.stanford.nlp/stanford-corenlp "3.8.0" :classifier "models"]
                 [org.slf4j/slf4j-log4j12 "1.7.25"]]

  :main cem.core

  :jvm-opts ["-Xms1g" "-Xmx4g"]

  :profiles {:dev {:source-paths ["dev" "src" "test"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [clj-stacktrace "0.2.8"]
                                  [proto-repl "0.3.1"]
                                  [proto-repl-charts "0.3.1"]
                                  [proto-repl-sayid "0.1.3"]
                                  [com.cemerick/pomegranate "0.3.1"]]
                   :repl-options {:init-ns user
                                  :init (start)
                                  :caught clj-stacktrace.repl/pst+}
                   :eastwood {:exclude-linters [:constant-test :def-in-def]}}
             :uberjar {:aot :all}})
