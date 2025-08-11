plugins {
  id("org.springframework.boot") version "3.3.2"
  id("io.spring.dependency-management") version "1.1.6"
  java
}

dependencies {
  implementation(project(":rate-common"))
  implementation(project(":rate-core"))
  implementation(project(":rate-config"))
  implementation(project(":rate-admin"))
  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")
  implementation("org.springframework.kafka:spring-kafka")
  implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  implementation("net.logstash.logback:logstash-logback-encoder:7.4")

  // Observability (ileride kullanacağız)
  implementation("io.micrometer:micrometer-registry-prometheus")
  implementation("io.micrometer:micrometer-tracing-bridge-otel")
  implementation("io.opentelemetry:opentelemetry-exporter-otlp")

  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
  useJUnitPlatform()
}
