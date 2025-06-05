# Distributed Rate Limiter

Production-ready distributed rate limiter built with Spring Boot, Redis, and Lua using the Token Bucket algorithm.

## Features

- Token Bucket algorithm with customizable capacity and refill rate
- Redis-backed token storage with atomic Lua scripts
- Global request filtering via Spring Boot
- Role-based and route-aware rate limiting
- Docker + Docker Compose support
- Architecture Decision Records (ADRs) included

## Getting Started

```bash
docker compose up -d         # Start Redis
./gradlew bootRun            # Run the application
```

## Request Example

```bash
curl -H "X-User-Id: user123" -X POST http://localhost:8080/api/request
```

## Project Structure

- `src/` – Application source code
- `docker-compose.yml` – Redis container definition
- `Dockerfile` – Multi-stage application build
- `docs/adr/` – Architectural decision records
- `README.md` – Project documentation

## License

MIT © 2025