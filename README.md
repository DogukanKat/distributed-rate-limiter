Distributed Rate Limiter

A high-performance, distributed, token-bucket-based rate limiter built with Java 17, Spring Boot 3.3.x, WebFlux, Redis, and Kafka (Redpanda).
Designed for multi-tenant, microservices environments with dynamic configuration and production-grade scalability.

✨ Features

Reactive & Non-blocking: Powered by Spring WebFlux and reactive Redis driver.

Token Bucket Algorithm: Configurable capacity, refill rate, and TTL per route.

Multi-Tenant Support: Separate quotas per tenant/user.

Dynamic Configuration: Rules fetched from Redis at runtime with Caffeine in-memory caching.

Longest Prefix Matching: Route-level granularity.

Fallback Config: In-memory defaults when Redis is unavailable.

Extensible Architecture: Plug in new ConfigProvider implementations (DB, REST API, etc.).

Dockerized Local Stack: Redis & Kafka (Redpanda) with docker-compose.

📦 Modules

* rate-common – Shared DTOs, constants, and utilities
* rate-core – Core rate limiting interfaces & algorithms
* rate-config – ConfigProvider abstractions & in-memory implementation
* rate-gateway – Spring Boot WebFlux API Gateway with RateLimiterFilter
* rate-admin – (Planned) Admin UI / API for managing configs

🛠 Tech Stack

Language: Java 17

Framework: Spring Boot 3.3.x, Spring WebFlux

Caching: Caffeine

Data Store: Redis (reactive)

Messaging: Kafka (Redpanda)

Build Tool: Gradle (Kotlin DSL)

Container: Docker & docker-compose

Testing: JUnit 5, Testcontainers, k6 (load testing)

🚀 Getting Started

Clone & Build

```
git clone https://github.com/<your-username>/distributed-rate-limiter.git
cd distributed-rate-limiter
./gradlew build
```
Start Local Infra
```
docker compose up -d
```

Services:
```
Redis: localhost:6379

Kafka (Redpanda): localhost:9092
```
Run Gateway
```
./gradlew :rate-gateway:bootRun
```
⚙️ Configuration in Redis

Key format:

```
rl:cfg:{<tenantId>}

Field: <METHOD> <routePrefix> (use * for wildcard method)
Value: capacity:refillPerSec:ttlSeconds
```
Example:
```
# default tenant — GET /hello: 20 cap, 5/s refill, 600s TTL
HSET 'rl:cfg:{default}' 'GET /hello' '20:5:600'

# acme tenant — GET /hello: 10 cap, 2/s refill
HSET 'rl:cfg:{acme}' 'GET /hello' '10:2:600'

# acme tenant — wildcard method, /api/payments prefix
HSET 'rl:cfg:{acme}' '* /api/payments' '50:10:600'
```
📊 Load Testing with k6

Install (macOS):

```
brew install k6
```
Example load-test.js:

```
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
stages: [
{ duration: '10s', target: 50 }, // burst
{ duration: '1m', target: 50 },  // steady
],
};

export default function () {
let res = http.get('http://localhost:8080/hello', {
headers: { 'X-Tenant-Id': 'acme', 'X-User-Id': 'u1' }
});
check(res, { 'status is 200 or 429': (r) => r.status === 200 || r.status === 429 });
sleep(0.1);
}
```
