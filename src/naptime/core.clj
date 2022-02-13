(ns naptime.core
  (:require [ring.mock.request :as mock] ;; can't locate this
            [reitit.ring :as ring]
            [reitit.dev.pretty :as pretty]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli :as malli]
            [reitit.interceptor.sieppari :as sieppari]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [reitit.ring.middleware.parameters :as parameters]))

(defn read-request [req]
  (let [params (:query-params req)]
    req))

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
                         muuntaja/format-negotiate-middleware
                         muuntaja/format-response-middleware
                         muuntaja/format-request-middleware
                         coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})
    {:executor sieppari/executor}))

(def d ((handler) (-> (mock/request :get "/people")
                     (mock/query-string {:or "(age.lt.18,age.gt.21)"
                                         :name "eq.david"}))))

;; => {"or" "(age.lt.18,age.gt.21)", "name" "eq.david"}
