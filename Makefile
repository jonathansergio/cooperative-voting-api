# Cooperative Voting API — single verification entrypoint.
# Source of truth for commands: docs/local-setup.md. Test targets need Docker running
# (Testcontainers starts its own PostgreSQL).
.DEFAULT_GOAL := help
.PHONY: help verify build test lint format cov mutation run db-up db-down up down load clean

MVN := ./mvnw -B -ntp

help: ## List targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN{FS=":.*?## "}{printf "  %-10s %s\n", $$1, $$2}'

verify: ## Full gate: format check + tests + coverage gate (mirrors CI)
	$(MVN) verify

build: ## Package the jar, skipping tests
	$(MVN) -DskipTests package

test: ## Run the test suite
	$(MVN) test

lint: ## Check formatting without rewriting files
	$(MVN) spotless:check

format: ## Auto-format the source
	$(MVN) spotless:apply

cov: verify ## Coverage report at target/site/jacoco/index.html
	@echo "report: target/site/jacoco/index.html"

mutation: ## Mutation testing over the services (slow, on demand)
	$(MVN) -Pmutation test-compile org.pitest:pitest-maven:mutationCoverage

run: ## Run the API on the host against the compose database
	$(MVN) spring-boot:run

db-up: ## Start PostgreSQL only
	docker compose up -d db

db-down: ## Stop the database
	docker compose down

up: ## Start the whole stack (API + database) in containers
	docker compose --profile app up --build -d

down: ## Stop the whole stack
	docker compose --profile app down

load: ## Run the k6 load test against a running API
	docker run --rm --network host -v $(PWD)/load:/load grafana/k6 run /load/voting.js

clean: ## Remove build output
	$(MVN) clean
