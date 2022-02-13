(ns naptime.core
  (:require [clojure.string :as str]
            [ring.mock.request :as mock]
            [ring.middleware.keyword-params :as kparams]
            [reitit.ring :as ring]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli :as malli]
            [reitit.interceptor.sieppari :as sieppari]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [reitit.ring.middleware.parameters :as parameters]))

(def logical-operators #{:or :and})

(def operators
  {:eq    :=
   :gt    :>
   :gte   :>=
   :lt    :<
   :lte   :<=
   :neq   :<>
   :like  :LIKE
   :ilike :ILIKE
   :in    :IN
   :is    :IS
   :fts   (keyword "@@")
   :plfts (keyword "@@")
   :phfts (keyword "@@")
   :wfts  (keyword "@@")
   :cs    (keyword "@>")
   :cd    (keyword "<@")
   :ov    (keyword "&&")
   :sl    (keyword "<<")
   :sr    (keyword ">>")
   :nxr   (keyword "&<")
   :nxl   (keyword "&>")
   :adj   (keyword "-|-")
   :not   :NOT
   :or    :or
   :and   :AND})

(defn is-resource-embeddding? [x]
  (= 2 (count (str/split x #"\."))))

;; {:grade "op.value" :and "", }
;; [[<type> <key> <value>] .... ] type :: filter | logic | resource-embedding

(defn parse-read-request [params]
  (reduce-kv (fn [acc k v]
               (cond
                 (contains? logical-operators k)
                 (conj acc [:logic k v])

                 (is-resource-embeddding? (name k))
                 (conj acc [:resource-embedding k v])

                 :else
                 (conj acc [:filter k v]))
               ) [] params))

(defn extract-filter [key x]
  (let [[op value] (str/split x #"\.")]
    [((keyword op) operators) key value]))

(defn extract-logic [key x]
  [:logic key x])

(defn extract-resource-embedding [key x]
  [:resource key x])

(defn ->hsql-map [tagged-values]
  (reduce (fn [acc [type key value]]
            (condp = type
              :filter             (conj acc (extract-filter key value))
              :logic              (conj acc (extract-logic  key value))
              :resource-embedding (conj acc (extract-resource-embedding key value))

              )) [] tagged-values))

(defn read-request [req]
  (let [query-params (:params req)
        _target       (-> req :path-params :target)]
    (-> query-params
        (parse-read-request)
        (->hsql-map))))

(defn routes []
  ;; TODO: POST, PUT, DELETE, ignore options for now
  [["/:target"
    {:get {:handler read-request}}]])

(defn handler []
  (ring/ring-handler
   (ring/router
    (routes)
    {:exception pretty/exception
     :data {:muuntaja m/instance
            :coercion malli/coercion
            :middleware [parameters/parameters-middleware
                         kparams/wrap-keyword-params
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})
    {:executor sieppari/executor}))

(defn req [qs] (-> (mock/request :get "/target-table")
                   (mock/query-string qs)))

;; tables and views
(def renaming-columns "select=fullName:full_name,birthDate:birth_date")
(def logical-operators-qs "grade=gte.90&student=is.true&or=(age.eq.14,not.and(age.gte.11,age.lte.17))")

;; resource embedding
(def embedding-through-join-tables "select=films(title,year)")
(def nested-embedding "select=roles(character,films(title,year))")
(def embedded-filters "select=*,actors(*)&actors.order=last_name,first_name")
(def embedded-top-level-filtering "select=title,actors(first_name,last_name)&actors.first_name=eq.Jehanne")
;; embedding after insertions / updates / deletions

((handler) (req renaming-columns))
;;((handler) (req embedding-through-join-tables))
((handler) (req logical-operators-qs))

;; or=(age.eq.14,not.and(age.gte.11,age.lte.17))
;;
;; honeysql
;; [:or [:= :age 14]
;;      [:not [:and [:>= :age 11] [:<= :age 17]]]]
