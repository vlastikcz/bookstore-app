# BOOKSTORE-APP

Principles - demo application ready for rapid growth. Simple and minimal, but open for simple extension.

Book Catalog service - uses Postgresql for storage and the fulltext search capabilities. This should fully fullfill the current requirements. It can run in many instances if needed, or required for uninterrupted deployment. 

If the indexing or search becomes a bottle neck, a separate search service can be implemented. Such service could be implemented for example with OpenSearch, and consume Catalog events (for example via SNS). Intial sync could be done with a batch job.

If separate indexing service would exist, cost saving could be considered by moving the primary storage from PostgreSQL to some schemaless db, such as DynamoDB. But this would be a bigger change - but a tradeoff between initial complexity and costs by inclusing separate indexing service from the start, vs leaving it out and adding only if/when needed.

The Catalog is ready for publishing Event also for other purposes or if new requirements arise.

An API gateway was implemented using **Envoy** to allow easier service management and development. It handles public API routing, applies rate limiting, validates JWTs against Keycloak, and keeps room for advanced migrations (for example API versioning based on the `Accept` header), while the catalog service still performs its own validation for defense in depth.

Authentication was implemented using Keycloak.

Secret management was implemented naively - in production it should use Consul or similar.


Sample account setup is defined in infra/compose/keycloak/bookstore-realm.json.

Demo accounts:
admin/Admin
staff/staff

## Local development

1. Start the infrastructure dependencies you need, e.g. `docker compose -f infra/compose/docker-compose.yaml up postgres` (and optionally add `envoy keycloak` if you want to exercise the full stack).
2. Launch the catalog service with `make run-local`. The `local` Spring profile points the service at `localhost` PostgreSQL and enables an in-process HS256 JWT issuer/decoder so Keycloak is not required for inner-loop work.
3. On startup the service logs ready-to-use Bearer tokens for `admin` and `staff` roles. Copy the value after `Authorization: Bearer ...` and add it to Postman/curl requests.
4. When routing through Envoy, keep sending the `Accept: application/vnd.bookstore.v1+json` header—Envoy matches that media type, validates JWTs via Keycloak’s JWKS, applies local rate limiting (100 req/min), and forwards the request downstream with an `X-Request-Id` header.

### Gateway

- The Envoy static configuration lives in `infra/gateway/envoy.yaml`. It defines listeners, JWT authn, local rate limiting, header-based routing (on `Accept`), and upstream clusters for the catalog service and Keycloak.
- Envoy runs in DB-less mode. To apply configuration changes run `docker compose -f infra/compose/docker-compose.yaml restart envoy`.
- The admin interface is exposed at `http://localhost:9901` (plain HTML/JSON). Use it to inspect stats, config dumps, or to trigger drains; secure/disable it outside local development.
- Tokens are validated against Keycloak’s JWKS endpoint (`http://keycloak:8080/realms/bookstore/protocol/openid-connect/certs`). The catalog service still performs JWT validation so requests remain protected even if Envoy is bypassed.

### Search

- PostgreSQL full-text search powers the catalog filters. Title, author, and genre inputs are converted to `websearch_to_tsquery` expressions (think Google-style search syntax) against weighted `to_tsvector` documents.
- `db/migration/V2__add_fulltext_indexes.sql` creates a combined GIN index using the `simple` dictionary (language-agnostic) plus trigram indexes for fuzzy matching.
- Ranking uses `ts_rank_cd` with field weighting and defaults to score-desc, title-asc ordering. Clients can request alternative sorting via the usual pageable `sort` parameter; a special `score` key exposes the relevance rank.
- The dictionary can be overridden with `catalog.search.fts-config` if you need language-specific stemming (for example `english`, `czech`, etc.).
