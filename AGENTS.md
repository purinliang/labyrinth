# Development guidance

Labyrinth is a Java Spring Boot backend for managing teams, users,
tickets, and comments, backed by PostgreSQL and Flyway migrations.

- Keep ticket ownership and status rules in the service layer.
- Persist a ticket transfer and its comment in the same transaction.
- Preserve optimistic locking and return HTTP 409 for conflicting updates.
- Keep historical Flyway migrations unchanged; add new versioned migrations
  for schema changes.
- Keep changes focused, document setup when adding components, and run
  relevant checks when available.
- The backend provides creation and lookup for teams, users, tickets, and
  comments, plus ticket closing, reopening, and team transfers.
- The user develops the source code. Help with review, formatting, and
  commits when requested; do not implement features unless explicitly asked.

## Java style

- Use Spotless with the shared Eclipse profile and Checkstyle; keep Java lines
  below 80 columns.
- Put a null check directly after the lookup or service call, without a blank
  line between them.
- Preserve each operation's missing-record behavior and map it to HTTP 404.
  Document nullable service results.
- Keep services focused on the requested operations.
- Declare request and response DTOs as public nested records in their
  controller.
