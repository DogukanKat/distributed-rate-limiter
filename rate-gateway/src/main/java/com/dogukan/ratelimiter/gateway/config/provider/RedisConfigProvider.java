package com.dogukan.ratelimiter.gateway.config.provider;

import com.dogukan.ratelimiter.config.ConfigProvider;
import com.dogukan.ratelimiter.config.InMemoryProvider;
import com.dogukan.ratelimiter.config.model.LimitPlan;
import com.dogukan.ratelimiter.config.model.RouteLimit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed config:
 *  - Key:   rl:cfg:{<tenant>}
 *  - Type:  Hash
 *  - Field: "<METHOD> <routePrefix>"  (METHOD can be "*" for any)
 *  - Value: "capacity:refillPerSec:ttlSeconds"  e.g. "10:2:600"
 *
 * Longest-prefix match is applied per tenant.
 * Rules are cached in-process for short TTL to avoid per-request Redis round trips.
 *
 * NOTE: resolve(...) is sync (per current interface). We do a short, cached block
 * with small timeout when loading tenant rules. This can be improved later
 * by making ConfigProvider reactive or warming cache out-of-band.
 */
@Primary
@Component
public class RedisConfigProvider implements ConfigProvider {

  private final ReactiveStringRedisTemplate redis;
  private final InMemoryProvider fallback;

  private final Cache<String, List<RouteLimit>> tenantRulesCache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(2, TimeUnit.SECONDS)
    .build();

  private final LimitPlan defaultPlan = LimitPlan.of(100, 50, 3600);
  private final String keyPrefix = "rl:cfg:";

  public RedisConfigProvider(ReactiveStringRedisTemplate redis,
                             InMemoryProvider fallback) {
    this.redis = redis;
    this.fallback = fallback;
  }

  @Override
  public LimitPlan resolve(String tenantId, String path, String method) {
    String tenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
    String cacheKey = tenant;
    List<RouteLimit> rules = tenantRulesCache.getIfPresent(cacheKey);
    if (rules == null) {
      rules = loadRulesForTenant(tenant);
      if (rules == null) {
        return fallback.resolve(tenant, path, method);
      }
      tenantRulesCache.put(cacheKey, rules);
    }
    // Longest prefix match
    RouteLimit best = null;
    int bestLen = -1;
    for (RouteLimit rl : rules) {
      if (rl.matches(path, method)) {
        int len = rl.routePrefix().length();
        if (len > bestLen) { best = rl; bestLen = len; }
      }
    }
    return best != null ? best.plan() : defaultPlan;
  }

  private List<RouteLimit> loadRulesForTenant(String tenant) {
    String key = keyPrefix + "{" + tenant + "}";
    try {
      Map<Object, Object> map = redis.opsForHash()
        .entries(key)
        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
        .block(Duration.ofMillis(50)); // short block, cached; can be tuned

      if (map == null || map.isEmpty()) return Collections.emptyList();

      List<RouteLimit> list = new ArrayList<>(map.size());
      for (Map.Entry<Object, Object> e : map.entrySet()) {
        String field = String.valueOf(e.getKey());         // "<METHOD> <prefix>"
        String value = String.valueOf(e.getValue());       // "cap:refill:ttl"
        String[] parts = field.split("\\s+", 2);
        String method = parts.length > 0 ? parts[0] : "*";
        String prefix = parts.length > 1 ? parts[1] : "/";
        String[] vals = value.split(":");
        long cap = Long.parseLong(vals[0]);
        long refill = Long.parseLong(vals[1]);
        long ttl = Long.parseLong(vals[2]);
        list.add(new RouteLimit(prefix, "*".equals(method) ? null : method, LimitPlan.of(cap, refill, ttl)));
      }
      // Longest prefix first (optional)
      list.sort(Comparator.comparingInt((RouteLimit r) -> r.routePrefix().length()).reversed());
      return list;
    } catch (Exception ex) {
      // On error, signal caller to use fallback
      return null;
    }
  }
}
