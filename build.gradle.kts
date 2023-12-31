import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  base
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.arrow.kotlin)
  alias(libs.plugins.dokka)
  alias(libs.plugins.publish)
  alias(libs.plugins.knit)
}

repositories {
  mavenCentral()
}

tasks {
  withType<Test> {
    useJUnitPlatform()
  }

  withType<DokkaTask>().configureEach {
    outputDirectory.set(rootDir.resolve("docs"))
    moduleName.set("Kotlin JDBC DSL")
    dokkaSourceSets {
      named("main") {
        includes.from("README.md")
        sourceLink {
          localDirectory.set(file("src/main/kotlin"))
          remoteUrl.set(uri("https://github.com/nomisRev/kotlin-jdbc-dsl/tree/main/src/main/kotlin").toURL())
          remoteLineSuffix.set("#L")
        }
      }
    }
  }
}

dependencies {
  implementation(kotlin("stdlib"))
}
