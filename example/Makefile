ARGS := $(wordlist 2,$(words $(MAKECMDGOALS)),$(MAKECMDGOALS))
$(eval $(ARGS):;@:)

.PHONY: db
db:
	@docker-compose up postgres

.PHONY: clean
clean:
	docker-compose stop
	docker-compose rm -f
	sudo rm -rf docker

.PHONY: migratus-migrate
migratus-migrate:
	clj -M:migrate migrate

.PHONY: migratus-create
migratus-create:
	clj -M:migrate create $(ARGS)
