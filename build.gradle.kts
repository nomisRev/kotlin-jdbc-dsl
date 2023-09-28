@Suppress("DSL_SCOPE_VIOLATION") plugins {
  base
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.arrow.kotlin)
  alias(libs.plugins.arrow.publish)
  alias(libs.plugins.arrow.nexus)
  alias(libs.plugins.dokka)
  id("org.jetbrains.kotlinx.knit") version "0.4.0"
}

repositories {
  mavenCentral()
}

tasks.withType<Test> {
  useJUnitPlatform()
}

dependencies {
  implementation(kotlin("stdlib"))
}
