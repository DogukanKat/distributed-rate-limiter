import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
  java
}

allprojects {
  group = "com.dogukan.ratelimiter"
  version = "0.1.0-SNAPSHOT"
}

subprojects {
  apply(plugin = "java")

  java {
    toolchain {
      languageVersion.set(JavaLanguageVersion.of(17))
    }
  }

  tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
      events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
  }
}
