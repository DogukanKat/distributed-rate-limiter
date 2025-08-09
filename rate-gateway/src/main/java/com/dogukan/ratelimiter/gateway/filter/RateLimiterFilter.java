package com.dogukan.ratelimiter.gateway.filter;

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

@Component // artık aktif
public class RateLimiterFilter implements WebFilter {

  private final RateLimiterService service;

  public RateLimiterFilter(RateLimiterService service) {
    this.service = service;
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

    // getMethodValue() yerine:
    String method = req.getMethod() != null ? req.getMethod().name() : "GET";

    var ctx = new RateContext(
      tenant,
      req.getPath().value(),
      method,
      identity,
      1,   // cost
      10,  // capacity (eski 100 -> 10)
      2,   // refill tokens/sec (eski 50 -> 2)
      300  // ttl seconds (farketmez, küçük tutabiliriz)
    );

    return service.check(ctx)
      .flatMap(decision -> handleDecision(exchange, chain, decision))
      .timeout(java.time.Duration.ofMillis(100))
      .onErrorResume(ex -> chain.filter(exchange)); // fail-open
  }

  private Mono<Void> handleDecision(ServerWebExchange exchange, WebFilterChain chain, Decision d) {
    if (d.allowed()) {
      exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", String.valueOf(d.remaining()));
      return chain.filter(exchange);
    }
    var res = exchange.getResponse();
    res.setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
    res.getHeaders().add("Retry-After", String.valueOf(d.retryAfterSeconds()));
    return res.setComplete();
  }
}
