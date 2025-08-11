package com.dogukan.ratelimiter.gateway.config;

import com.dogukan.ratelimiter.config.ConfigProvider;
import com.dogukan.ratelimiter.config.InMemoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigBeans {

  @Bean
  public ConfigProvider configProvider() {
    return new InMemoryProvider();
  }
}
