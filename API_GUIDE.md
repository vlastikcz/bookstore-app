# Bookstore API Guide

## Accept-Based Versioning
- Clients supply `Accept: application/vnd.vbookstore.catalog+json;version=1` (catalog) or `Accept: application/vnd.vbookstore.auth+json;version=1`.
- Requests with bodies mirror the version in `Content-Type`.
- Unknown or unsupported versions return `406 Not Acceptable` with an `application/problem+json` payload.

## Media Types & Character Encoding
- All representations are UTF-8 JSON.
- Naming uses `snake_case`; timestamps follow RFC 3339 UTC.
- Money objects always use `{ "amount": number, "currency": "ISO-4217" }`.

## Conditional Requests & Idempotency
- Every representation includes an `ETag` header (derived form the ID and version number).
- Mutating requests require optimistic locking:
  - `PUT`/`PATCH`/`DELETE` demand `If-Match` with the latest `ETag`.
  - Creation with client-supplied IDs and idempotency uses `PUT /resource/{id}` + `If-None-Match: *`.
- Read endpoints support `If-None-Match`; return `304 Not Modified` when appropriate.

## Errors
- Errors follow RFC 7807 (`application/problem+json`) with fields: `type`, `title`, `status`, `detail`, `instance`, optional `code`, `violations`.
- Validation errors aggregate field-level details in `violations`.

## Common Headers
- `traceparent` (W3C Trace Context) propagated across services.
- `RateLimit-Limit` exposed on throttled endpoints (standard IETF header). Remaining/reset values may be introduced later.

## Pagination & Filtering
- `page[number]` (default `1`), `page[size]` (default `20`, max `100`).
- Filtering and sparse fieldsets follow Zalando grammar: `filter[title]=`, `filter[price][lt]=`, `fields=(title,price(amount,currency))`.
- Sorting uses comma-separated list; prefix with `-` for descending (e.g., `sort=title,-published_at`).

## Book Catalog Service

### Resources
- `Book`, `Author`: canonical entity stored in Aurora PostgreSQL and indexed for search.
- `Genre`: supporting read-only collections for UI picklists.

### Endpoints

See [libs/api-contract/src/main/resources/openapi/catalog-service-api.yaml](libs/api-contract/src/main/resources/openapi/catalog-service-api.yaml) for the canonical contract. Highlights:

- `/api/books` (`GET`) – paginated listing with optional `embed=authors` and RFC 7232 conditional headers.
- `/api/books/{book_id}` (`PUT`/`GET`/`PATCH`/`DELETE`) – optimistic locking via `If-None-Match: *` for create, `If-Match` for updates, and JSON Merge Patch for partial changes.
- `/api/book-search` (`GET`) – full-text search with relevancy, sorting, and `filter[title|author|genres]` selectors.
- `/api/authors` (`GET`) and `/api/authors/{author_id}` (`PUT`/`GET`/`PATCH`/`DELETE`) – manage author metadata with the same concurrency and validation semantics as books.

### Request/Response Patterns
- Responses wrap data in `{ "content": [], "meta": { ... } }`.
- Search response embed `_links` with `self` with the resource URL.
- `PATCH` accepts `application/merge-patch+json`.
- All mutation endpoints return updated representation with new `ETag` except `DELETE` which returns `204`.
