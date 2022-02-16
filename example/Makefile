.PHONY: db
db:
	@docker-compose up postgres

.PHONY: clean
clean:
	docker-compose stop
	docker-compose rm -f
	sudo rm -rf docker
