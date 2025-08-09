package com.dogukan.ratelimiter.core;

import reactor.core.publisher.Mono;

public interface RateLimiterService {
  Mono<Decision> check(RateContext ctx);
}
