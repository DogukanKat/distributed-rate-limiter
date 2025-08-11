package com.dogukan.ratelimiter.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.dogukan.ratelimiter")
public class RateGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(RateGatewayApplication.class, args);
  }
}
