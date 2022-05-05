(ns naptime.layers.honey.create
  (:require [honey.sql.helpers :as hh]
            [instaparse.core :refer [transform]]))

(defn regular-column? [xs]
  (= (first xs) :regular-column))

(defn column-in-table? [table m]
  (= (:pg_class/table_name m) table))

(defn get-table-columns [meta table]
  (->> ((:meta/all-columns meta))
       (filterv (partial column-in-table? table))))

(defn gather-columns [xs]
  (->> xs (filter regular-column?) (mapv second)))

(defn get-columns-for-insert [meta table parsed-dsl]
  (if-let [columns (some #(when (= (first %) :columns) %) parsed-dsl)]
    (mapv keyword (transform {:columns gather-columns} columns))
    (->> table (get-table-columns meta)
         (mapv (comp keyword :pg_attribute/name)))))

;; columns is the only applicable feature for CREATE
(defn create [meta table parsed-dsl values]
  (let [columns (get-columns-for-insert meta table parsed-dsl)]
    (-> (hh/insert-into (keyword table))
        (hh/values (mapv #(select-keys % columns) values)))))
