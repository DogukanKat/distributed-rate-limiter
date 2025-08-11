package com.dogukan.ratelimiter.gateway.filter;

import com.dogukan.ratelimiter.common.audit.AuditEvent;
import com.dogukan.ratelimiter.config.ConfigProvider;
import com.dogukan.ratelimiter.config.model.LimitPlan;
import com.dogukan.ratelimiter.core.Decision;
import com.dogukan.ratelimiter.core.RateContext;
import com.dogukan.ratelimiter.core.RateLimiterService;
import com.dogukan.ratelimiter.gateway.audit.AuditPublisher;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimiterFilter implements WebFilter {

  private final RateLimiterService service;
  private final ConfigProvider config;
  private final AuditPublisher audit;
  private final Counter allowCounter;
  private final Counter denyCounter;
  private final Timer decisionTimer;

  private final double nearLimitRatio = 0.10;

  public RateLimiterFilter(
    RateLimiterService service,
    ConfigProvider config,
    AuditPublisher audit,
    MeterRegistry registry
  ) {
    this.service = service;
    this.config = config;
    this.audit = audit;
    this.allowCounter = registry.counter("rate_limit_allowed_total");
    this.denyCounter = registry.counter("rate_limit_blocked_total");
    this.decisionTimer = registry.timer("rate_limit_decision_duration_ms");
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var req = exchange.getRequest();
    var headers = req.getHeaders();

    String tenant = headers.getFirst("X-Tenant-Id");
    if (tenant == null || tenant.isBlank()) tenant = "default";

    String identity = headers.getFirst("X-User-Id");
    if (identity == null || identity.isBlank()) {
      identity = req.getRemoteAddress() != null
        ? req.getRemoteAddress().getAddress().getHostAddress()
        : "anon";
    }

    String method = req.getMethod() != null ? req.getMethod().name() : "GET";
    String path = req.getPath().value();

    LimitPlan plan = config.resolve(tenant, path, method);

    var ctx = new RateContext(
      tenant, path, method, identity,
      1,
      plan.capacity(),
      plan.refillPerSec(),
      plan.ttlSeconds()
    );

    final String fTenant = tenant;
    final String fPath = path;
    final String fMethod = method;
    final String fIdentity = identity;
    final long fCapacity = plan.capacity();
    final long fRefill = plan.refillPerSec();

    long start = System.nanoTime();
    return service.check(ctx)
      .flatMap(d -> {
        decisionTimer.record(Duration.ofNanos(System.nanoTime() - start));
        long now = System.currentTimeMillis();

        if (d.allowed()) {
          allowCounter.increment();
          exchange.getResponse().getHeaders()
            .add("X-RateLimit-Remaining", String.valueOf(d.remaining()));

          if (fCapacity > 0 && (double) d.remaining() / (double) fCapacity <= nearLimitRatio) {
            audit.publish(AuditEvent.nearLimit(
              fTenant, fPath, fMethod, fIdentity,
              d.remaining(), fCapacity, fRefill, now
            )).subscribe();
          }

          // Allow audit (fire-and-forget)
          audit.publish(AuditEvent.allow(
            fTenant, fPath, fMethod, fIdentity,
            d.remaining(), fCapacity, fRefill, now
          )).subscribe();

          return chain.filter(exchange);
        } else {
          denyCounter.increment();
          var res = exchange.getResponse();
          res.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
          res.getHeaders().add("Retry-After", String.valueOf(d.retryAfterSeconds()));

          // Deny audit (fire-and-forget)
          audit.publish(AuditEvent.deny(
            fTenant, fPath, fMethod, fIdentity,
            d.remaining(), fCapacity, fRefill, now
          )).subscribe();

          return res.setComplete();
        }
      })
      .timeout(Duration.ofMillis(100))
      // Fail-open: hatada trafiği kesmeyelim (opsiyonel: metric/log eklenebilir)
      .onErrorResume(ex -> chain.filter(exchange));
  }
}
