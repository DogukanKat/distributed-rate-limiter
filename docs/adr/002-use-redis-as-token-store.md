# ADR 002: Use Redis as Token Store

## Status
Accepted

## Context
To support distributed rate limiting, we need a shared, low-latency, and scalable storage system to keep track of token counts per user or IP. The system must allow fast reads/writes, TTL support, and atomic operations.

We considered the following options:
- In-memory (local cache)
- Relational Database (PostgreSQL/MySQL)
- Redis

## Decision
We chose **Redis** for the following reasons:

- Provides fast in-memory access with sub-millisecond latency
- Native support for key expiration (TTL), ideal for token bucket refill windows
- Supports atomic operations via Lua
