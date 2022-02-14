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
   :or    :OR
   :and   :AND})


(defn select?   [x] (= :select x))
(defn order?    [x] (= :order x))
(defn operator? [x] (->> operators vals set x))

(defn unpack-alias [x]
  (let [parts (str/split x #"\:")
        have-alias? (> (count parts) 1)]
    (if have-alias?
      parts
      x)))

(defn extract-select [x] ;; TODO: casting column, jsonb path, computed column, join foreign tables
  [:select (mapv unpack-alias (str/split x #"\,"))])

(defn extract-filter [key x]
  (let [[op value] (str/split x #"\.")]
    [((keyword op) operators) key value]))

(defn extract-logic [s]
  (let [or-matches (re-matches #"or\((.*)\)" s)
        and-matches (re-matches #"and\((.*)\)" s)
        not-matches (re-matches #"not\.(.*)" s)
        pred (str/split s #",")]
    (cond
      or-matches [:or (extract-logic (str/join (rest or-matches)))]
      and-matches [:and (extract-logic (str/join (rest and-matches)))]
      not-matches [:not (extract-logic (str/join (rest not-matches)))]
      :else (do
              (let [[column operator val] (str/split (first pred) #"\.")]
                ;(prn column operator val)
                (if (> (count pred) 1)
                  (conj
                    (vector (keyword operator) (keyword column) val)
                    (extract-logic (str/join (rest pred))))
                  (vector (keyword operator) (keyword column) val)))))))

;;(prn (parse "or(age.eq.14,not.and(age.gte.11,age.lte.17))"))
;;(prn (parse "age.eq.14,not.and(age.gte.11,age.lte.17)"))
;;(prn (parse "not.and(age.gte.11,age.lte.17)"))

(defn extract-resource-embedding [key x]
  [:resource key x]) ;; affect build (inner) join for honeysql

(defn ->honeysql [params]
  (let [clauses (reduce-kv (fn [acc k v]
                     (conj acc
                           (cond
                             (contains? logical-operators k)
                             (extract-logic v)

                             (select? k)
                             (extract-select v)

                             (order? k)
                             (extract-filter k v) ;; TODO

                             :else
                             (extract-filter k v))))
                           [] params)
        select-clause (take-while (comp select? first)   clauses)
        where-clauses (filter     (comp operator? first) clauses)
        order-clause  (take-while (comp order? first)    clauses)] ; TODO: multiple?
    {:select (second (first select-clause))
     :where  (vec where-clauses)
     ;; join conditions
     ;; order
     ;; postgres doesn't allow group by
     }))


(defn read-request [req]
  (let [query-params (:params req)
        _target       (-> req :path-params :target)]
    (->honeysql query-params)))

(def mutation-request read-request)

(defn routes []
  ;; TODO: POST, PUT, DELETE, ignore options for now
  [["/:target"
    {:get {:handler read-request}
     :put {:handler mutation-request}}]
   ["/rpc/:target"
    {:post {:handler mutation-request}}]])

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
(def logical-operators-qs "select=*&grade=gte.90&student=is.true&or=(age.eq.14,not.and(age.gte.11,age.lte.17))")

;; resource embedding
(def embedding-through-join-tables "select=films(title,year)")
(def nested-embedding "select=roles(character,films(title,year))")
(def embedded-filters "select=*,actors(*)&actors.order=last_name,first_name")
(def embedded-top-level-filtering "select=title,actors(first_name,last_name)&actors.first_name=eq.Jehanne")
;; embedding after insertions / updates / deletions

;; ((handler) (req renaming-columns))
;;((handler) (req embedding-through-join-tables))
((handler) (req logical-operators-qs))

;; or=(age.eq.14,not.and(age.gte.11,age.lte.17))
;;
;; honeysql
;; [:or [:= :age 14]
;;      [:not [:and [:>= :age 11] [:<= :age 17]]]]
