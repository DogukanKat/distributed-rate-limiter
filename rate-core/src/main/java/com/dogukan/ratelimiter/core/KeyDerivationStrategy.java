package com.dogukan.ratelimiter.core;

public interface KeyDerivationStrategy {
  String key(RateContext ctx);
}
