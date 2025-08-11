package com.dogukan.ratelimiter.admin.dto;

public record AdminRuleView(
  String tenantId,
  String method,
  String routePrefix,
  long capacity,
  long refillPerSec,
  long ttlSeconds
) { }
