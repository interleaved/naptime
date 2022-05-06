(ns naptime.layers.honey.read
  (:require [honey.sql.helpers :as hh]
            [instaparse.core :refer [transform]]))

;; I should build a bunch of CTEs
;; OR clauses should go in the final query
;; .OR and other filters can go in the associated CTE

;; Join order is specified by SELECT & (HOR.) FILTER params
;; Single SELECT param
;; multiple SELECT params
;; SELECT param controls join order - FILTER params don't

;; step 1 - build a DAG of all the ways to go from T1->T2
;; step 2 - walk the DAG and error when there are multiple routes w/ no hint disambiguation
