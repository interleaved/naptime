(ns naptime.layers.graph
  (:require [ubergraph.core :as uber]
            [ubergraph.alg :as alg]))

(defn make-edge
  [{:keys [pg_class/table_name
           pg_class/foreign_table_name
           pg_constraint/constraint_name]}]
  [table_name foreign_table_name
   {:constraint constraint_name :weight 1}])

(defn build-graph [meta]
  (-> (uber/multigraph)
      (uber/add-nodes*
       (->> ((:meta/all-tables meta))
            (map :pg_class/table_name)))
      (uber/add-edges*
       (->> ((:meta/many-to-one meta))
            (map make-edge)))))

(defn get-path
  ([g from to]
   (get-path g from to {}))
  ([g from to filters]
   (->> filters
        (merge {:start-node from
                :end-node to
                :cost-attr :weight})
        (alg/shortest-path g))))

(defn edge-count [g]
  (count (uber/edges g)))

(defn invalidate-path [g edges]
  (let [weight (edge-count g)]
    (reduce #(uber/add-attr %1 %2 :weight weight) g edges)))

(defn get-unique-path
  ([g from to]
   (get-unique-path g from to {}))
  ([g from to filters]
   (let [path1 (-> g (get-path from to filters) alg/edges-in-path set)]
     (when (= path1 (-> g (invalidate-path path1)
                        (get-path from to filters)
                        alg/edges-in-path set))
       (mapv #(uber/attr g % :constraint) path1)))))

(defn is-constraint?
  ([g constraint-name]
   (fn [constraint]
     (let [constraint (uber/edge-with-attrs g constraint)]
       (when (= (get-in constraint [2 :constraint]) constraint-name)
         constraint))))
  ([g from to]
   (fn [constraint]
     (let [[a b {:keys [constraint]}] (uber/edge-with-attrs g constraint)]
       (when (and (= from a) (= to b))
         constraint)))))

(defn lookup-constraint [g constraint-name]
  (->> g uber/edges (some (is-constraint? g constraint-name))))

(defn lookup-constraints [g from to]
  (->> g uber/edges (keep (is-constraint? g from to))))

(defn order-nodes [g from to fk]
  (when-let [[mid1 mid2 _] (lookup-constraint g fk)]
    (cond
      (and (= from mid1) (= to mid2)) [:0-jump []]
      (and (= from mid2) (= to mid1)) [:0-jump []]
      (= to mid1) [:1-jump [from mid2]]
      (= to mid2) [:1-jump [from mid1]]
      (= from mid1) [:2-jump [mid2 to]]
      (= from mid2) [:2-jump [mid1 to]]
      :else [:n-jump [from mid1 fk mid2 to] [from mid2 fk mid1 to]])))

(defn node-filter [blacklist]
  (let [blacklist (set blacklist)]
    (fn [node]
      (nil? (blacklist node)))))

(defn edge-filter [g blacklist]
  (let [blacklist (set blacklist)]
    (fn [edge]
      (nil? (blacklist (uber/attr g edge :constraint))))))

(defn find-path-with-tbl-hint [g from mid to]
  (when-let [path1 (get-unique-path
                    g from mid {:node-filter (node-filter [to])})]
    (when-let [path2 (get-unique-path
                      g mid to {:node-filter (node-filter [from])})]
      (concat path1 path2))))

(defn find-winding-path [g [a b fk c d]]
  (when-let [path1 (get-unique-path
                    g a b
                    {:node-filter (node-filter [c d])
                     :edge-filter (edge-filter g [fk])})]
    (when-let [path2 (get-unique-path
                      g c d
                      {:node-filter (node-filter [a b])
                       :edge-filter (edge-filter g [fk])})]
      (concat path1 [fk] path2))))

(defn find-path-with-fk-hint [g from fk to]
  (when-let [[tag jumps] (order-nodes g from to fk)]
    (case tag
      :0-jump [fk]
      :1-jump (let [[a b] jumps]
                (when-let [path (get-unique-path
                                 g a b
                                 {:node-filter (node-filter [to])
                                  :edge-filter (edge-filter g [fk])})]
                  (concat path [fk])))
      :2-jump (let [[a b] jumps]
                (when-let [path (get-unique-path
                                 g a b
                                 {:node-filter (node-filter [from])
                                  :edge-filter (edge-filter g [fk])})]
                  (concat [fk] path)))
      (let [n1 (first jumps)
            n2 (second jumps)
            p1 (find-winding-path g n1)
            p2 (find-winding-path g n2)]
        ;; logical xor would be handy here....
        (cond
          (and (seq p1) (not (seq p2))) p1
          (and (seq p2) (not (seq p1))) p2
          :else nil)))))

(defn find-path
  ([g from to]
   (let [edges (distinct (lookup-constraints g from to))]
     (if (= (count edges) 1)
       edges
       (when-let [path (get-unique-path g from to)]
         path))))
  ([g from to {:keys [tbl fk] :as hint}]
   (cond
     (some? fk) (find-path-with-fk-hint g from fk to)
     (some? tbl) (find-path-with-tbl-hint g from tbl to))))
