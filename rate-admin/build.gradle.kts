plugins {
  `java-library`
  id("io.spring.dependency-management") version "1.1.6"
}

java {
  toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}

dependencyManagement {
  imports {
    mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.2")
  }
}

dependencies {
  implementation(project(":rate-common"))
  implementation(project(":rate-config"))

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
}
