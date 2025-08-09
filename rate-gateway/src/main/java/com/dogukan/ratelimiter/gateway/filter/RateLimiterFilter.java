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

@Component
public class RateLimiterFilter implements WebFilter {

  private final RateLimiterService service;

  public RateLimiterFilter(RateLimiterService service) {
    this.service = service;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Şimdilik kontrol yapmadan devam ediyoruz (Redis bağlanınca aktif olacak)
    return chain.filter(exchange);
  }
}
