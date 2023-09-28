@Suppress("DSL_SCOPE_VIOLATION") plugins {
  base
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.arrow.kotlin)
  alias(libs.plugins.arrow.publish)
  alias(libs.plugins.arrow.nexus)
  alias(libs.plugins.kotest.multiplatform)
  alias(libs.plugins.detekt)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kover)
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

  testImplementation(libs.kotest.property)
  testImplementation(libs.kotest.assertions)
  testImplementation(libs.kotest.junit5)
}
