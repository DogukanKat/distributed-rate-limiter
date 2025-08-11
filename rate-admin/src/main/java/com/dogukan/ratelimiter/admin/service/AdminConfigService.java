package com.dogukan.ratelimiter.admin.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;

@Service
public class AdminConfigService {

  private final ReactiveStringRedisTemplate redis;
  private final ReactiveRedisConnectionFactory cf;

  public AdminConfigService(ReactiveStringRedisTemplate redis, ReactiveRedisConnectionFactory cf) {
    this.redis = redis;
    this.cf = cf;
  }

  private String key(String tenant) { return "rl:cfg:{" + tenant + "}"; }
  private String ev(String tenant) { return "rl:cfg:ev:{" + tenant + "}"; }

  public Mono<Boolean> upsertRule(String tenant, String method, String prefix,
                                  long capacity, long refillPerSec, long ttlSeconds) {
    String field = method + " " + prefix;
    String value = capacity + ":" + refillPerSec + ":" + ttlSeconds;
    return redis.opsForHash().put(key(tenant), field, value)
      .flatMap(ok -> publishEv(tenant).thenReturn(ok));
  }

  public Mono<Long> deleteRule(String tenant, String method, String prefix) {
    String field = method + " " + prefix;
    return redis.opsForHash().remove(key(tenant), field)
      .flatMap(cnt -> publishEv(tenant).thenReturn(cnt));
  }

  public Flux<Map.Entry<Object,Object>> listRaw(String tenant) {
    return redis.opsForHash().entries(key(tenant));
  }

  private Mono<Long> publishEv(String tenant) {
    // payload önemli değil; invalidation için ping
    return redis.convertAndSend(ev(tenant), "invalidate");
  }
}
