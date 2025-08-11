package com.dogukan.ratelimiter.admin.service;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AdminConfigService {

  private final ReactiveStringRedisTemplate redis;

  public AdminConfigService(ReactiveStringRedisTemplate redis) {
    this.redis = redis;
  }

  private String key(String tenant) {
    return "rl:cfg:{" + tenant + "}";
  }

  public Mono<Boolean> upsertRule(String tenant, String method, String prefix,
                                  long capacity, long refillPerSec, long ttlSeconds) {
    String field = method + " " + prefix;
    String value = capacity + ":" + refillPerSec + ":" + ttlSeconds;
    return redis.opsForHash().put(key(tenant), field, value);
  }

  public Mono<Long> deleteRule(String tenant, String method, String prefix) {
    String field = method + " " + prefix;
    return redis.opsForHash().remove(key(tenant), field);
  }

  public Flux<Map.Entry<Object,Object>> listRaw(String tenant) {
    return redis.opsForHash().entries(key(tenant));
  }
}
