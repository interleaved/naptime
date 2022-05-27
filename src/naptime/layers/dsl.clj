(ns naptime.layers.dsl
  (:require [clojure.string :as sg]
            [clojure.java.io :as io]
            [instaparse.core :as insta :refer [defparser]]))

(defparser select-grammar (io/resource "grammars/select.bnf"))
(defparser order-grammar (io/resource "grammars/order.bnf"))
(defparser filter-grammar (io/resource "grammars/filter.bnf"))

(defn parse-param [[type dsl]]
  (case type
    "select" [:select (insta/parse select-grammar dsl)]
    "limit" [:limit (Long/parseLong dsl)]
    "offset" [:offset (Long/parseLong dsl)]
    "order" [:order (insta/parse order-grammar dsl)]
    "or" [:or (insta/parse filter-grammar dsl :start :or-body)]
    "columns" [:columns (insta/parse select-grammar dsl)]
    (cond
      (sg/ends-with? type ".limit")
      [[:sublimit (insta/parse select-grammar (sg/replace type ".limit" "") :start :col)]
       (Long/parseLong dsl)]
      (sg/ends-with? type ".offset")
      [[:suboffset (insta/parse select-grammar (sg/replace type ".offset" "") :start :col)]
       (Long/parseLong dsl)]
      (sg/ends-with? type ".order")
      [[:suborder (insta/parse select-grammar (sg/replace type ".order" "") :start :col)]
       (insta/parse order-grammar dsl)]
      (sg/ends-with? type ".or")
      [[:subor (insta/parse select-grammar (sg/replace type ".or" "") :start :col)]
       (insta/parse filter-grammar dsl :start :or-body)]
      :else
      [(insta/parse select-grammar type :start :col)
       (insta/parse filter-grammar dsl :start :where)])))

(defn parse-params [params]
  (mapv parse-param params))
