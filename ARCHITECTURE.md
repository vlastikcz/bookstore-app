# Bookstore Inventory Management System – High-Level Architecture

## Vision
Design a modular, future-ready platform that starts lean but can evolve into a fully distributed, event-driven ecosystem. The initial release focuses on Book CRUD, search, authentication, and authorization while keeping migration paths open for AWS-native, serverless components.

## Core Components
- **API Gateway** (Kong, Envoy, or AWS API Gateway): Terminates TLS, validates JWTs, enforces rate limits, performs header negotiation (including `Accept`-based versions), and routes traffic.
- **Book Catalog Service** (Spring Boot, stateless containers on ECS/EKS/Fargate or Docker Compose):
  - Owns the canonical book model and synchronous REST APIs for CRUD and search.
  - Structured following hexagonal architecture to separate domain logic from adapters.
  - Emits domain events after successful transactions.
- **Auth Service** (Spring Boot or AWS Cognito):
  - Provides login, token issuance, and user/role management for bookstore staff.
  - Issues JWTs containing role claims (`admin`, `staff`).

## Data Stores
- **Aurora PostgreSQL** (primary store):
  - Tables: `books`, `authors`, `genres`, junction `book_genres`, and `book_history` for audit.
  - Leverages PostgreSQL full-text search (`tsvector`, `GIN`) for title/author/genre queries.
  - Handles all transactional updates with strong consistency.
- **Optional Read Models (future phases)**:
  - **OpenSearch**: for advanced full-text, relevancy tuning, and analytics.
  - **DynamoDB**: denormalized views for ultra-fast, pattern-based lookups.
  - Both populated asynchronously via the event pipeline when scale demands.

## Messaging & Eventing
- **SNS Topic (+ SQS or Kinesis consumers)**:
  - Receives `BookCreated`, `BookUpdated`, `BookDeleted` events emitted by the catalog service post-commit.
  - Downstream consumers (indexers, analytics, notifications) subscribe without coupling to the main service.
- **Indexing Lambda / Service** (future):
  - Listens to book events, updates OpenSearch/DynamoDB read models, enabling eventual consistency and high scalability.

## Authentication & Authorization Flow
1. Staff authenticates via Auth Service / Cognito and receives a JWT.
2. API Gateway/JWT Authorizer validates the token and forwards role claims.
3. Book Catalog Service enforces role-based access:
   - `admin`: full CRUD.
   - `staff`: read-only search endpoints.
4. Spring Security annotations plus a domain-level policy layer guard sensitive operations.

## Secrets & Configuration Management
- **PoC (Docker Compose)**:
  - Use **HashiCorp Vault** in dev mode or **SOPS-encrypted `.env` files** committed to Git (with keys stored securely) to supply database credentials, JWT signing keys, and gateway configuration. Containers load secrets at start via environment variables or mounted files managed by a simple bootstrap script.
  - Alternatively, leverage Docker Secrets (Swarm-compatible) by mounting secrets into services if Compose stack version supports it; this keeps plaintext out of images.
- **Path to AWS Native**:
  - Replace Vault dev mode with **AWS Secrets Manager** or **AWS Parameter Store**. Inject secrets into ECS/EKS tasks via task definitions or Secrets Store CSI driver. Spring Boot services can auto-refresh through the AWS SDK.
  - JWT signing keys stored in **AWS KMS**; gateway uses KMS-backed Secrets Manager entry, enabling rotation via Lambda.
  - Use Terraform or AWS CDK to manage secret definitions, policies, and rotation schedules as code; mirror the Compose bootstrap scripts with IaC modules for environments.
- **Policies**:
  - Strict least privilege for services accessing secrets; separate roles for read vs. rotate.
  - Audit logging of secret access (Vault audit device or AWS CloudTrail).
  - Rotate database credentials and tokens regularly (e.g., every 90 days) with automation pipelines.

## Scalability & Operations
- **Stateless Services**: Horizontal scaling through container orchestration (ECS/EKS). Rolling deployments with blue/green or canary releases.
- **Database Scaling**: Aurora read replicas for reporting; Aurora Serverless v2 as demand grows.
- **Caching Layer**: Optional ElastiCache/Redis for hot search results or session tokens (if not using Cognito).
- **Observability**: Structured logging (JSON), distributed tracing via AWS X-Ray/OpenTelemetry, metrics exported to CloudWatch/Prometheus.

## Development Best Practices
- Hexagonal architecture with domain, application, and adapter layers.
- DTO validation at boundaries (Jakarta Validation).
- Flyway/Liquibase for schema migrations.
- Contract tests for REST APIs; unit tests around domain services; integration tests against containerized Postgres.

## Evolution Path
- **Short Term**: Single service for CRUD/search keeps operational overhead low while delivering required functionality.
- **Medium Term**: Introduce event-driven read models (OpenSearch, DynamoDB) when query load or feature complexity increases.
- **Long Term**: Split search/indexing into dedicated microservices or serverless functions, reusing the established event stream. This unlocks migration to fully serverless CRUD (Lambda + API Gateway + Aurora Serverless) without re-architecting core domain logic.

This architecture balances immediate delivery with a clear runway to high-scale, AWS-native, event-driven operations.
