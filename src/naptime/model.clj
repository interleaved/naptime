(ns naptime.model
  (:require [naptime.db :as db]))

(defn all-tables [db queries]
  (db/query db queries :all-tables))

(defn all-columns [db queries]
  (db/query db queries :all-columns))

(defn many-to-one [db queries]
  (db/query db queries :many-to-one))

(defn all-primary-keys [db queries]
  (db/query db queries :all-primary-keys))

(defn all-stored-procedures [db queries]
  (db/query db queries :all-stored-procedures))

(defn primary-and-foreign-keys-referenced-in-views [db queries]
  (db/query db queries :primary-and-foreign-keys-referenced-in-views))
