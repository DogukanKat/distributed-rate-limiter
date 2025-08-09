pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
  }
}

rootProject.name = "distributed-rate-limiter"
include(":rate-common", ":rate-core", ":rate-config", ":rate-admin", ":rate-gateway")
