# V-Bookstore API Guide

## Accept-Based Versioning
- Clients supply `Accept: application/vnd.vbookstore.catalog+json;version=1` (catalog) or `Accept: application/vnd.vbookstore.auth+json;version=1`.
- Requests with bodies mirror the version in `Content-Type`.
- Unknown or unsupported versions return `406 Not Acceptable` with an `application/problem+json` payload.

## Media Types & Character Encoding
- All representations are UTF-8 JSON.
- Naming uses `snake_case`; timestamps follow RFC 3339 UTC.
- Money objects always use `{ "amount": number, "currency": "ISO-4217" }`.

## Conditional Requests & Idempotency
- Every representation includes an `ETag` header (hash of a canonical JSON or `updated_at`).
- Mutating requests require optimistic locking:
  - `PUT`/`PATCH`/`DELETE` demand `If-Match` with the latest `ETag`.
  - Creation with client-supplied IDs and idempotency uses `PUT /resource/{id}` + `If-None-Match: *`.
- Read endpoints support `If-None-Match`; return `304 Not Modified` when appropriate.

## Errors
- Errors follow RFC 7807 (`application/problem+json`) with fields: `type`, `title`, `status`, `detail`, `instance`, optional `code`, `violations`.
- Validation errors aggregate field-level details in `violations`.

## Common Headers
- `Trace-Id`, `Correlation-Id` propagated across services.
- `RateLimit-Limit`, `RateLimit-Remaining`, `RateLimit-Reset` exposed on throttled endpoints.
- `Sunset` and `Deprecation` headers announce breaking changes.
- Pagination uses `Link` header with `rel="next"|"prev"|"first"|"last"`.

## Pagination & Filtering
- `page[number]` (default `1`), `page[size]` (default `20`, max `100`).
- Filtering and sparse fieldsets follow Zalando grammar: `filter[title]=`, `filter[price][lt]=`, `fields=(title,price(amount,currency))`.
- Sorting uses comma-separated list; prefix with `-` for descending (e.g., `sort=title,-published_at`).

## Book Catalog Service

### Resources
- `Book`: canonical entity stored in Aurora PostgreSQL and indexed for search.
- `Author`, `Genre`: supporting read-only collections for UI picklists.

### Endpoints

| Method | Path | Description | Notes |
|--------|------|-------------|-------|
| `PUT` | `/books/{book_id}` | Idempotent create or full replace | Requires client UUID, `If-None-Match: *` for creation or `If-Match` for replace |
| `GET` | `/books/{book_id}` | Retrieve a single book | Supports `fields`, `include=authors,genres`, conditional GET |
| `PATCH` | `/books/{book_id}` | Partial update (JSON Merge Patch) | Requires `If-Match`; emits domain event on success |
| `DELETE` | `/books/{book_id}` | Delete book | Requires `If-Match`; returns `204` |
| `GET` | `/books` | Search/list books | Filtering, sorting, pagination, optional includes |
| `PUT` | `/books/{book_id}/inventory` | Replace inventory snapshot | Separate `ETag` per inventory representation |
| `GET` | `/authors` | List authors | Read-only, paginated |
| `GET` | `/genres` | List genres | Read-only, paginated |

### Request/Response Patterns
- Search responses wrap data in `{ "data": [], "meta": { ... }, "links": { ... } }`.
- Collection items embed `_links` with `self`, related resource URIs.
- `PATCH` accepts `application/merge-patch+json`.
- All mutation endpoints return updated representation with new `ETag` except `DELETE` which returns `204`.

## Authentication Service

### Resources
- `User`: staff identity with role assignments (`ADMIN`, `STAFF`).
- `Token`: access and refresh tokens in JWT format.

### Endpoints

| Method | Path | Description | Notes |
|--------|------|-------------|-------|
| `POST` | `/auth/tokens` | Password grant to issue access/refresh token pair | Responds `201`; rate limited |
| `POST` | `/auth/tokens/refresh` | Refresh access token | Validates refresh token validity |
| `POST` | `/auth/users/{user_id}` | Idempotent user creation | Admin only; client provides UUID with `If-None-Match: *` |
| `GET` | `/auth/users/{user_id}` | Fetch user metadata | Requires proper scopes, supports conditional GET |
| `PATCH` | `/auth/users/{user_id}` | Partial update (roles, status, display name) | Requires `If-Match`; admin only |
| `POST` | `/auth/users/{user_id}/credentials/reset` | Initiate credential reset | Returns `202`; triggers async workflow |
| `GET` | `/auth/roles` | List available role descriptors | Read-only |

### Security Model
- Access tokens include scopes (`catalog:read`, `catalog:write`, `auth:manage`).
- API Gateway validates JWT, injects `scope` and `role` claims into forwarded headers.
- Service-level RBAC via Spring Security annotations aligned with scopes.

## Evolution Strategy
- Backward-compatible enhancements: new optional fields, additional links, new query params.
- Breaking changes require new media type version (e.g., `version=2`) while keeping older versions until deprecation period lapses.
- Event-driven read models (OpenSearch, DynamoDB) can be introduced without altering API contract.
- Maintain changelog aligned with versioned media types; expose upcoming removals via `Link` pointing to documentation.

## Observability
- Structured JSON logging with `Trace-Id`, `Correlation-Id`, `user_id` (if available).
- Emit metrics per endpoint (`http.server.requests`), include `Accept` version label.
- Distributed tracing integrated with API Gateway/X-Ray.

## Testing Expectations
- Contract tests verifying Accept header negotiation, ETag enforcement, and error payloads.
- Integration tests for conditional requests and optimistic locking failure scenarios.
- Load tests for search endpoint with realistic pagination patterns.

