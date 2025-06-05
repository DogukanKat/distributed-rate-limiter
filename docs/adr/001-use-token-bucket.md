# ADR 001: Use Token Bucket Algorithm

## Status
Accepted

## Context
We need to implement rate limiting in a distributed environment to prevent abuse and ensure fair usage of our API. The algorithm should support burst traffic, be easy to reason about, and allow flexibility in configuration.

We considered the following algorithms:
- Fixed Window
- Sliding Window
- Leaky Bucket
- Token Bucket

## Decision
We chose the **Token Bucket** algorithm because:

- It allows burst traffic within capacity limits.
- It is simple to implement and understand.
- It supports flexible configuration (capacity, refill rate).
- It works well with Redis as a backing store for token counts.

## Consequences
- We must track token state per user or per IP.
- We'll need a background refill mechanism or calculate refill at runtime.
- If not implemented carefully, concurrency issues may occur — we'll mitigate this by using Lua scripts in Redis for atomicity.

## Alternatives Considered

### Fixed Window
- ✅ Easy to implement
- ❌ Not burst-tolerant
- ❌ Can create "thundering herd" issues

### Sliding Window
- ✅ Smooth rate limiting
- ❌ Requires more storage and computation
- ❌ Harder to implement in Redis without advanced data structures

### Leaky Bucket
- ✅ Good for smoothing traffic
- ❌ Constant refill rate; less flexible
- ❌ Less intuitive configuration

## Notes
This ADR will be revisited if performance issues arise under extreme load or if business requirements change (e.g., needing dynamic policy updates or adaptive throttling).
