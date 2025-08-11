package com.dogukan.ratelimiter.config;

import com.dogukan.ratelimiter.config.model.LimitPlan;

public interface ConfigProvider {
  /**
   * Resolve limit plan using tenantId + path + method.
   * Implementations should apply longest-prefix match semantics.
   */
  LimitPlan resolve(String tenantId, String path, String method);
}
