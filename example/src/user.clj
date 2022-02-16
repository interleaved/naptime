(ns user
  (:require [naptime.db :as db]
            [next.jdbc.sql :as sql]
            [conman.core :as conman]
            [mount.core :as mount :refer [defstate]]))

(def pool-spec {:jdbc-url "jdbc:postgresql://localhost/naptime?user=postgres"})

(defstate ^:dynamic *db*
  :start (conman/connect! pool-spec)
  :stop (conman/disconnect! *db*))
