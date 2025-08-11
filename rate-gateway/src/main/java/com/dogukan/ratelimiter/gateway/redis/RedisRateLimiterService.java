package com.dogukan.ratelimiter.gateway.redis;

import com.dogukan.ratelimiter.core.*;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

@Service
public class RedisRateLimiterService implements RateLimiterService {

  private final ReactiveStringRedisTemplate redis;
  private final DefaultRedisScript<List> tokenBucketScript;
  private final KeyDerivationStrategy keyDerivation = new DefaultKeyDerivation();
  private final Clock clock = Clock.systemUTC();

  public RedisRateLimiterService(ReactiveStringRedisTemplate redis,
                                 DefaultRedisScript<List> tokenBucketScript) {
    this.redis = redis;
    this.tokenBucketScript = tokenBucketScript;
  }

  @Override
  public Mono<Decision> check(RateContext ctx) {
    String key = keyDerivation.key(ctx);
    Long nowMillis = clock.millis();
    List<String> keys = List.of(key);
    Object[] argv = {
      String.valueOf(ctx.capacity()),
      String.valueOf(ctx.refillPerSec()),
      String.valueOf(nowMillis),
      String.valueOf(Math.max(1, ctx.cost())),
      String.valueOf(ctx.ttlSeconds())
    };

    return redis.execute(tokenBucketScript, keys, Arrays.asList(argv))
      .next()
      .map(res -> {
        long allowed = ((Number) res.get(0)).longValue();
        long remaining = ((Number) res.get(1)).longValue();
        long retryAfter = ((Number) res.get(2)).longValue();
        return allowed == 1 ? Decision.allow(remaining)
          : Decision.deny(retryAfter, remaining);
      });
  }
}
