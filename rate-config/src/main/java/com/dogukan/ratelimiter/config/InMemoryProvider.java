package com.dogukan.ratelimiter.config;

import com.dogukan.ratelimiter.config.model.LimitPlan;
import com.dogukan.ratelimiter.config.model.RouteLimit;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory provider with per-tenant route rules.
 * - Longest prefix wins
 * - Caffeine cache to avoid hot-path map scans
 */
public class InMemoryProvider implements ConfigProvider {

  private final Map<String, List<RouteLimit>> rulesByTenant = new ConcurrentHashMap<>();
  private final Cache<String, LimitPlan> cache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(Duration.ofMillis(500)) // short TTL to emulate dynamic reloads
    .build();

  private final LimitPlan defaultPlan = LimitPlan.of(100, 50, 3600);

  public InMemoryProvider() {
    seed();
  }

  @Override
  public LimitPlan resolve(String tenantId, String path, String method) {
    String t = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
    String cacheKey = t + '|' + method + '|' + path;
    return cache.get(cacheKey, k -> resolveSlow(t, path, method));
  }

  private LimitPlan resolveSlow(String tenantId, String path, String method) {
    var list = rulesByTenant.getOrDefault(tenantId, Collections.emptyList());
    RouteLimit best = null;
    int bestLen = -1;
    for (RouteLimit rl : list) {
      if (rl.matches(path, method)) {
        int len = rl.routePrefix().length();
        if (len > bestLen) { best = rl; bestLen = len; }
      }
    }
    return best != null ? best.plan() : defaultPlan;
  }

  /** Seed demo rules; in real life, these would be loaded from DB/Kafka */
  private void seed() {
    rulesByTenant.put("default", List.of(
      new RouteLimit("/hello", "GET", LimitPlan.of(20, 5, 600))
    ));
    rulesByTenant.put("acme", List.of(
      new RouteLimit("/hello", "GET", LimitPlan.of(10, 2, 600)),
      new RouteLimit("/api/payments", null, LimitPlan.of(50, 10, 600)) // any method
    ));
  }
}
