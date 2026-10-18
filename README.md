# Labyrinth

A Spring Boot backend for managing teams, users, tickets, and comments.
Tickets support closing, reopening, and transfers between teams, with
optimistic locking to prevent lost updates. Transfers save the team
change and its comment in one PostgreSQL transaction.

## Project structure

```text
labyrinth/
├── backend/
│   ├── config/                 # Java formatting and lint rules
│   ├── requests/               # HTTP request examples
│   └── src/                    # Application, migrations, and tests
├── frontend/
│   └── android/                # Kotlin Android application
├── compose.yaml
├── .env.example
├── AGENTS.md
└── README.md
```

## Status

The Android starter lives in [`frontend/android`](frontend/android/README.md).
Open that directory as a separate Android Studio project to build and run
the welcome screen.

The project is in early development. The Spring Boot backend provides
creation and lookup for teams, users, tickets, and comments through JPA,
plus ticket status changes and transfers.
Local PostgreSQL is available through Docker Compose. The backend
connects to PostgreSQL and applies Flyway schema migrations on startup.
Records are persisted in PostgreSQL.

## Local PostgreSQL

Requires Docker with Docker Compose. From the repository root:

```sh
cp .env.example .env
```

Set `POSTGRES_PASSWORD` in `.env` to your local development password.
If port 15432 is occupied, change `POSTGRES_PORT` in the same file.

```sh
docker compose up -d --wait postgres
```

`POSTGRES_DB` and `POSTGRES_USER` default to `labyrinth` in the example.
PostgreSQL is available on `127.0.0.1`, using `POSTGRES_PORT` (15432 in
the example). Data is stored in the `postgres_data` Docker volume.
`.env` is ignored by Git.

Open a SQL shell:

```sh
docker compose exec postgres sh -c \
  'exec psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Stop PostgreSQL while keeping its data:

```sh
docker compose down
```

Database credentials initialize a new volume only. Changing `.env`
does not update the password of an existing database. The backend
persists records in PostgreSQL through Spring Data JPA.

## Run with Docker Compose

After setting up `.env`, build and start the backend and PostgreSQL:

```sh
docker compose up -d --build --wait
docker compose logs -f backend
```

Once both services are healthy, use the REST API at
`http://localhost:8080`. The backend connects to `postgres:5432` on the
Compose network. `POSTGRES_PORT` controls the published database port
for host tools; `POSTGRES_HOST` is used when running Java locally.
PostgreSQL must be healthy before the backend starts. The backend image
runs as a non-root user.

The backend health check polls `/actuator/health`. It reports healthy
when the application and its PostgreSQL connection are available.
The endpoint returns a status without exposing component details.
Check container health with `docker compose ps`.

Rebuild after changing backend code with `docker compose up -d --build`.
Stop the stack with `docker compose down`; its database volume is kept.

The multi-stage image build compiles the application and runs Checkstyle
and Spotless checks. It skips Testcontainers tests; run
`mvn -f backend/pom.xml verify` separately with Docker available before
building the image.

## Run the backend locally

Requires Java 21, Maven, and a running PostgreSQL instance. Compose
reads `.env` automatically, but the Java application does not. Use
shell-compatible assignments in `.env`; quote values containing shell
special characters. From the repository root, export its values:

```sh
set -a
. ./.env
set +a
mvn -f backend/pom.xml spring-boot:run
```

`POSTGRES_HOST` is the database host seen by the Java process; use
`localhost` when running the backend on the host. Flyway applies
versioned migrations from `backend/src/main/resources/db/migration`.
Do not edit a migration after applying it to a persistent database;
add a new version instead.

Create a team:

```sh
curl -i http://localhost:8080/api/teams \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo"}'
```

The response is HTTP 201 with the team data and a `Location` header
for retrieving it. See the team API below for details.

Build and run available tests with `mvn -f backend/pom.xml verify`.

Spring Boot DevTools is enabled for local development and is excluded
from the packaged application.

## Team API

`POST /api/teams` accepts a JSON body such as `{"name":"Backend"}`.
It returns HTTP 201 with the team's ID and name, and a `Location` header
pointing to `GET /api/teams/{id}`. Lookup returns HTTP 200 for an existing
team or HTTP 404 for an unknown ID. Missing, null, or blank names return
HTTP 400. Team names are not required to be unique.

Teams are persisted in PostgreSQL. Request examples are available in
[team.http](backend/requests/team.http).

## User API

`POST /api/users` accepts a name and an existing team's ID, for example
`{"name":"Researcher","teamId":1}`. It returns HTTP 201 with the user's
ID, name, and `teamId`, plus a `Location` header for `GET /api/users/{id}`.
Lookup returns HTTP 200 for an existing user or HTTP 404 for an unknown
ID. Creation with an unknown team also returns HTTP 404. Missing, null,
or blank names and missing, null, or nonpositive team IDs return HTTP 400.

Users are persisted in PostgreSQL. Request examples are available in
[user.http](backend/requests/user.http).

## Ticket API

`POST /api/tickets` accepts `title`, `content`, and `currentTeamId`.
It returns HTTP 201 with a `Location` header for `GET /api/tickets/{id}`.
The response includes the ticket's ID, title, content, `OPEN` status,
current team ID, initial version (0), and database-generated `createdAt`.
Lookup returns HTTP 200 or HTTP 404 for an unknown ticket. Creation with
an unknown team returns HTTP 404. Missing, null, or blank title/content
and missing, null, or nonpositive team IDs return HTTP 400.

`POST /api/tickets/{id}/close` and `POST /api/tickets/{id}/reopen`
accept `{"expectedVersion":0}` using the ticket's current nonnegative
version. A successful transition returns HTTP 200 with the updated
status and incremented version. An unknown ticket returns HTTP 404;
a stale version, concurrent update, or already requested status returns
HTTP 409. Missing, null, or negative versions return HTTP 400.

`POST /api/tickets/{id}/transfer` accepts `targetTeamId`, `authorId`,
`comment`, and `expectedVersion`. Only an author belonging to the current
team can transfer an open ticket to a different existing team. The
transfer comment and team assignment commit together; the response is
HTTP 200 with the updated ticket and incremented version.

Missing tickets, authors, or teams return HTTP 404. An author outside
the current team returns HTTP 403. Closed tickets, transfers to the same
team, stale versions, and concurrent updates return HTTP 409. Missing,
null, or nonpositive IDs, blank comments, and missing, null, or negative
versions return HTTP 400. Failed transfers leave no transfer comment
and do not change the ticket.

Request examples are in [ticket.http](backend/requests/ticket.http).

## Comment API

`POST /api/comments` accepts `content`, `authorId`, and `ticketId`.
It returns HTTP 201 with a `Location` header for `GET /api/comments/{id}`.
The response includes the comment's ID, content, author ID, ticket ID,
and database-generated `createdAt`. Lookup returns HTTP 200 or HTTP 404
for an unknown comment. Creation with an unknown author or ticket also
returns HTTP 404. Missing, null, or blank content and missing, null, or
nonpositive author/ticket IDs return HTTP 400.

Request examples are in [comment.http](backend/requests/comment.http).

## Error responses

Services reject missing records or required references by throwing a
`ResponseStatusException` with HTTP 404 and a clear reason. The global
exception handler preserves the status and returns that reason in JSON.
Controllers map successful service results to response DTOs. For example,
a ticket business error can return:

```json
{"message":"Ticket already closed"}
```

Validation failures return `{"message":"Invalid argument"}`. Concurrent
update conflicts return `{"message":"Version conflict"}`. The frontend
can read the `message` field to display these errors.

## Java formatting and lint

Spotless uses the Eclipse JDT formatter with the shared profile in
`backend/config/eclipse-formatter.xml`. It uses four spaces and wraps at
79 columns. It also sorts imports and removes unused imports.

Apply formatting:

```sh
mvn -f backend/pom.xml spotless:apply
```

Check formatting and run Checkstyle without compiling:

```sh
mvn -f backend/pom.xml spotless:check checkstyle:check
```

The lint rules are in `backend/config/checkstyle.xml`. Checkstyle runs
during Maven's `validate` phase; Spotless checks run during `verify`.
Use these commands as the source of truth for formatting.

Put a null check directly after its lookup or service call, with no blank
line in between. The formatter preserves this layout. Service comments
document missing-record exceptions and business rejection rules; comments
on transaction guards explain why those rules must be enforced.

## Automated tests

```sh
mvn -f backend/pom.xml verify
```

The API tests use Spring Boot and MockMvc with the real application
components. Tests require a running Docker daemon accessible to the
current user. Testcontainers starts an isolated PostgreSQL 17 instance
on a random port; no `.env` or existing database is used. Spring manages
the container lifecycle and supplies its connection details to the
test application. Flyway runs the production migrations and Hibernate
validates the resulting schema. API tests cover team, user, ticket, and
comment creation and lookup, `Location` headers with a
context path, invalid request fields and IDs, and missing records or
references. Ticket status and transfer tests also cover malformed JSON.
Health tests check the Actuator endpoint.
They use returned IDs rather than assuming test execution order.

Migration tests check fresh PostgreSQL databases, upgrades from version
2, retained ticket data, and repeat Flyway execution without data loss.
Persistence tests check committed SQL data, reloads through fresh JPA
persistence contexts, and optimistic locking. Ticket and comment
service tests cover creation, lookup, missing references, persisted
relationships, and database-generated timestamps. Ticket status tests
cover close/reopen transitions, committed versions, rejected requests,
and optimistic locking under concurrent transactions. Transfer tests
check team membership, validation, comment persistence, rollback after
writes, and concurrent transfers or status changes.

For a faster test-only run, use `mvn -f backend/pom.xml test`.
