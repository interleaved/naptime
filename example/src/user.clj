(ns user
  (:require [naptime.db :as db]
            [clojure.pprint :as pp]
            [jsonista.core :as json]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as result-set]
            [conman.core :as conman]
            [mount.core :as mount :refer [defstate]])
  (:import [org.postgresql.util PGobject]
           (java.sql PreparedStatement Array)
           (clojure.lang IPersistentVector IPersistentMap)))

(def pool-spec {:jdbc-url "jdbc:postgresql://localhost:1234/studio?user=postgres&password=tpJ4dAmQU52mdL5"})

(defn ->pgobject [type value]
  (doto (PGobject.)
    (.setType type)
    (.setValue (json/write-value-as-string value))))

(defn jsonb [value]
  (->pgobject "JSONB" value))

(defn <-pgobject [^PGobject v]
  (let [type (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (json/read-value value)
      value)))

(extend-protocol result-set/ReadableColumn
  Array
  (read-column-by-index [val _ _]
    (into [] (.getArray val)))

  PGobject
  (read-column-by-label [x _] (<-pgobject x))
  (read-column-by-index [x _2 _3] (<-pgobject x)))

(defstate ^:dynamic *db*
  :start (conman/connect! pool-spec)
  :stop (conman/disconnect! *db*))

(defstate queries
  :start (db/load-queries :postgres))

(defn all-tables [db]
  (db/query db queries :all-tables))

(defn many-to-one [db]
  (db/query db queries :many-to-one))

(clojure.pprint/pprint (many-to-one *db*))
((naptime/handler *db*) (mock/request :get naptime/logical-operators-qs))
