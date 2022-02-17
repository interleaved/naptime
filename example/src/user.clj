(ns user
  (:require [naptime.db :as db]
            [naptime.core :as naptime]
            [ring.mock.request :as mock]
            [clojure.pprint :as pp]
            [next.jdbc.sql :as sql]
            [conman.core :as conman]
            [mount.core :as mount :refer [defstate]]))

(def pool-spec {:jdbc-url "jdbc:postgresql://localhost/naptime?user=postgres"})

(defstate ^:dynamic *db*
  :start (conman/connect! pool-spec)
  :stop (conman/disconnect! *db*))

(defstate queries
  :start (db/load-queries :postgres))

(defn all-tables [db]
  (db/query db queries :all-tables))

(defn many-to-one [db]
  (db/query db queries :many-to-one))

((naptime/handler *db*) (mock/request :get naptime/logical-operators-qs))
