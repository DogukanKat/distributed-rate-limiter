package com.dogukan.ratelimiter.core;

import java.util.Objects;

public record RateContext(
  String tenantId,
  String routeId,
  String method,
  String identity,   // userId/ip hash gibi
  int cost,          // default 1
  long capacity,     // bucket capacity / burst
  long refillPerSec, // tokens per sec
  long ttlSeconds    // redis key ttl
) {
  public RateContext {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(routeId, "routeId");
    Objects.requireNonNull(method, "method");
    Objects.requireNonNull(identity, "identity");
    if (cost <= 0) throw new IllegalArgumentException("cost must be > 0");
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
    if (refillPerSec <= 0) throw new IllegalArgumentException("refillPerSec must be > 0");
    if (ttlSeconds <= 0) throw new IllegalArgumentException("ttlSeconds must be > 0");
  }
}
