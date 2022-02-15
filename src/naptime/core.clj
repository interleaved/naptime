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

(defn destruct-parens [s]
  (re-matches #"\((.*)\)" s))

(defn split-comma [s]
  (str/split s #","))

(defn split-dot [s]
  (str/split s #"\."))

(defn split-opening-paren[s]
  (str/split s #"\("))

(defn extract-condition [s]
  (let [[field op value] (split-dot s)]
    [op field value]))

(defn is-condition? [s]
  (= (count (split-dot s)) 3))


(defn extract-expression [s]
  (let [[op & expression] (split-dot s)]
    (when (seq expression)
      (let [sub-expressions (split-opening-paren expression)
            has-sub-expression? (> (count sub-expressions) 1)]
        (if has-sub-expression?
          (into [op] [(first sub-expressions) (map extract-expression (rest sub-expressions))])
          (into [op] (extract-expression expression)))))))

(defn extract-logic [x]
  (let [parts (destruct-parens x)]
    (when (seq parts)
      (let [conditions (split-comma (second parts))]
        (mapv (fn [condition]
                (if (is-condition? condition)
                  (extract-condition condition)
                  (extract-expression condition))) conditions)))))

(defn extract-logic-old [s]
  (let [or-matches (re-matches #"or\((.*)\)" s)
        or-preds (str/split (str/join (rest or-matches)) #"," 2)
        not-matches (re-matches #"not\.(.*)" s)
        pred (str/split s #",")]
    (cond
      or-matches
      (into [:or] (mapv extract-logic or-preds))

      not-matches
      [:not (extract-logic (str/join (rest not-matches)))]

      :else
      (let [[column operator val] (str/split (first pred) #"\.")]
         (if (> (count pred) 1)
            (conj
              (vector (keyword operator) (keyword column) val)
              (extract-logic (str/join (rest pred))))
            (vector (keyword operator) (keyword column) val))))))

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
        select-clause (take-while (comp select? first)   clauses)
        ;; where-clauses (filter     (comp operator? first) clauses)
        ; TODO: detect embed, build join clause, left vs. inner vs. right?
        order-clause  (take-while (comp order? first)    clauses)] ; TODO: multiple?
    {:select (second (first select-clause))
     :where  [] ;;(vec where-clauses)
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


;;(re-matches #"\((.*)\)" "abc")
