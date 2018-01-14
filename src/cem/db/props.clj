(ns cem.db.props
  (:require [clojure.java.io :as io]
            [clojurewerkz.propertied.properties :as props]))

(def props (delay (-> (io/resource "db.properties")
                      (props/load-from)
                      (props/properties->map true))))
