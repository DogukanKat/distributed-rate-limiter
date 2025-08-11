package com.dogukan.ratelimiter.common.audit;

public record AuditEvent(
  String tenantId,
  String route,
  String method,
  String identity,
  String type,          // ALLOW | DENY | NEAR_LIMIT
  long remaining,
  long capacity,
  long refillPerSec,
  long timestampMs
) {
  public static AuditEvent allow(String tenantId, String route, String method, String identity,
                                 long remaining, long capacity, long refillPerSec, long ts) {
    return new AuditEvent(tenantId, route, method, identity, "ALLOW", remaining, capacity, refillPerSec, ts);
  }
  public static AuditEvent deny(String tenantId, String route, String method, String identity,
                                long remaining, long capacity, long refillPerSec, long ts) {
    return new AuditEvent(tenantId, route, method, identity, "DENY", remaining, capacity, refillPerSec, ts);
  }
  public static AuditEvent nearLimit(String tenantId, String route, String method, String identity,
                                     long remaining, long capacity, long refillPerSec, long ts) {
    return new AuditEvent(tenantId, route, method, identity, "NEAR_LIMIT", remaining, capacity, refillPerSec, ts);
  }
}
