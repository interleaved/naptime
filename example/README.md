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
(mount/start *db*)
(sql/query *db* ["SELECT 1;"])
```
