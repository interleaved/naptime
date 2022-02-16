(ns naptime.core
  (:require [muuntaja.core :as m]
            [honey.sql :as sql]
            [clojure.string :as str]
            [ring.mock.request :as mock]
            [ring.middleware.keyword-params :as kparams]
            [reitit.ring :as ring]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli :as malli]
            [reitit.interceptor.sieppari :as sieppari]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]))

(def logical-operators #{:or :and})

(def order-keyword
  {"asc" :asc
   "desc" :desc
   "nullsfirst" :nulls-first
   "nullslast" :nulls-last})

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
   :is    :=
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

(defn keyword-upper-case [x]
  (-> x name str/upper-case keyword))

(defn select?   [x] (= :select x))
(defn order?    [x] (= :order x))

(defn operator? [x]
  (let [s (-> operators vals set)]
    (or (contains? s x) (contains? s (keyword-upper-case x)))))

(defn split-colon [s]
  (str/split s #":"))

(defn split-comma [s]
  (str/split s #","))

(defn split-comma2 [s]
  (str/split s #"," 2))

(defn split-dot [s]
  (str/split s #"\."))

(defn unpack-alias [x]
  (let [parts (split-colon x)
        have-alias? (> (count parts) 1)]
    (if have-alias?
      parts
      x)))

(defn extract-select [x] ;; TODO: casting column, jsonb path, computed column, unpack parens
  (mapv unpack-alias (str/split x #"\,")))

(defn extract-filter [key x]
  (let [[op value] (split-dot x)]
    [((keyword op) operators) key value]))

(defn extract-condition [s]
  (let [[field op value] (split-dot s)]
    [((keyword op) operators) (keyword field) value]))

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

(defn extract-order [x]
  (mapv (fn [each]
          (let [parts (split-dot each)]
            (if (= 2 (count parts))
              [(keyword (first parts)) (->> parts second (get order-keyword))]
              [(keyword (first parts)) :asc])))
        (split-comma x)))

(defn ->honeysql [params]
  (let [clauses (reduce-kv
                  (fn [acc k v]
                    (conj acc
                      (cond
                        (contains? logical-operators k)
                        (into [k] (extract-logic v))

                        (select? k)
                        (into [:select] (extract-select v))

                        (order? k)
                        (into [:order] (extract-order v))

                        :else
                        (extract-filter k v))))
                  [] params)
        select-clause (take-while (comp select? first)   clauses)
        where-clauses (filter     (comp operator? first) clauses)
        ; TODO: detect embed, build join clause, left vs. inner vs. right?
        order-clause  (filter (comp order? first)    clauses)]
    {:select (second (first select-clause))
     :where  (into [:and] where-clauses)
     ;; join conditions
     :order-by (vec (rest (first order-clause)))
     ;; postgrest doesn't allow group by
     }))

(def x (atom nil))
(def y (atom nil))
(def z (atom nil))

(defn read-request [req]
  (let [params (:params req)
        target (-> req :path-params :target)
        ;; TODO: read db structure (should read only once) and include
        ;; the information as part of building honeysql

        ;; TODO: not a valid sql
        _ (prn (->honeysql params))
       ]
    (reset! x params)
    (reset! y (->honeysql params))
    (reset! z (sql/format (->honeysql params)))
    (sql/format (->honeysql params))))

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
(def logical-operators-qs "/people?select=*&grade=gte.90&student=is.true&or=(age.eq.14,not.and(age.gte.11,age.lte.17))&order=age.desc,height.asc,david")

;; resource embedding
(def embedding-through-join-tables "/actors?select=films(title,year)")
(def nested-embedding "/actors?select=roles(character,films(title,year))")
(def embedded-filters "/films?select=*,actors(*)&actors.order=last_name,first_name")
(def embedded-top-level-filtering "/films?select=title,actors(first_name,last_name)&actors.first_name=eq.Jehanne")

;; embedding after insertions / updates / deletions
((handler) (mock/request :get logical-operators-qs))
