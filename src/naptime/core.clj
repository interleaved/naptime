(ns naptime.core
  (:require [naptime.layers.meta.core :as meta]
            [naptime.layers.queries.core :as queries]))

;; don't need this yet but
;; host->database->schema->table

(defn init-meta [datasource]
  (let [queries (meta/load-queries :postgres)]
    {:datasource datasource
     :meta/all-tables (partial meta/all-tables datasource queries)
     :meta/all-columns (partial meta/all-columns datasource queries)
     :meta/many-to-one (partial meta/many-to-one datasource queries)
     :meta/all-primary-keys (partial meta/all-primary-keys datasource queries)
     :meta/all-stored-procedures (partial meta/all-stored-procedures datasource queries)
     :meta/primary-and-foreign-keys-referenced-in-views
     (partial meta/primary-and-foreign-keys-referenced-in-views datasource queries)}))

(defn init-queries [meta]
  (let [tables (->> ((:meta/all-tables meta))
                    (map (comp keyword :pg_class/table_name)))]
    (assoc
     meta
     :queries/create (queries/get-create-queries tables)
     :queries/read-table (queries/get-read-table-queries tables)
     :queries/delete (queries/get-delete-queries tables))))
