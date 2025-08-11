package com.dogukan.ratelimiter.config.model;

public record LimitPlan(long capacity, long refillPerSec, long ttlSeconds) {
  public static LimitPlan of(long capacity, long refillPerSec, long ttlSeconds) {
    if (capacity <= 0 || refillPerSec <= 0 || ttlSeconds <= 0) {
      throw new IllegalArgumentException("capacity/refill/ttl must be > 0");
    }
    return new LimitPlan(capacity, refillPerSec, ttlSeconds);
  }
}
