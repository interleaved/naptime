(ns naptime.core
  (:require [naptime.layers.meta :as meta]
            [naptime.layers.query :as query]
            [naptime.layers.graph :as graph]
            [clojure.core.memoize :as memo]))

;; don't need this yet but
;; host->database->schema->table

(defn init-meta
  ([datasource]
   (init-meta datasource {}))
  ([datasource
    {:keys [ttl]
     :or {ttl (* 6 3600000)}
     :as opts}]
   (let [queries (meta/load-queries :postgres)]
     {:datasource datasource
      :meta/all-tables (memo/ttl (partial meta/all-tables datasource queries) :ttl/threshold ttl)
      :meta/all-columns (memo/ttl (partial meta/all-columns datasource queries) :ttl/threshold ttl)
      :meta/many-to-one (memo/ttl (partial meta/many-to-one datasource queries) :ttl/threshold ttl)
      :meta/all-primary-keys (memo/ttl (partial meta/all-primary-keys datasource queries) :ttl/threshold ttl)
      :meta/all-stored-procedures (memo/ttl (partial meta/all-stored-procedures datasource queries) :ttl/threshold ttl)
      :meta/primary-and-foreign-keys-referenced-in-views
      (memo/ttl (partial meta/primary-and-foreign-keys-referenced-in-views datasource queries) :ttl/threshold ttl)})))

(defn init-queries [meta]
  (-> meta query/get-create-fns))
