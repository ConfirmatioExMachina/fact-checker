(ns cem.db.props
  "Loads Neo4j and ES connection information."
  (:require [clojure.java.io :as io]
            [clojurewerkz.propertied.properties :as props]))

(def props (delay (-> (io/resource "db.properties")
                      (props/load-from)
                      (props/properties->map true))))
