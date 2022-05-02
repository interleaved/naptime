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
(defn honey-query [db queries ks f & args]
  (sql/query db (hsql/format (apply f (cons (get-in queries ks) args)))))

(defn read-table [db queries table f & args]
  (apply honey-query (concat [db queries [:read-table table] f] args)))

(defn create [db queries table & args]
  (apply honey-query (concat [db queries [:create table] hh/values] args)))
