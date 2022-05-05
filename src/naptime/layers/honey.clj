(ns naptime.layers.honey
  (:require [naptime.layers.honey.create :as create]))

(defn create [meta table parsed-dsl values]
  (create/create meta table parsed-dsl values))
