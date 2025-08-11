package com.dogukan.ratelimiter.admin.dto;

import jakarta.validation.constraints.*;
import org.checkerframework.checker.index.qual.Positive;

public record UpsertRuleRequest(
  @NotBlank String tenantId,
  @NotBlank String routePrefix,
  @Pattern(regexp="\\*|GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS") String method,
  @Positive long capacity,
  @Positive long refillPerSec,
  @Positive long ttlSeconds
) { }
