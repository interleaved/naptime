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
(pp/print-table (model/all-tables))

| :pg_namespace/table_schema | :pg_class/table_name | :table_description | :insertable |
|----------------------------+----------------------+--------------------+-------------|
|                  hollywood |               actors |                    |       false |
|                  hollywood |           bacons_law |                    |       false |
|                  hollywood |         competitions |                    |        true |
|                  hollywood |            directors |                    |       false |
|                  hollywood |                films |                    |        true |
|                  hollywood |              friends |                    |       false |
|                  hollywood |          nominations |                    |        true |
|                  hollywood |              persons |                    |        true |
|                  hollywood |                roles |                    |        true |
|                  hollywood |                stats |                    |       false |
|                     public |    schema_migrations |                    |        true |
|                      world |          all_friends |                    |       false |
|                      world |         competitions |                    |        true |
|                      world |              friends |                    |        true |
|                      world |          nominations |                    |        true |
|                      world |              persons |                    |        true |
```
