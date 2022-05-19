(ns naptime.layers.dsl-test
  (:require [ring.mock.request :as mock]
            [clojure.test :refer :all]
            [naptime.layers.dsl :as dsl]
            [ring.middleware.params :as params]
            [naptime.layers.query :as query]
            [instaparse.core :as insta]))

(def input
  [{"age" "lt.13"}
   {"age" "gte.18" "student" "is.true"}
   {"or" "(age.lt.18,age.gt.21)"}
   {"grade" "gte.90" "student" "is.true" "or" "(age.eq.14,not.and(age.gte.11,age.lte.17))"}
   {"my_tsv" "fts(french).amusant"}
   {"my_tsv" "plfts.The Fat Cats"}
   {"my_tsv" "not.phfts(english).The Fat Cats"}
   {"my_tsv" "not.wfts(french).amusant"}
   {"select" "first_name,age"}
   {"select" "fullName:full_name,birthDate:birth_date"}
   {"select" "full_name,salary::text"}
   {"select" "id,json_data->>blood_type,json_data->phones"}
   {"select" "id,json_data->phones->0->>number"}
   {"select" "id,json_data->blood_type" "json_data->>blood_type" "eq.A-"}
   {"select" "id,json_data->age" "json_data->age" "gt.20"}
   {"full_name" "fts.Beckett"}
   {"select" "*,full_name"}
   {"Unit Price" "lt.200"}
   {"name" "in.(\"Hebdon,John\",\"Williams,Mary\")"}
   {"\"information.cpe\"" "like.*MS*"}
   {"order" "age.desc,height.asc"}
   {"order" "age"}
   {"order" "age.nullsfirst"}
   {"order" "age.desc.nullslast"}
   {"id" "eq.1"}
   {"select" "title"}
   {"select" "title,directors(id,last_name)"}
   {"select" "title,director:directors(id,last_name)"}
   {"select" "films(title,year)"}
   {"select" "roles(character,films(title,year))"}
   {"select" "*,actors(*)" "actors.order" "last_name,first_name"}
   {"select" "*,roles(*)" "roles.character" "in.(Chico,Harpo,Groucho)"}
   {"select" "*,roles(*)" "roles.or" "(character.eq.Gummo,character.eq.Zeppo)"}
   {"select" "*,actors(*)"}
   {"select" "*,90_comps:competitions(name),91_comps:competitions(name)" "90_comps.year" "eq.1990" "91_comps.year" "eq.1991"}
   {"select" "*,roles(*,actors(*))" "roles.actors.order" "last_name" "roles.actors.first_name" "like.*Tom*"}
   {"select" "title,actors(first_name,last_name)" "actors.first_name" "eq.Jehanne"}
   {"select" "title,actors!inner(first_name,last_name)" "actors.first_name" "eq.Jehanne"}
   {"select" "bo_date,gross_revenue,films(title)" "gross_revenue" "gte.1000000"}
   {"select" "rank,competitions(name,year),films(title)" "rank" "eq.5"}
   {"select" "title,year,director:directors(first_name,last_name)"}
   {"select" "*,addresses(*)"}
   {"select" "name,billing_address(name)"}
   {"select" "name,billing_address:billing_address_id(name)"}
   {"select" "*,billing_address(*)"}
   {"select" "*,central_addresses!billing_address(*)"}
   {"select" "*,central_addresses!billing_address!inner(*)" "central_addresses.code" "eq.AB1000"}
   {"age" "lt.13"}
   {"id" "eq.4"}
   {"active" "is.false"}
   {"id" "eq.1"}
   {"columns" "source,publication_date,figure"}])

(defn query-params [uri-with-query-string]
  (-> (mock/request :get uri-with-query-string)
      params/params-request
      :query-params))

(defn select-inputs [k]
  (->> input
       (map #(select-keys % [k]))
       (keep seq)
       (mapcat identity)))

(deftest ambiguity-test
  (with-redefs [insta/parse insta/parses]
    (testing "zero select ambiguites"
      (is (every? #(== % 1) (map (comp count second dsl/parse-param) (select-inputs "select")))))))

;; from this file: https://github.com/PostgREST/postgrest/blob/main/test/spec/Feature/Query/QuerySpec.hs
(deftest dsl-tests
  (testing "matches with equality"
    (is (= [[[[:regular-column "id"]] [[:condition [:op [:eq [:number "5"]]]]]]]
           (dsl/parse-params (query-params "items?id=eq.5")))))

  (testing "matches with equality not using operator"
    ;; TODO: not should not be a :column-name but :not
    (is (= [[[[:regular-column "id"]] [[:condition [:column-name "not"] [:op [:eq [:number "5"]]]]]] [:order [[:column [:column-name "id"]]]]]
           (dsl/parse-params (query-params "items?id=not.eq.5&order=id")))))

  (testing "matches with more than one condition using not operator"
    (is (vector? (dsl/parse-params (query-params "/simple_pk?k=like.*yx&extra=not.eq.u")))))

  (testing "matches with inequality using not operator"
    (is (vector? (dsl/parse-params (query-params "/items?id=not.lt.14&order=id.asc"))))))
