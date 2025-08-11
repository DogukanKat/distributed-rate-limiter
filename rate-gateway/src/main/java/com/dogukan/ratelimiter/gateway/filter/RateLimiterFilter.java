package com.dogukan.ratelimiter.gateway.filter;

import com.dogukan.ratelimiter.config.ConfigProvider;
import com.dogukan.ratelimiter.config.model.LimitPlan;
import com.dogukan.ratelimiter.core.Decision;
import com.dogukan.ratelimiter.core.RateContext;
import com.dogukan.ratelimiter.core.RateLimiterService;
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

  public RateLimiterFilter(RateLimiterService service, ConfigProvider config) {
    this.service = service;
    this.config = config;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    var req = exchange.getRequest();
    var headers = req.getHeaders();

    String tenant = headers.getFirst("X-Tenant-Id");
    if (tenant == null || tenant.isBlank()) tenant = "default";

    String identity = headers.getFirst("X-User-Id");
    if (identity == null || identity.isBlank()) {
      identity = req.getRemoteAddress() != null ? req.getRemoteAddress().getAddress().getHostAddress() : "anon";
    }

    String method = req.getMethod() != null ? req.getMethod().name() : "GET";
    String path = req.getPath().value();

    // Dinamik planı çöz
    LimitPlan plan = config.resolve(tenant, path, method);

    var ctx = new RateContext(
      tenant,
      path,
      method,
      identity,
      1,                      // cost
      plan.capacity(),
      plan.refillPerSec(),
      plan.ttlSeconds()
    );

    return service.check(ctx)
      .flatMap(d -> handle(exchange, chain, d))
      .timeout(Duration.ofMillis(100))
      .onErrorResume(ex -> chain.filter(exchange)); // fail-open
  }

  private Mono<Void> handle(ServerWebExchange exchange, WebFilterChain chain, Decision d) {
    if (d.allowed()) {
      exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(d.remaining()));
      return chain.filter(exchange);
    }
    var res = exchange.getResponse();
    res.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
    res.getHeaders().add("Retry-After", String.valueOf(d.retryAfterSeconds()));
    return res.setComplete();
  }
}
