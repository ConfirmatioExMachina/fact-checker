(ns cem.db.elasticsearch
  (:require [qbits.spandex :as s]
            [qbits.spandex.url :refer [encode-fragment]]
            [loom.graph :as graph]
            [loom.attr :as attr]
            [cem.db.props :refer [props]]
            [cem.helpers.id :as id]))

(def mapping-types {:titles {:title {:type "text"}}
                    :nodes {:label {:type "text"}
                            :group {:type "keyword"}
                            :named {:type "boolean"}}})

(defn- create-index!
  [db name props]
  (try
    (s/request db {:method :put
                   :url (encode-fragment name)
                   :body {:mappings {:doc {:properties props}}}})
    (println (str "Created index: " name))
    (catch Exception e)))

(defn- delete-index!
  [db name]
  (try
    (s/request db {:method :delete, :url (encode-fragment name)})
    (catch Exception e)))

(defn- init-indices!
  [db]
  (doto db
    (create-index! "titles" (:titles mapping-types))
    (create-index! "nodes" (:nodes mapping-types))))

(defn- create-doc!
  ([db index doc]
   (s/request db {:method :post
                  :url (str (encode-fragment index) "/doc")
                  :body doc}))
  ([db index id doc]
   (s/request db {:method :put
                  :url (str (encode-fragment index) "/doc/"
                            (encode-fragment id))
                  :body doc})))

(defn- create-docs!
  [db index docs]
  (if (seq docs)
    (s/request db {:method :post, :url "/_bulk"
                   :body (s/chunks->body (mapcat (fn [doc]
                                                   (if (vector? doc)
                                                     [{:index {:_index index
                                                               :_id (first doc)
                                                               :_type "doc"}}
                                                      (second doc)]
                                                     [{:index {:_index index}} doc]))
                                                 docs))})))

(defn- get-doc
  [db index id]
  (try
    (:body (s/request db {:method :get
                          :url (str (encode-fragment index) "/doc/"
                                    (encode-fragment id))}))
    (catch Exception e
      (:body (ex-data e)))))

(defn- search-docs
  [db index query]
  (-> (s/request db {:method :get
                     :url (str index "/_search")
                     :body {:query query}})
      :body :hits :hits))

(def ^:private db (delay (let [{uri :elasticsearch.uri} @props
                               db (s/client {:hosts [uri]})]
                           (init-indices! db))))

(defn empty-db!
  []
  (doto @db
    (delete-index! "titles")
    (delete-index! "nodes")
    (init-indices!)))

(defn insert-titles!
  [titles]
  (create-docs! @db "titles"
                (map #(vector % {:title %})
                     titles)))

(defn contains-title?
  [title]
  (:found (get-doc @db "titles" title) false))

(defn search-nodes
  [label]
  (map :_id
       (search-docs @db "nodes"
                    {:bool {:must [{:match {:label label}}
                                   {:match {:named true}}
                                   {:match {:global true}}]}})))

(defn insert-graph-nodes!
  ([g] (insert-graph-nodes! g identity))
  ([g names]
   (create-docs! @db "nodes"
                 (map #(vector (names %)
                               (select-keys (attr/attrs g %)
                                            [:label :group :named :global]))
                      (graph/nodes g)))))
