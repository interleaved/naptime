(ns naptime.db
  (:require [next.jdbc.sql :as sql]
            [hugsql.core :as hugsql]
            [honey.sql :as hsql]
            [honey.sql.helpers :as hh]))

(defn load-queries
  "keyword of :postgres, :mysql, etc"
  [db-type]
  (hugsql.core/map-of-sqlvec-fns
   (case db-type
     :postgres "postgres.sql")))

(defn get-query [queries k & args]
  (apply (-> queries k :fn) args))

(defn hug-query [db queries k & args]
  (sql/query db (get-query queries k args)))

;; TODO: f could be single-arity and use ks for varargs instead
(defn honey-query [db queries ks f]
  (sql/query db (hsql/format (f (get-in queries ks)))))

(defn read-table
  ([queries table]
   (read-table queries table identity))
  ([queries table f]
   (honey-query (:datasource queries) queries [:read-table table] f)))

(defn create [queries table entities]
  (honey-query (:datasource queries) queries [:create table] #(hh/values % entities)))

(defn delete
  ([queries table]
   (delete queries table identity))
  ([queries table f]
   (honey-query (:datasource queries) queries [:delete table] f)))
