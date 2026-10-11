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

The project is in early development. The backend has a Spring Boot application with greeting endpoints that accept names through query parameters, path variables, or a JSON body. Experiment execution, the frontend, workers, and infrastructure are still planned.

## Run the backend

Requires Java 21 and Maven.

```sh
mvn -f backend/pom.xml spring-boot:run
```

Check the endpoint:

```sh
curl http://localhost:8080/api/hello
```

Expected response: `{"message":"Hello, Labyrinth!"}`.

To provide a name, use `GET /api/hello?name=Alice`, `GET /api/hello/Alice`, or `POST /api/hello` with a JSON body such as `{"name":"Alice"}` and `Content-Type: application/json`. Request examples are available in [backend/request.http](backend/request.http).

Build and run available tests with `mvn -f backend/pom.xml verify`.
