plugins { `java-library` }

dependencies {
  implementation(project(":rate-common"))
  api("com.github.ben-manes.caffeine:caffeine:3.1.8")
}
