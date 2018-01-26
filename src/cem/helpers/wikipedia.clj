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
        titles (remove bl titles)
        resolve-map (try
                      (into {} (MQuery/resolveRedirects @wiki (ArrayList. titles)))
                      ; If resolve fails, use the given titles for lookup:
                      (catch Exception e identity))
        resolved-titles (->> titles
                             (keep resolve-map)
                             (remove bl))
        summaries (if-not (empty? resolved-titles)
                    (into {} (MQuery/getTextExtracts @wiki (ArrayList. resolved-titles)))
                    {})
        lc-summaries (->> summaries
                          (map (fn [[k v]] [(str/lower-case k) v]))
                          (into {}))
        unresolved-summaries (->> titles
                                  (map (juxt identity
                                             (comp lc-summaries
                                                   str/lower-case
                                                   resolve-map)))
                                  (into {}))]
    (swap! blacklist (partial apply conj)
           (mapcat (fn [title]
                     (when (-> title unresolved-summaries nil?)
                       (conj #{} title (resolve-map title))))
                   titles))
    unresolved-summaries))

(defn fetch-summary
  [title]
  (get (fetch-summaries [title]) title))

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
