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

