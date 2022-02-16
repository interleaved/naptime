(ns naptime.db
  (:require [next.jdbc.sql :as sql]
            [hugsql.core :as hugsql]))

(defn load-queries
  "keyword of :postgres, :mysql, etc"
  [db-type]
  (hugsql.core/map-of-sqlvec-fns
   (case db-type
     :postgres "postgres.sql")))

(defn get-query [queries k & args]
  (let [k (keyword (str (name k) "-sqlvec"))]
    (apply (-> queries k :fn) args)))

(defn query [db queries k & args]
  (sql/query db (get-query queries k args)))
