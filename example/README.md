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
