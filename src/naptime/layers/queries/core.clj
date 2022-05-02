(ns naptime.layers.queries.core
  (:require [next.jdbc.sql :as sql]
            [honey.sql :as hsql]
            [honey.sql.helpers :as hh]))

;; TODO: validation, maybe

(defn query [db queries ks f]
  (sql/query db (hsql/format (f (get-in queries ks)))))

;; TODO: make more consistent

(defn create [queries table entities]
  (query (:datasource queries) queries [:queries/create table] #(hh/values % entities)))

(defn read-table
  ([queries table]
   (read-table queries table identity))
  ([queries table f]
   (query (:datasource queries) queries [:queries/read-table table] f)))

(defn update-table
  ([queries table map]
   (query (:datasource queries) queries [:queries/update table]
          #(hh/set % map)))
  ([queries table map filters]
   (query (:datasource queries) queries [:queries/update table]
          #(-> % (hh/set map) (hh/where filters)))))

(defn delete
  ([queries table]
   (delete queries table identity))
  ([queries table f]
   (query (:datasource queries) queries [:queries/delete table] f)))

;; ---------- create queries ----------

(defn get-insert-table-query [table]
  (-> (hh/insert-into table)))

(defn get-create-queries [tables]
  (->> tables
       (map (juxt identity get-insert-table-query))
       (into {})))

;; ---------- read-table queries ----------

;; TODO: also build read queries for views
;; TODO: specify columns
;; TODO: incorporate joins

(defn get-read-table-query [table]
  (-> (hh/select :*)
      (hh/from table)))

(defn get-read-table-queries [tables]
  (->> tables
       (map (juxt identity get-read-table-query))
       (into {})))

;; ---------- update queries ----------

(defn get-update-table-query [table]
  (-> (hh/update table)))

(defn get-update-queries [tables]
  (->> tables
       (map (juxt identity get-update-table-query))
       (into {})))

;; ---------- delete queries ----------

(defn get-delete-table-query [table]
  (-> (hh/delete-from table)))

(defn get-delete-queries [tables]
  (->> tables
       (map (juxt identity get-delete-table-query))
       (into {})))
