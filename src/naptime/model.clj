(ns naptime.model
  (:require [naptime.db :as db]))

(defn all-tables [db queries]
  (db/hug-query db queries :all-tables-sqlvec))

(defn all-columns [db queries]
  (db/hug-query db queries :all-columns-sqlvec))

(defn many-to-one [db queries]
  (db/hug-query db queries :many-to-one-sqlvec))

(defn all-primary-keys [db queries]
  (db/hug-query db queries :all-primary-keys-sqlvec))

(defn all-stored-procedures [db queries]
  (db/hug-query db queries :all-stored-procedures-sqlvec))

(defn primary-and-foreign-keys-referenced-in-views [db queries]
  (db/hug-query db queries :primary-and-foreign-keys-referenced-in-views-sqlvec))
