(ns naptime.core
  (:require [naptime.db :as db]
            [naptime.queries :as q]
            [naptime.model :as model]
            [honey.sql.helpers :as hh]))

;; don't need this yet but
;; host->database->schema->table

(defn query-map [db]
  (let [queries (db/load-queries :postgres)
        tables (->> queries
                    (model/all-tables db)
                    (map (comp keyword :pg_class/table_name)))]
    {:create (q/get-create-queries tables)
     :read-table (q/get-read-table-queries tables)}))
