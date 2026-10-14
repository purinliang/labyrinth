# Development guidance

Labyrinth is a scientific research platform with a Java Spring Boot backend, a React + TypeScript frontend, Python workers, PostgreSQL, and RabbitMQ. Researchers submit Python scripts and hyperparameter grids, monitor execution live, and visualize results.

- Treat an experiment as a grid search and a trial as one parameter combination.
- Keep orchestration in the backend and Python execution in isolated workers.
- Persist trial parameters, status, metrics, and failures so results are reproducible and progress survives reconnects.
- Push live progress to the frontend and publish an experiment completion event through RabbitMQ for future consumers.
- Keep notification consumers independent of experiment execution.
- Keep changes focused, document setup when adding components, and run relevant checks when available.
- The backend has JPA creation and lookup for experiments, teams, users, tickets, and comments backed by PostgreSQL, with Flyway migrations. Execution, the frontend, workers, and RabbitMQ remain planned. Do not describe planned features as implemented.
- The user develops the source code. Help with review, formatting, and commits when requested; do not implement features unless explicitly asked.

## Java style

- Use Spotless with the shared Eclipse profile and Checkstyle; keep Java lines
  below 80 columns.
- Put a null check directly after the lookup or service call, without a blank
  line between them.
- Services return null for missing records or required references; controllers
  translate that result into HTTP 404. Document nullable service results.
- Keep services focused on create/get operations until additional operations
  are requested.
