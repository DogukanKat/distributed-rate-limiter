package com.dogukan.ratelimiter.gateway.fake;

import com.dogukan.ratelimiter.core.Decision;
import com.dogukan.ratelimiter.core.RateContext;
import com.dogukan.ratelimiter.core.RateLimiterService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FakeRateLimiterService implements RateLimiterService {
  @Override
  public Mono<Decision> check(RateContext ctx) {
    // Her zaman izin ver
    return Mono.just(Decision.allow(Long.MAX_VALUE));
  }
}
