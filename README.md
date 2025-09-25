# BOOKSTORE-APP

Principles - demo application ready for rapid growth. Simple and minimal, but open for simple extension.

Book Catalog service - uses Postgresql for storage and the fulltext search capabilities. This should fully fullfill the current requirements. It can run in many instances if needed, or required for uninterrupted deployment. 

If the indexing or search becomes a bottle neck, a separate search service can be implemented. Such service could be implemented for example with OpenSearch, and consume Catalog events (for example via SNS). Intial sync could be done with a batch job.

If separate indexing service would exist, cost saving could be considered by moving the primary storage from PostgreSQL to some schemaless db, such as DynamoDB. But this would be a bigger change - but a tradeoff between initial complexity and costs by inclusing separate indexing service from the start, vs leaving it out and adding only if/when needed.

The Catalog is ready for publishing Event also for other purposes or if new requirements arise.

An API Gateway was implemented using Traefik to allow easier service management and developemnt. It handle the public API routing, verifies JWT, and allows advanced migrations (for example API versioning based on Accept header).

Authentication was implemented using Keycloak.

Secret management was implemented naively - in production it should use Consul or similar.


Sample account setup is defined in infra/compose/keycloak/bookstore-realm.json.

Demo accounts:
admin/Admin
staff/staff

## Local development

1. Start the infrastructure dependencies you need, e.g. `docker compose -f infra/compose/docker-compose.yaml up postgres` (and optionally Keycloak/Traefik if you want to exercise the full stack).
2. Launch the catalog service with `make run-local`. The `local` Spring profile points the service at `localhost` PostgreSQL and enables an in-process HS256 JWT issuer/decoder so Keycloak is not required for inner-loop work.
3. On startup the service logs ready-to-use Bearer tokens for `admin` and `staff` roles. Copy the value after `Authorization: Bearer ...` and add it to Postman/curl requests.
4. When routing through Traefik, keep sending the `Accept: application/vnd.bookstore.v1+json` header—Traefik enforces a basic rate limit, while the catalog service now assigns an `X-Request-Id` header for correlation and continues to validate JWTs internally.

### Search

- PostgreSQL full-text search powers the catalog filters. Title, author, and genre inputs are converted to `websearch_to_tsquery` expressions (think Google-style search syntax) against weighted `to_tsvector` documents.
- `db/migration/V2__add_fulltext_indexes.sql` creates a combined GIN index using the `simple` dictionary (language-agnostic) plus trigram indexes for fuzzy matching.
- Ranking uses `ts_rank_cd` with field weighting and defaults to score-desc, title-asc ordering. Clients can request alternative sorting via the usual pageable `sort` parameter; a special `score` key exposes the relevance rank.
- The dictionary can be overridden with `catalog.search.fts-config` if you need language-specific stemming (for example `english`, `czech`, etc.).
