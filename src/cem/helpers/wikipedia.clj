(ns cem.helpers.wikipedia
  (require [clj-http.client :as client]
           [clj-http.util :as client-util]
           [clojure.data.json :as json]
           [clojure.string :as string])
  (:import (fastily.jwiki.core Wiki)))

(def wikipedia (delay (Wiki. "en.wikipedia.org")))
(def blacklist (atom #{}))

(defn fetch-summary
  [title]
  (if-not (contains? @blacklist title)
    (let [summary (.getTextExtract @wikipedia title)]
      (if (some? summary)
        summary
        (do
          (swap! blacklist conj title)
          nil)))))



(defn- extract-infobox
  [body]
  (map (fn [x] (hash-map :property (string/trim (get x 0)),
                         :value (string/trim (get x 1))))
       (filter (fn [e] (= (count e) 2))
               (map (fn [i] (string/split (string/trim (string/replace i "\\n" "")) #"="))
                    (filter (fn [s] (string/includes? s "="))
                            (string/split (subs body
                                                (string/index-of body (re-find #"\{\{\s*[Ii]nfobox" body))
                                                (count body))
                                          #"\|"))))))

(defn fetch-infobox
  [title]
  (if-not(contains? @blacklist title)
     (extract-infobox
      (get
       (client/get
        (str "http://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&titles="
             (client-util/url-encode title)))
       :body))))
