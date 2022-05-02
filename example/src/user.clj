(ns user
  (:require [naptime.db :as db]
            [clojure.pprint :as pp]
            [next.jdbc.sql :as sql]
            [conman.core :as conman]
            [mount.core :as mount :refer [defstate]]))

(def pool-spec {:jdbc-url "jdbc:postgresql://localhost/university?user=postgres"})

(defstate ^:dynamic *db*
  :start (conman/connect! pool-spec)
  :stop (conman/disconnect! *db*))

(defstate queries
  :start (db/load-queries :postgres))

(defn get-tables []
  (db/query *db* queries :all-tables))
