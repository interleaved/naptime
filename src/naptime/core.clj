(ns naptime.core
  (:require [naptime.db :as db]
            [naptime.queries :as q]))

;; don't need this yet but
;; host->database->schema->table

(defn query-map [db]
  (let [queries (db/load-queries :postgres)]
    {:read (q/get-read-queries db queries)}))
