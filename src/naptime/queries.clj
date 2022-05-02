(ns naptime.queries
  (:require [naptime.model :as model]
            [honey.sql.helpers :as hh]))

;; TODO: specify columns
;; TODO: incorporate joins
(defn get-read-table-query [table]
  (-> (hh/select :*)
      (hh/from table)))

(defn get-read-table-queries [db queries]
  (->> queries
       (model/all-tables db)
       (map (comp keyword :pg_class/table_name))
       (map (juxt identity get-read-table-query))
       (into {})))

;; TODO: also build read queries for views
(defn get-read-queries [db queries]
  {:table (get-read-table-queries db queries)})
