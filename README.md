# Bookstore App

Demo application that establishes a minimal but realistic foundation for a future-ready bookstore platform.

## Technology

 - Spring Boot
 - Vertical modules to keep domain, application, and infrastructure concerns isolated per feature
 - Contract first via [catalog-service-api.yaml](libs/api-contract/src/main/resources/openapi/catalog-service-api.yaml)
 - Versioned REST API compliant with [Zalando RESTful API Guidelines](https://opensource.zalando.com/restful-api-guidelines/)
 - PostgreSQL for persistence and full-text search (`websearch` queries)
 - Identity and access management with Keycloak-issued JWTs (HS256 for local profile, RS256 via Envoy in stack mode)
 - Envoy API Gateway with JWT validation, request tracing, rate limiting, and `Accept` header-based routing
   - See also [API_GUIDE.md](API_GUIDE.md)

## Quick start guide

 - Requirements: Java 25
 - Recommended: Docker Compose, `make`

### Local development

1. Start the infrastructure dependencies.
   - PostgreSQL is required – `docker compose -f infra/compose/docker-compose.yaml up postgres`
   - Add `envoy` and `keycloak` containers if you want to exercise the full stack.
2. Launch the catalog service with `make run-local`. The `local` profile aims at `localhost` PostgreSQL and enables an in-process HS256 JWT issuer/decoder, so Keycloak is optional for inner-loop work.
3. On startup the service [logs](services/catalog-service/src/main/java/com/example/bookstore/catalog/config/LocalSecurityConfig.java) ready-to-use Bearer tokens for `admin` and `staff`. Copy the value after `Authorization: Bearer …` and attach it to curl requests or [Swagger UI](http://localhost:8880/swagger-ui/index.html).
4. Run the contract, unit, and integration test suite with `make verify`.

### Production-like deployment

Run `make run-stack` to launch Envoy, Keycloak, PostgreSQL, and the catalog service behind the gateway. Manage demo accounts and secrets via `.env` (see [infra/compose/.env.example](infra/compose/.env.example) and [infra/compose/keycloak/bookstore-realm.json](infra/compose/keycloak/bookstore-realm.json)).

> Heads-up: Docker Compose uses bridge networking to simplify development. This also exposes debug surfaces (for example the Envoy admin port) to the host machine; harden or disable when demoing outside a controlled environment.

### Links

After start, open [http://localhost:8880/swagger-ui/index.html](http://localhost:8880/swagger-ui/index.html).

## Features

### Functional
 
- Authors – CRUD with pagination; deletion detaches books via junction table clean-up.
- Books – CRUD with pagination; authors list can be empty; genres are an enum to avoid premature lookup tables.
- Search – Full-text search with `websearch` query semantics and relevance ordering.

### Non functional
- API Gateway – Separates ingress concerns from the service. Supports future decomposition without breaking client contracts.
- Identity and access management – Built with Keycloak, but can be migrated to AWS Cognito as needed.
- Concurrency control – Standard HTTP mechanisms (`ETag`, `If-Match`, `If-None-Match`) protect against lost updates in concurrent environment.
- Search boundary – Lightweight search resource models stable, index-friendly attributes. Keeps the door open for OpenSearch or external indexers driven by domain events when query demands grow.

## Development guidelines

- Idempotency – All operations are idempotent. Resource creation uses client-provided IDs to avoid duplicates and makes retries safe.
- Persistence vs. domain – Dedicated mappers keep domain purity and allow persistence swaps.
- Module boundaries – Services are atomic, repositories do not cross module boundaries; supports persistence and deployment flexibility.


## Limitations

- Secret management via `.env` files 
  - Production would move to AWS Secrets Manager / Vault with automated rotation.
  - Strict least privilege for services accessing secrets; separate roles for read vs. rotate.
  - Audit logging of secret access (Vault audit device or AWS CloudTrail).
- Observability and monitoring
  - Compose stack uses container health checks only.
  - Production should add centralized logging with alerts, metrics (OpenTelemetry + Prometheus/Grafana or CloudWatch, DataDog), and cost monitoring hooks.
  
### Evolution Path
- **Short Term**: Single service for CRUD/search keeps operational overhead low while delivering required functionality.
- **Medium Term**: Introduce event-driven read models (OpenSearch, DynamoDB) when query load or feature complexity increases.
- **Long Term**: Split search/indexing into dedicated microservices or serverless (Lambda) functions, reusing the established event stream. This unlocks migration to fully serverless CRUD (Lambda + API Gateway + Aurora Serverless) without re-architecting core domain logic.

### Scalability & Operations
- **Stateless Services**: Horizontal scaling through container orchestration (ECS/EKS). Rolling deployments with blue/green releases.
- **Database Scaling**: Aurora read replicas for reporting; Aurora Serverless v2 as demand grows.
- **Caching Layer**: Optional ElastiCache/Redis for hot search results or downtime resiliency.

## Validation and Testing

- Static analysis: Checkstyle, SpotBugs, OWASP Dependency-Check (Maven `verify` bundles them)
- JUnit 5: Unit tests for domain slices
- Integration tests: Testcontainers (PostgreSQL), mock JWT, `MockMvc`; contract coverage directly linked to [catalog-service-api.yaml](libs/api-contract/src/main/resources/openapi/catalog-service-api.yaml)

### Future improvements
- E2E tests covering Keycloak + Envoy flow, running also after production deployment (CD) 
- Performance regression harness (Gatling/Locust) to track possible impact of search/index refactors
