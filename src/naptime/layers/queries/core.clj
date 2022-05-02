(ns naptime.layers.queries.core
  (:require [next.jdbc.sql :as sql]
            [honey.sql :as hsql]
            [honey.sql.helpers :as hh]))

(defn query [db queries ks f]
  (sql/query db (hsql/format (f (get-in queries ks)))))

(defn read-table
  ([queries table]
   (read-table queries table identity))
  ([queries table f]
   (query (:datasource queries) queries [:queries/read-table table] f)))

(defn create [queries table entities]
  (query (:datasource queries) queries [:queries/create table] #(hh/values % entities)))

(defn delete
  ([queries table]
   (delete queries table identity))
  ([queries table f]
   (query (:datasource queries) queries [:queries/delete table] f)))

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

;; ---------- create queries ----------

(defn get-insert-table-query [table]
  (-> (hh/insert-into table)))

(defn get-create-queries [tables]
  (->> tables
       (map (juxt identity get-insert-table-query))
       (into {})))

;; ---------- delete queries ----------

(defn get-delete-table-query [table]
  (-> (hh/delete-from table)))

(defn get-delete-queries [tables]
  (->> tables
       (map (juxt identity get-delete-table-query))
       (into {})))
