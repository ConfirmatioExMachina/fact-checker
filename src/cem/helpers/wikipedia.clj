(ns cem.helpers.wikipedia
  (:require [clj-http.client :as client]
            [clj-http.util :as client-util]
            [clojure.data.json :as json]
            [clojure.string :as str])
  (:import (java.util ArrayList)
           (fastily.jwiki.core Wiki MQuery)))

(def ^:private wiki (delay (Wiki. "en.wikipedia.org")))

(def ^:private infobox-property-map {
                                     :postal_code "postal_code"
                                     :Town "town"
                                     :type "type"
                                     :population "population"
                                     :pop_dens "population_density"})

(defn fetch-summaries
  [titles]
  (let [titles (distinct titles)
        res-map (try
                  (into {} (MQuery/resolveRedirects @wiki (ArrayList. titles)))
                  ; If resolve fails, use the given titles for lookup:
                  (catch Exception e identity))
        res-titles (keep res-map titles)
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
    [titles->ref-titles summaries]))

(defn fetch-summary
  [title]
  (let [[title-map summaries] (fetch-summaries [title])]
    (-> title title-map summaries)))

(defn- extract-infobox
  [body]
  (into (hash-map) (map (fn [x] [(keyword (str/trim (get x 0)),)
                                 (str/trim (get x 1))])
                        (filter (fn [e] (= (count e) 2))
                                (map (fn [i] (str/split (str/trim (str/replace i "\\n" "")) #"="))
                                     (filter (fn [s] (str/includes? s "="))
                                             (str/split (subs body
                                                              (str/index-of body (re-find #"\{\{\s*[Ii]nfobox" body))
                                                              (count body))
                                                        #"\|")))))))

(defn- fetch-infobox
  [title]
  (extract-infobox
   (get
    (client/get
     (str "http://en.wikipedia.org/w/api.php?format=json&action=query&prop=revisions&rvprop=content&titles="
          (client-util/url-encode title)))
    :body)))

(defn get-infobox-elements
  [title]
  (into (hash-map) (map (fn [e] [(keyword (get infobox-property-map (get e 0)))
                                 (get e 1)])
                        (filter (fn [[k v]] (contains? infobox-property-map k))
                                (fetch-infobox title)))))
