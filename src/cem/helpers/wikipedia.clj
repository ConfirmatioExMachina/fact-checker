(ns cem.helpers.wikipedia
  (require [clj-http.client :as client]
           [clj-http.util :as client-util]
           [clojure.data.json :as json]
           [clojure.string :as str])
  (:import (java.util ArrayList)
           (fastily.jwiki.core Wiki MQuery)))

(def wiki (delay (Wiki. "en.wikipedia.org")))
(def blacklist (atom #{}))

(defn fetch-summaries
  [titles]
  (let [bl @blacklist
        titles (distinct titles)
        res-map (try
                      (into {} (MQuery/resolveRedirects @wiki (ArrayList. titles)))
                      ; If resolve fails, use the given titles for lookup:
                      (catch Exception e identity))
        res-titles (->> titles
                        (keep res-map)
                        (remove bl))
        summaries (if-not (empty? res-titles)
                    (into {} (MQuery/getTextExtracts @wiki (ArrayList. res-titles)))
                    {})
        lc-res-titles->res-titles (->> summaries
                                       (map (fn [[t s]] [(str/lower-case t) t]))
                                       (into {}))
        titles->ref-titles (->> titles
                                (map (juxt identity (comp lc-res-titles->res-titles
                                                          str/lower-case
                                                          res-map)))
                                (into {}))]
    (swap! blacklist (partial apply conj)
           (mapcat (fn [title]
                     (when (-> title titles->ref-titles summaries nil?)
                       [title (titles->ref-titles title)]))
              titles))
    [titles->ref-titles summaries]))

(defn fetch-summary
  [title]
  (let [[title-map summaries] (fetch-summaries [title])]
    (-> title title-map summaries)))

(defn- extract-infobox
  [body]
  (map (fn [x] (hash-map :property (str/trim (get x 0)),
                         :value (str/trim (get x 1))))
       (filter (fn [e] (= (count e) 2))
               (map (fn [i] (str/split (str/trim (str/replace i "\\n" "")) #"="))
                    (filter (fn [s] (str/includes? s "="))
                            (str/split (subs body
                                             (str/index-of body (re-find #"\{\{\s*[Ii]nfobox" body))
                                             (count body)
                                          #"\|")))))))

(defn fetch-infobox
  [title]
  (if-not(contains? @blacklist title)
     (extract-infobox
      (get
       (client/get
        (str "http://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&titles="
             (client-util/url-encode title)))
       :body))))
