(ns naptime.layers.dsl-test
  (:require [ring.mock.request :as mock]
            [clojure.test :refer :all]
            [naptime.layers.dsl :as dsl]
            [ring.util.codec :as codec]
            [ring.util.response :as response]
            [ring.middleware.params :as params]
            [instaparse.core :as insta]))

(defn query-params [uri-with-query-string]
  (-> (mock/request :get uri-with-query-string)
      params/params-request
      :query-params))

(def filter-inputs
  [["age" "lt.13"]
   ["age" "gte.18"]
   ["student" "is.true"]
   ["grade" "gte.90"]
   ["student" "is.true"]
   ["my_tsv" "fts(french).amusant"]
   ["my_tsv" "plfts.The Fat Cats"]
   ["my_tsv" "not.phfts(english).The Fat Cats"]
   ["my_tsv" "not.wfts(french).amusant"]
   ["json_data->>blood_type" "eq.A-"]
   ["json_data->age" "gt.20"]
   ["full_name" "fts.Beckett"]
   ["Unit Price" "lt.200"]
   ["name" "in.(\"Hebdon,John\",\"Williams,Mary\")"]
   ["\"information.cpe\"" "like.*MS*"]
   ["id" "eq.1"]
   ["roles.character" "in.(Chico,Harpo,Groucho)"]
   ["90_comps.year" "eq.1990"]
   ["91_comps.year" "eq.1991"]
   ["roles.actors.first_name" "like.*Tom*"]
   ["actors.first_name" "eq.Jehanne"]
   ["actors.first_name" "eq.Jehanne"]
   ["gross_revenue" "gte.1000000"]
   ["rank" "eq.5"]
   ["central_addresses.code" "eq.AB1000"]
   ["age" "lt.13"]
   ["id" "eq.4"]
   ["active" "is.false"]
   ["id" "eq.1"]
   ["id" "eq.5"]
   ["id" "not.eq.5"]
   ["k" "like.*yx"]
   ["extra" "not.eq.u"]
   ["id" "not.lt.14"]
   ["id" "in.(1,3,5)"]
   ["id" "not.in.(2,4,6,7,8,9,10,11,12,13,14,15)"]
   ["a" "not.is.null"]
   ["a" "is.null"]
   ["done" "is.true"]
   ["done" "is.false"]
   ["done" "is.unknown"]
   ["done" "is.NULL"]
   ["done" "is.TRUE"]
   ["done" "is.FAlSe"]
   ["done" "is.UnKnOwN"]
   ["k" "like.*yx"]
   ["k" "like.xy*"]
   ["k" "like.*YY*"]
   ["k" "not.like.*yx"]
   ["k" "ilike.xy*"]
   ["k" "ilike.*YY*"]
   ["k" "not.ilike.xy*"]
   ["k" "match.yx$"]
   ["k" "match.^xy"]
   ["k" "match.YY"]
   ["k" "not.match.yx$"]
   ["k" "imatch.^xy"]
   ["k" "imatch..*YY.*"]])

(def or-inputs
  [["or" "(age.lt.18,age.gt.21)"]
   ["or" "(age.eq.14,not.and(age.gte.11,age.lte.17))"]])

(def dot-or-inputs
  [["roles.or" "(character.eq.Gummo,character.eq.Zeppo)"]])

(def select-inputs
  [["select" "first_name,age"]
   ["select" "fullName:full_name,birthDate:birth_date"]
   ["select" "full_name,salary::text"]
   ["select" "id,json_data->>blood_type,json_data->phones"]
   ["select" "id,json_data->phones->0->>number"]
   ["select" "id,json_data->blood_type"]
   ["select" "id,json_data->age"]
   ["select" "*,full_name"]
   ["select" "title"]
   ["select" "title,directors(id,last_name)"]
   ["select" "title,director:directors(id,last_name)"]
   ["select" "films(title,year)"]
   ["select" "roles(character,films(title,year))"]
   ["select" "*,actors(*)"]
   ["select" "*,roles(*)"]
   ["select" "*,roles(*)"]
   ["select" "*,actors(*)"]
   ["select" "*,90_comps:competitions(name),91_comps:competitions(name)"]
   ["select" "*,roles(*,actors(*))"]
   ["select" "title,actors(first_name,last_name)"]
   ["select" "title,actors!inner(first_name,last_name)"]
   ["select" "bo_date,gross_revenue,films(title)"]
   ["select" "rank,competitions(name,year),films(title)"]
   ["select" "title,year,director:directors(first_name,last_name)"]
   ["select" "*,addresses(*)"]
   ["select" "name,billing_address(name)"]
   ["select" "name,billing_address:billing_address_id(name)"]
   ["select" "*,billing_address(*)"]
   ["select" "*,central_addresses!billing_address(*)"]
   ["select" "*,central_addresses!billing_address!inner(*)"]])

(def order-inputs
  [["order" "age.desc,height.asc"]
   ["order" "age"]
   ["order" "age.nullsfirst"]
   ["order" "age.desc.nullslast"]
   ["order" "id"]
   ["order" "id.asc"]
   ["order" "extra.asc"]])

(deftest ambiguity-test
  (with-redefs [insta/parse insta/parses]
    (testing "zero select ambiguites"
      (doseq [[input ast] (map (juxt identity (comp second dsl/parse-param)) select-inputs)]
        (is (== 1 (count ast)) input)))
    (testing "zero filter ambiguites"
      (doseq [[input ast] (map (juxt identity (comp second dsl/parse-param)) filter-inputs)]
        (is (== 1 (count ast)) input)))))

;; from this file: https://github.com/PostgREST/postgrest/blob/main/test/spec/Feature/Query/QuerySpec.hs
(deftest dsl-tests
  (testing "matches with equality"
    (is (= [[[[:regular-column "id"]] [:where [:op [:eq [:number "5"]]]]]]
           (dsl/parse-params (query-params "items?id=eq.5")))))

  (testing "matches with equality not using operator"
    (is (= [[[[:regular-column "id"]] [:where [:not] [:op [:eq [:number "5"]]]]] [:order [[:column [:column-name "id"]]]]]
           (dsl/parse-params (query-params "items?id=not.eq.5&order=id")))))

  (testing "matches with more than one condition using not operator"
    (is (is (= [[[[:regular-column "k"]] [:where [:op [:like [:bare-string "*yx"]]]]]
                [[[:regular-column "extra"]] [:where [:not] [:op [:eq [:bare-string "u"]]]]]]
               (dsl/parse-params (query-params "/simple_pk?k=like.*yx&extra=not.eq.u"))))))

  (testing "matches with inequality using not operator"
    (is (=  [[[[:regular-column "id"]]
              [:where [:not] [:op [:lt [:number "14"]]]]]
             [:order [[:column [:column-name "id"] [:asc]]]]]
            (dsl/parse-params (query-params "/items?id=not.lt.14&order=id.asc")))))

  (testing "matches items IN"
    (is (= [[[[:regular-column "id"]] [:where [:op [:in [:number "1"] [:number "3"] [:number "5"]]]]]]
           (dsl/parse-params (query-params "/items?id=in.(1,3,5)")))))

  (testing "matches items NOT IN using not operator"
    ;; TODO: not should not be column-name
    (is (= [[[[:regular-column "id"]]
             [:where
              [:not]
              [:op
               [:in
                [:number "2"]
                [:number "4"]
                [:number "6"]
                [:number "7"]
                [:number "8"]
                [:number "9"]
                [:number "10"]
                [:number "11"]
                [:number "12"]
                [:number "13"]
                [:number "14"]
                [:number "15"]]]]]]
           (dsl/parse-params (query-params "/items?id=not.in.(2,4,6,7,8,9,10,11,12,13,14,15)")))))

  (testing "matches nulls using not operator"
    ;; TODO: might not want null as string but operator type, when translate to sql IS NULL is actually a clause, not value
    (is (= [[[[:regular-column "a"]]
             [:where [:not] [:op [:is "null"]]]]]
           (dsl/parse-params (query-params "/no_pk?a=not.is.null")))))

  (testing "matches nulls in varchar and numeric fields alike"
    ;; TODO: null as op
    (is (= [[[[:regular-column "a"]] [:where [:op [:is "null"]]]]]
           (dsl/parse-params (query-params "/no_pk?a=is.null")))))

  (testing "matches with trilean values"
    (testing "/chores?done=is.true"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "true"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.true")))))

    (testing "/chores?done=is.false"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "false"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.false")))))

    (testing "/chores?done=is.unknown"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "unknown"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.unknown")))))

    (testing "matches with trilean values in upper or mixed case"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "NULL"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.NULL")))))

    (testing "/chores?done=is.TRUE"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "TRUE"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.TRUE")))))

    (testing "/chores?done=is.FAlSe"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "FAlSe"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.FAlSe")))))

    (testing "/chores?done=is.UnKnOwN"
      (is (= [[[[:regular-column "done"]] [:where [:op [:is "UnKnOwN"]]]]]
             (dsl/parse-params (query-params "/chores?done=is.UnKnOwN"))))))

  (testing "fails if 'is' used and there's no null or trilean value"
    (is (-> (query-params "/chores?done=is.nil")
            dsl/parse-params
            first
            second
            insta/failure?))
    (is (-> (query-params "/chores?done=is.ok")
            dsl/parse-params
            first
            second
            insta/failure?)))

  (testing "matches with like"
    (testing "prefix"
      (is (= [[[[:regular-column "k"]]
               [:where [:op [:like [:bare-string "*yx"]]]]]]
             (dsl/parse-params (query-params "/simple_pk?k=like.*yx")))))
    (testing "suffix"
      (is (= [[[[:regular-column "k"]]
               [:where [:op [:like [:bare-string "xy*"]]]]]]
             (dsl/parse-params (query-params "/simple_pk?k=like.xy*")))))
    (testing "prefix and suffix"
      (is (= [[[[:regular-column "k"]]
               [:where [:op [:like [:bare-string "*YY*"]]]]]]
             (dsl/parse-params (query-params "/simple_pk?k=like.*YY*"))))))

  (testing "matches with like using not operator"
    (is (= [[[[:regular-column "k"]]
             [:where [:not] [:op [:like [:bare-string "*yx"]]]]]]
           (dsl/parse-params (query-params "/simple_pk?k=not.like.*yx")))))

  (testing "matches with ilike"
    (testing "suffix"
      (is (= [[[[:regular-column "k"]]
               [:where [:op [:ilike [:bare-string "xy*"]]]]]
              [:order [[:column [:column-name "extra"] [:asc]]]]]
             (dsl/parse-params (query-params "/simple_pk?k=ilike.xy*&order=extra.asc")))))

    (testing "prefix suffix"
      (is (= [[[[:regular-column "k"]]
               [:where [:op [:ilike [:bare-string "*YY*"]]]]]
              [:order [[:column [:column-name "extra"] [:asc]]]]]
             (dsl/parse-params (query-params "/simple_pk?k=ilike.*YY*&order=extra.asc"))))))

  (testing "matches with ilike using not operator"
    ;; TODO: not
    (is (= [[[[:regular-column "k"]]
             [:where [:not] [:op [:ilike [:bare-string "xy*"]]]]]
            [:order [[:column [:column-name "extra"] [:asc]]]]]
           (dsl/parse-params (query-params "/simple_pk?k=not.ilike.xy*&order=extra.asc")))))

  (testing "matches with ~"
    (testing "terminating dollar sign"
      (is (= [[[[:regular-column "k"]] [:where [:op [:match [:bare-string "yx$"]]]]]]
             (dsl/parse-params (query-params "/simple_pk?k=match.yx$")))))

    (testing "head sign prefix"
      (is (= [[[[:regular-column "k"]] [:where [:op [:match [:bare-string "^xy"]]]]]]
             (dsl/parse-params (query-params
                                "/simple_pk?k=match.%5Exy")))))
    (testing "match regular"
      (is (= [[[[:regular-column "k"]] [:where [:op [:match [:bare-string "YY"]]]]]]
             (dsl/parse-params (query-params
                                "/simple_pk?k=match.YY"))))))

  (testing "matches with ~ using not operator"
    ;; TODO: not
    (is (= [[[[:regular-column "k"]]
             [:where [:not] [:op [:match [:bare-string "yx$"]]]]]]
           (dsl/parse-params (query-params "/simple_pk?k=not.match.yx$")))))

  (testing "matches with ~*" ;; case-insensitive
    #_(dsl/parse-params (query-params "/simple_pk?k=imatch.^xy&order=extra.asc"))
    #_(dsl/parse-params (query-params "/simple_pk?k=imatch..*YY.*&order=extra.asc"))))

(deftest url-encoded-query-component-tests
  (testing "uri with ^ sign is not valid"
    (try
      (query-params "/simple_pk?k=match.^xy")
      (catch java.net.URISyntaxException _
        (is true))))

  (testing "by default, server won't decode url-encoded url"
    (is (empty? (query-params (codec/url-encode "/simple_pk?k=match.^xy")))))

  (testing "with form encoded" ;; form encoded only applies to body
    (is (empty?
         (-> (mock/request :get (codec/url-encode "/simple_pk?k=match.^xy"))
             (response/update-header "Content-Type" (constantly "application/x-www-form-urlencoded"))
             params/params-request
             :query-params)))
    (is (empty?
         (-> (mock/request :get (codec/form-encode "/simple_pk?k=match.^xy"))
             (response/update-header "Content-Type" (constantly "application/x-www-form-urlencoded"))
             params/params-request
             :query-params))))

  (testing "work if we url-encode just the component part"
    (is (= {"k" "match.^xy"}
           (query-params (str "/simple_pk?k=" (codec/url-encode "match.^xy")))))))
