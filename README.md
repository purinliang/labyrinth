# Labyrinth

A scientific research platform for exploring hyperparameter spaces through grid search and live visualization.

Researchers submit a Python script and a hyperparameter grid. Labyrinth runs one trial per parameter combination, tracks progress through a Java Spring backend, and pushes live updates to a web frontend for monitoring and result comparison.

## Planned workflow

1. Submit a Python script and hyperparameter values.
2. Expand the grid into trials and execute them in isolated Python workers.
3. Store trial parameters, status, logs, and metrics; stream progress to the frontend.
4. When the experiment finishes, publish a completion event through RabbitMQ.
5. Future downstream services consume that event to send email or other notifications.

## Architecture

![Labyrinth Architecture](docs/assets/architecture.png)

## Planned project structure

```text
labyrinth/
├── frontend/                  # React + TypeScript
├── backend/                   # Java + Spring Boot
├── worker/                    # Python
├── examples/                  # Research programs
├── contracts/                 # Shared schemas
│
├── infra/
│   ├── postgres/
│   ├── rabbitmq/
│   └── docker/
│
├── docs/
├── compose.yaml
├── .env.example
├── AGENTS.md
└── README.md
```

## Status

The project is in early development. The Spring Boot backend provides
greeting endpoints and experiment creation and lookup in memory.
Local PostgreSQL is available through Docker Compose. The backend
connects to PostgreSQL and applies Flyway schema migrations on startup.
Database-backed experiment storage, execution, the frontend, workers,
and RabbitMQ are still planned.

## Local PostgreSQL

Requires Docker with Docker Compose. From the repository root:

```sh
cp .env.example .env
```

Set `POSTGRES_PASSWORD` in `.env` to your local development password.
If port 5432 is occupied, change `POSTGRES_PORT` in the same file.

```sh
docker compose up -d --wait postgres
```

`POSTGRES_DB` and `POSTGRES_USER` default to `labyrinth` in the example.
PostgreSQL is available on `127.0.0.1`, using `POSTGRES_PORT` (5432 in
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
connects to PostgreSQL, but experiment records still use the in-memory
repository.

## Run the backend

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

Check the endpoint:

```sh
curl http://localhost:8080/api/hello
```

Expected response: `{"message":"Hello, Labyrinth!"}`.

To provide a name, use `GET /api/hello?name=Alice`,
`GET /api/hello/Alice`, or `POST /api/hello` with a JSON body such as
`{"name":"Alice"}` and `Content-Type: application/json`. Request examples
are available in [hello.http](backend/requests/hello.http).

Build and run available tests with `mvn -f backend/pom.xml verify`.

POST greetings require a nonblank `name`. Missing, null, or blank names
return HTTP 400 with `{"message":"Invalid argument"}`.

Spring Boot DevTools is enabled for local development and is excluded
from the packaged application.

## Experiment API

`POST /api/experiments` accepts a JSON body such as `{"name":"Demo"}`.
It returns HTTP 201 with an ID, name, `PENDING` status, and a `Location`
header pointing to `GET /api/experiments/{id}`. Lookup returns HTTP 200
for an existing experiment or HTTP 404 for an unknown ID. Missing,
null, or blank names return HTTP 400.

Experiments are currently stored in memory and are lost on restart.
Execution and persistent storage are still planned. Request examples
are in [experiments.http](backend/requests/experiments.http).

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

## Automated tests

```sh
mvn -f backend/pom.xml verify
```

The API tests use Spring Boot and MockMvc with the real application
components, without opening a network port. Their `test` profile disables
JDBC and Flyway auto-configuration, so PostgreSQL and database credentials
are not required for `mvn verify`. They cover experiment
creation and lookup, the `Location` header with a context path, invalid
names and IDs, malformed JSON, unknown IDs, and greeting regressions.
They use returned IDs rather than assuming test execution order.

The repository concurrency test starts eight workers together and
checks that all 800 experiments have unique IDs and remain retrievable.
It uses bounded waits and shuts down its executor after the test.

For a faster test-only run, use `mvn -f backend/pom.xml test`.
