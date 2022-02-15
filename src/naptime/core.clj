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

(defn operator? [x]
  (let [s (-> operators vals set)]
    (or (contains? s x) (contains? s (str/upper-case x)))))

(defn unpack-alias [x]
  (let [parts (str/split x #"\:")
        have-alias? (> (count parts) 1)]
    (if have-alias?
      parts
      x)))

(defn extract-select [x] ;; TODO: casting column, jsonb path, computed column, join foreign tables
  [:select (mapv unpack-alias (str/split x #"\,"))])

(defn split-comma2 [s]
  (str/split s #"," 2))

(defn split-dot [s]
  (str/split s #"\."))

(defn extract-filter [key x]
  (let [[op value] (split-dot x)]
    [((keyword op) operators) key value]))

(defn extract-condition [s]
  (let [[field op value] (split-dot s)]
    [(keyword op) (keyword field) value]))

(defn extract-logic [x]
  (cond
    (str/starts-with? x "(")
    (extract-logic (second (re-matches #"\((.*)\)" x)))

    (str/starts-with? x "not")
    [:not (extract-logic (str/replace-first x #"not." ""))]

    (str/starts-with? x "and")
    [:and (extract-logic (str/replace-first x #"and" ""))]

    (str/starts-with? x "or")
    [:or (extract-logic (str/replace-first x #"or" ""))]

    (= 2 (count (split-comma2 x)))
    (let [[head tail] (split-comma2 x)]
      (conj (extract-logic head) (extract-logic tail)))

    :else
    [(extract-condition x)]))

(defn extract-resource-embedding [key x]
  [:resource key x]) ;; affect build (inner) join for honeysql

(defn ->honeysql [params]
  (let [clauses (reduce-kv
                  (fn [acc k v]
                    (conj acc
                      (cond
                        (contains? logical-operators k)
                        (into [k] (extract-logic v))

                        (select? k)
                        (extract-select v)

                        (order? k)
                        (extract-filter k v) ;; TODO

                        :else
                        (extract-filter k v))))
                  [] params)
        _ (prn clauses)
        select-clause (take-while (comp select? first)   clauses)
        where-clauses (filter     (comp operator? first) clauses)
        _ (prn where-clauses)
        ; TODO: detect embed, build join clause, left vs. inner vs. right?
        order-clause  (take-while (comp order? first)    clauses)] ; TODO: multiple?
    {:select (second (first select-clause))
     :where  (vec where-clauses)
     ;; join conditions
     ;; order
     ;; postgres doesn't allow group by
     }))

(defn read-request [req]
  (let [params (:params req)
        target (-> req :path-params :target)
        ;; TODO: read db structure (should read only once) and include
        ;; the information as part of building honeysql
       ]
    (->honeysql params)))

(def mutation-request read-request)

(def routes
  ;; TODO: POST, PUT, DELETE, ignore options for now
  [["/:target"
    {:get {:handler read-request}
     :put {:handler mutation-request}}]
   ["/rpc/:target"
    {:post {:handler mutation-request}}]])

(defn handler []
  (ring/ring-handler
   (ring/router
    routes
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

;; tables
(def renaming-columns "/people?select=fullName:full_name,birthDate:birth_date")
(def logical-operators-qs "/people?select=*&grade=gte.90&student=is.true&or=(age.eq.14,not.and(age.gte.11,age.lte.17))")

;; resource embedding
(def embedding-through-join-tables "/actors?select=films(title,year)")
(def nested-embedding "/actors?select=roles(character,films(title,year))")
(def embedded-filters "/films?select=*,actors(*)&actors.order=last_name,first_name")
(def embedded-top-level-filtering "/films?select=title,actors(first_name,last_name)&actors.first_name=eq.Jehanne")

;; embedding after insertions / updates / deletions
((handler) (mock/request :get logical-operators-qs))
