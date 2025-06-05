# ADR 003: Role-Based and Route-Aware Rate Limiting

## Status

Accepted

## Context

The current system applies the same token bucket rate limit to every user and every endpoint. In a production
environment, we need:

* Different rate limits for different user roles (e.g., “basic”, “premium”, “admin”).
* The ability to configure per-endpoint (route) overrides.
* Easy extensibility to add new roles or endpoints without code changes.

## Decision

1. Introduce a `RateLimiterProperties` class annotated with `@ConfigurationProperties(prefix = "rate-limiter")` to bind
   configuration from `application.yml`. It will contain two maps:

    * `roles`: maps each role name (e.g., “basic”, “premium”, “admin”) to a `RateLimitConfig` object containing
      `maxTokens` and `refillIntervalMs`.
    * `routes`: maps each endpoint path (e.g., `/api/request`, `/api/checkout`) to another map that associates role
      names with their own `RateLimitConfig`.

2. Create a `RateLimitConfig` DTO with two fields:

   ```java
   public class RateLimitConfig {
       private long maxTokens;
       private long refillIntervalMs;
   }
   ```

3. Implement a `RateLimitPolicyResolver` component that takes `(role, route)` as input and returns the appropriate
   `RateLimitConfig`. It will:

    * First check for a route-specific override in `routes[route][role]`.
    * If none exists, fall back to `roles[role]`.
    * If the role is not found, fall back to the “basic” role’s default configuration.

4. Update `TokenBucketRateLimiter.consume(...)` to accept `(userId, role, route)` parameters. Inside `consume`, call
   `policyResolver.resolve(role, route)` to obtain `maxTokens` and `refillIntervalMs`, then pass those values to the
   existing Lua-based token bucket logic.

5. In `application.yml`, configure defaults under `rate-limiter.roles` and any per-route overrides under
   `rate-limiter.routes`. For example:

   ```yaml
   rate-limiter:
     roles:
       basic:
         max-tokens: 10
         refill-interval-ms: 60000
       premium:
         max-tokens: 50
         refill-interval-ms: 60000
       admin:
         max-tokens: 1000
         refill-interval-ms: 60000

     routes:
       /api/request:
         basic:
           max-tokens: 10
           refill-interval-ms: 60000
         premium:
           max-tokens: 50
           refill-interval-ms: 60000

       /api/checkout:
         basic:
           max-tokens: 5
           refill-interval-ms: 60000
         premium:
           max-tokens: 30
           refill-interval-ms: 60000
   ```

## Consequences

**Positive:**

* Flexible: New roles or endpoints can be added via configuration only.
* Matches production requirements: Different SLAs for different user tiers and critical endpoints.

**Negative:**

* Configuration becomes more complex and verbose.
* Unit and integration tests need to account for per-role and per-route overrides.

## Alternatives Considered

### Single (Fixed) Limit for All

* ✅ Simplest to implement.
* ❌ Lacks flexibility: cannot differentiate between user tiers or endpoints.

### Database-Backed Dynamic Config

* ✅ Can update limits at runtime without redeploying the application.
* ❌ Requires additional infrastructure (database) and adds complexity.

### In-Memory Policy Store

* ✅ Fast lookups.
* ❌ Not shared across multiple instances; cannot scale in a distributed environment.
