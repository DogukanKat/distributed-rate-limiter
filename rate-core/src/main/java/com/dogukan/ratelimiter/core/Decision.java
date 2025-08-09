package com.dogukan.ratelimiter.core;

public record Decision(boolean allowed, long remaining, long retryAfterSeconds) {
  public static Decision allow(long remaining) {
    return new Decision(true, remaining, 0);
  }
  public static Decision deny(long retryAfterSeconds, long remaining) {
    return new Decision(false, remaining, Math.max(0, retryAfterSeconds));
  }
}
