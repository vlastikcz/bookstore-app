SHELL := /bin/bash

COMPOSE_FILE := infra/compose/docker-compose.yaml

.PHONY: run-local run-stack stop test clean

run-local:
	./mvnw -pl services/catalog-service -am spring-boot:run -Dspring-boot.run.profiles=local

run-stack:
	docker compose -f $(COMPOSE_FILE) up --build

stop:
	docker compose -f $(COMPOSE_FILE) down

test:
	./mvnw verify

clean:
	./mvnw clean
