(ns naptime.layers.meta.core
  (:require [next.jdbc.sql :as sql]
            [hugsql.core :as hugsql]))

;; TODO: support other db-types
(defn load-queries
  "keyword of :postgres, :mysql, etc"
  [db-type]
  (hugsql.core/map-of-sqlvec-fns
   (case db-type
     :postgres "postgres.sql")))

(defn get-query [queries k & args]
  (apply (-> queries k :fn) args))

(defn query [db queries k & args]
  (sql/query db (get-query queries k args)))

(defn all-tables [db queries]
  (query db queries :all-tables-sqlvec))

(defn all-columns [db queries]
  (query db queries :all-columns-sqlvec))

(defn many-to-one [db queries]
  (query db queries :many-to-one-sqlvec))

(defn all-primary-keys [db queries]
  (query db queries :all-primary-keys-sqlvec))

(defn all-stored-procedures [db queries]
  (query db queries :all-stored-procedures-sqlvec))

(defn primary-and-foreign-keys-referenced-in-views [db queries]
  (query db queries :primary-and-foreign-keys-referenced-in-views-sqlvec))
