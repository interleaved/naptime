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
(queries/create queries :professor (repeatedly 10 gen-professor))
;=>[#:next.jdbc{:update-count 10}]

;; read some data
(pp/print-table (queries/read-table queries :professor))
|                        :professor/id | :professor/first_name | :professor/last_name |
|--------------------------------------+-----------------------+----------------------|
| 29932f65-cc24-4d8e-91c7-77e06b64463d |                Ashley |                Baker |
| 01d58b6f-9565-40e4-9d33-1794bb4a4184 |              Kimberly |              Stewart |
| e9bee4f1-adb6-482b-bf93-8f15bab2ce46 |                   Roy |                James |
| b370ac2f-cbe8-411d-966a-7b00b9198305 |               Patrick |                 Reed |
| b2dc3046-d899-433f-973a-3e8c037bfaec |                   Joe |                White |
| c3441dc0-891a-4d58-b316-c2cbff9f903e |                 Frank |                Davis |
| 74a26d44-6384-47db-8d1b-884f62b21898 |                 Helen |               Torres |
| fea3dd5d-6fde-4f71-b7e4-78a49964ef30 |                Daniel |           Richardson |
| 9eca687f-a88f-47e7-9ddc-2d7bb6faa1ad |               Richard |              Jimenez |
| b7c42434-0b45-4b94-bab6-1a0a83ac7542 |               Jessica |            Rodriguez |

;; update some data
(queries/update-table queries :professor
  {:professor/first_name "Jamie"}
  [:= :professor/id (java.util.UUID/fromString "84a7a47a-7d99-4e4e-8ad5-8dc5b8265ed2")])
;=>[#:next.jdbc{:update-count 1}]

;; delete some data
(queries/delete queries :professor)
;=>[#:next.jdbc{:update-count 10}]
```
