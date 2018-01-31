(ns cem.helpers.wikipedia
  "Fetches summaries and infobox data from Wikipedia for given article titles."
  (:require [clojure.string :as str]
            [cem.helpers.alg :as alg])
  (:import (java.util ArrayList)
           (fastily.jwiki.core Wiki MQuery WParser)))

(def ^:private wiki (delay (Wiki. "en.wikipedia.org")))

(defn- extract-infobox
  [page]
  (when page
    (if-let [info-template (->> page
                                (.getTemplates)
                                (some #(when (re-find #"(?i)infobox" (.-title %)) %)))]
      (->> (.keySet info-template)
           (map (fn [prop]
                  (vector (str/replace prop #"[_|]" " ")
                          (->> (-> (.toString (.get info-template prop) true)
                                   (str/replace #"([*\[\]}]+)|(\{\{[a-zA-Z]*)|(<!--.*-->)" "")
                                   (str/replace #"[_|]" " ")
                                   (str/split #"\n+|<br\s*/?>|[0-9]+="))
                               (map str/trim)
                               (remove #(or (str/blank? %)
                                            (> (count %) 40)))))))
           (remove (comp empty? second))
           (into {})))))

(defn fetch-data
  [titles & opts]
  (let [{:keys [fetch-summaries
                fetch-infoboxes]} (if opts
                                    (set opts)
                                    #{:fetch-summaries :fetch-infoboxes})
        titles (distinct titles)
        res-map (try
                  (into {} (MQuery/resolveRedirects @wiki (ArrayList. titles)))
                  ; If resolve fails, use the given titles for lookup:
                  (catch Exception e identity))
        res-titles (ArrayList. (keep res-map titles))
        summaries (future (if (and fetch-summaries (seq res-titles))
                            (into {} (MQuery/getTextExtracts @wiki res-titles))
                            {}))
        infoboxes (if (and fetch-infoboxes (seq res-titles))
                    (->> res-titles
                         (pmap (juxt identity #(extract-infobox (WParser/parsePage @wiki %))))
                         (into {})))
        summaries @summaries
        lc-res-titles->res-titles (->> (merge summaries infoboxes)
                                       (keys)
                                       (map (fn [t] [(str/lower-case t) t]))
                                       (into {}))
        titles->ref-titles (->> titles
                                (map (juxt identity (comp lc-res-titles->res-titles
                                                          str/lower-case
                                                          res-map)))
                                (into {}))]
    {:title-map titles->ref-titles
     :summaries summaries
     :infoboxes infoboxes}))

(defn fetch-summaries
  [titles]
  (fetch-data titles :fetch-summaries))

(defn fetch-infoboxes
  [titles]
  (fetch-data titles :fetch-infoboxes))

(defn fetch-summary
  [title]
  (let [{:keys [title-map summaries]} (fetch-summaries [title])]
    (-> title title-map summaries)))

(defn fetch-infobox
  [title]
  (let [{:keys [title-map infoboxes]} (fetch-infoboxes [title])]
    (-> title title-map infoboxes)))
