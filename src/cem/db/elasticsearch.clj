(ns cem.db.elasticsearch
  (:require [qbits.spandex :as s]
            [qbits.spandex.url :refer [encode-fragment]]
            [cem.db.props :refer [props]]
            [cem.helpers.id :as id]))

(def mapping-types {:articles {:title {:type "text"}}
                    :nodes {:label {:type "text"}}})

(defn- create-index!
  [db name props]
  (try
    (doto db
      (s/request {:method :put
                  :url (encode-fragment name)
                  :body {:mappings {:doc {:properties props}}}}))
    (println (str "Created index " name "."))
    (catch Exception e)))

(defn- delete-index!
  [db name]
  (try
    (s/request db {:method :delete, :url (encode-fragment name)})
    (catch Exception e)))

(defn- init-indices!
  [db]
  (doto db
    (create-index! "articles" (:article mapping-types))
    (create-index! "nodes" (:nodes mapping-types))))

(defn- create-doc!
  ([db index doc]
   (s/request db {:method :post
                  :url (str (encode-fragment index)
                            "/doc")
                  :body doc}))
  ([db index id doc]
   (s/request db {:method :put
                  :url (str (encode-fragment index) "/doc/"
                            (encode-fragment id))
                  :body doc})))

(defn- get-doc
  [db index id]
  (:body (s/request db {:method :get
                        :url (str (encode-fragment index) "/doc/"
                                  (encode-fragment id))})))

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
    (delete-index! "articles")
    (delete-index! "nodes")
    (init-indices!)))

(defn create-article!
  [title]
  (create-doc! @db "articles" title {:title title}))

(empty-db!)
(create-article! (str "test"))
(get-doc @db "articles" "test")
