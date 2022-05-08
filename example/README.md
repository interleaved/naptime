# How to use

First, start the database.

```bash
make db
```

Then, in another terminal...

```bash
clj -M:migrate init
clj -M:migrate migrate
```

Now, connect to the database in your favorite repl.

```clojure
(mount/start)

;; create some data
((-> queries :create :professor) 
 [] (repeatedly 10 gen-professor))
;=>[#:next.jdbc{:update-count 10}]

;; already supports the `columns` param
((-> queries :create :professor) 
 {"columns" "first_name,last_name"}
 (repeatedly 10 gen-professor))
;=>[#:next.jdbc{:update-count 10}]

```

The graph layer is built out already.

```clojure
(def g (graph/build-graph queries))

(ubergraph.core/viz-graph g)
```

![university visualization](resources/university.png "University visualization")

```clojure
;; You can search for an ordered set of constraints between tables
(graph/find-path g "class" "material")
;=> ["class_material_material_key" "class_material_class_key"]

;; naptime can't select a path if more than one path exists
(graph/find-path g "class" "address")
;=> nil

;; So you can provide a hint.  Here, we tell naptime to join through the professor table.
(graph/find-path g "class" "address" {:tbl "professor"})
;=> ("class_professor_key" "professor_address_key")

;; Sometimes even that isn't enough. Here, it's not enough to find billing addresses for students in classes.
(graph/find-path g "class" "address" {:tbl "student"})
;=> nil

;; To get around this, naptime also finds paths based on constraint name hints.  Here, we specify that we want the student's shipping address instead of the billing address by specifyng the foreign key.
(graph/find-path g "class" "address" {:fk "student_billing_address_key"})
;=> ("class_student_student_key"
     "class_student_class_key"
     "student_billing_address_key")
     
;; If two tables have only one constraint between them, naptime returns that even if there are multiple paths. In this example, you can get to "address" by going through "students", but because "address" is only one step away, naptime assumes you want that.
(graph/find-path g "professor" "address" )
;=> ("professor_address_key")

;; However, that shortcut won't work if there are two similar constraints.  Here, naptime can't choose between the student's billing and shipping address.
(graph/find-path g "student" "address" )
;=> nil

;; You must provide a foreign key hint in this case.
(graph/find-path g "student" "address" {:fk "student_billing_address_key"})
;=> ["student_billing_address_key"]
```
