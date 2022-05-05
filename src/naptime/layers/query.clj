(ns naptime.layers.query
  (:require [next.jdbc.sql :as sql]
            [honey.sql :as hsql]
            [naptime.layers.dsl :as dsl]
            [naptime.layers.honey :as honey]))

(defn query [meta query]
  (sql/query (:datasource meta) (hsql/format query)))

;; ---------- create fns ----------

(defn create [meta table]
  (fn [params values]
    (query meta (honey/create meta table (dsl/parse-params params) values))))

(defn get-create-fns [meta]
  (->> ((:meta/all-tables meta))
       (map (comp (juxt keyword identity)
                  :pg_class/table_name))
       (reduce
        (fn [a [k table]]
          (->> (create meta table)
               (assoc-in a [:create k])))
        meta)))
