(ns cms0.db
  (:require [duratom.core :as duratom]))

(defonce db (duratom/duratom :local-file
                             :file-path "resources/db"
                             :init {}))
(defn next-id [db collection]
  (or (some->> @db collection (map :id) seq (apply max) inc)
      0))

(defn query [db collection query-fn]
  (->> @db
       collection
       (filter query-fn)))

(defn query-one [db collection query-fn]
  (first (query db collection query-fn)))

(defn transact! [db collection f x]
  (swap! db update collection f x))

(defn replace [xs to-replace]
  (mapv (fn [x]
          (if (= (:id x) (:id to-replace))
            to-replace
            x)) xs))
