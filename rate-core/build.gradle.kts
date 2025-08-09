plugins { `java-library` }

dependencies {
  implementation(project(":rate-common"))
  api("io.projectreactor:reactor-core:3.6.6")
}
