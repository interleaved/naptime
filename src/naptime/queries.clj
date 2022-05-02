(ns naptime.queries
  (:require [naptime.model :as model]
            [honey.sql.helpers :as hh]))

;; TODO: also build read queries for views

;; ---------- read-table queries ----------

;; TODO: specify columns
;; TODO: incorporate joins

(defn get-read-table-query [table]
  (-> (hh/select :*)
      (hh/from table)))

(defn get-read-table-queries [tables]
  (->> tables
       (map (juxt identity get-read-table-query))
       (into {})))

;; ---------- create queries ----------

(defn get-insert-table-query [table]
  (-> (hh/insert-into table)))

(defn get-create-queries [tables]
  (->> tables
       (map (juxt identity get-insert-table-query))
       (into {})))
