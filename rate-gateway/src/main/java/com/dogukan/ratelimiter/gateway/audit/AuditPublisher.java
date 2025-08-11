package com.dogukan.ratelimiter.gateway.audit;

import com.dogukan.ratelimiter.common.audit.AuditEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AuditPublisher {

  private static final String TOPIC = "rl-audit";
  private final KafkaTemplate<String, Object> kafka;

  public AuditPublisher(KafkaTemplate<String, Object> kafka) {
    this.kafka = kafka;
  }

  public Mono<Void> publish(AuditEvent event) {
    return Mono.fromFuture(kafka.send(TOPIC, event.identity(), event))
      .then()
      .onErrorResume(ex -> Mono.empty());
  }
}
