SHELL := /bin/bash

COMPOSE_FILE := infra/compose/docker-compose.yaml

.PHONY: bootstrap run-local run-stack stop test clean

bootstrap:
	@if [ ! -x ./mvnw ]; then \
		if command -v mvn >/dev/null 2>&1; then \
			mvn -N io.takari:maven:wrapper; \
		else \
			echo "Maven wrapper not found and mvn unavailable. Install Maven to run 'make bootstrap' or add mvnw manually."; \
			exit 1; \
		fi; \
	fi

run-local:
	./mvnw -pl services/catalog-service -am spring-boot:run -Dspring-boot.run.profiles=default

run-stack:
	docker compose -f $(COMPOSE_FILE) up --build

stop:
	docker compose -f $(COMPOSE_FILE) down

test:
	./mvnw verify

clean:
	./mvnw clean
