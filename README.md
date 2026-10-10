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

The project is in the design stage; this repository currently contains documentation and directory placeholders. The structure above describes the planned layout, using React + TypeScript with Vite, Java + Spring Boot, Python workers, PostgreSQL, and RabbitMQ.
