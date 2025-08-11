package com.dogukan.ratelimiter.gateway.config.events;

import com.dogukan.ratelimiter.gateway.config.provider.RedisConfigProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer;
import jakarta.annotation.PostConstruct;
import reactor.core.Disposable;

@Configuration
public class ConfigChangeListener {

  private final ReactiveRedisMessageListenerContainer container;
  private final RedisConfigProvider provider;
  private Disposable subscription;

  public ConfigChangeListener(ReactiveRedisConnectionFactory cf, RedisConfigProvider provider) {
    this.container = new ReactiveRedisMessageListenerContainer(cf);
    this.provider = provider;
  }

  @PostConstruct
  public void init() {
    // Channel pattern: rl:cfg:ev:{<tenant>}
    subscription = container.receive(new ChannelTopic("rl:cfg:ev:*"))
      .subscribe(msg -> {
        String ch = msg.getChannel();
        // channel might be PatternTopic; guard both
        String channel = ch != null ? ch : msg.getChannel();
        if (channel == null) return;
        // extract tenant between braces
        String raw = channel; // e.g., rl:cfg:ev:{acme}
        int s = raw.indexOf('{');
        int e = raw.indexOf('}');
        String tenant = (s >= 0 && e > s) ? raw.substring(s+1, e) : "default";
        provider.invalidateTenant(tenant);
      });
  }
}
