package com.dogukan.ratelimiter.config.model;

public record RouteLimit(String routePrefix, String method, LimitPlan plan) {
  public boolean matches(String path, String method) {
    if (this.method != null && !this.method.equalsIgnoreCase(method)) return false;
    return path.startsWith(routePrefix);
  }
}
