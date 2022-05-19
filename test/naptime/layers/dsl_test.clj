(ns naptime.layers.dsl-test
  (:require [ring.mock.request :as mock]
            [clojure.test :refer :all]
            [naptime.layers.dsl :as dsl]
            [ring.middleware.params :as params]
            [naptime.layers.query :as query]))

(defn query-params [uri-with-query-string]
  (-> (mock/request :get uri-with-query-string)
      params/params-request
      :query-params))

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
