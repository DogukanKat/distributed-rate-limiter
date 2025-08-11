package com.dogukan.ratelimiter.admin.api;

import com.dogukan.ratelimiter.admin.dto.AdminRuleView;
import com.dogukan.ratelimiter.admin.dto.UpsertRuleRequest;
import com.dogukan.ratelimiter.admin.service.AdminConfigService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/admin/limits")
public class AdminController {

  private final AdminConfigService svc;

  public AdminController(AdminConfigService svc) {
    this.svc = svc;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Void> upsert(@Valid @RequestBody UpsertRuleRequest req) {
    String method = (req.method() == null || req.method().isBlank()) ? "*" : req.method();
    return svc.upsertRule(req.tenantId(), method, req.routePrefix(),
      req.capacity(), req.refillPerSec(), req.ttlSeconds()).then();
  }

  @DeleteMapping
  public Mono<Void> delete(@RequestParam String tenantId,
                           @RequestParam String method,
                           @RequestParam String routePrefix) {
    return svc.deleteRule(tenantId, method, routePrefix).then();
  }

  @GetMapping(produces = MediaType.APPLICATION_NDJSON_VALUE)
  public Flux<AdminRuleView> list(@RequestParam String tenantId) {
    return svc.listRaw(tenantId)
      .flatMap(e -> {
        try {
          String field = String.valueOf(e.getKey());     // "<METHOD> <prefix>"
          String value = String.valueOf(e.getValue());   // "cap:refill:ttl"

          String[] parts = field.split("\\s+", 2);
          String method = parts.length > 0 ? parts[0] : "*";
          String prefix = parts.length > 1 ? parts[1] : "/";

          String[] vals = value != null ? value.split(":") : new String[0];
          if (vals.length != 3) {
            return Mono.empty();
          }
          long cap = Long.parseLong(vals[0]);
          long refill = Long.parseLong(vals[1]);
          long ttl = Long.parseLong(vals[2]);

          return Mono.just(new AdminRuleView(tenantId, method, prefix, cap, refill, ttl));
        } catch (Exception ignore) {
          return Mono.empty();
        }
      });
  }

}
