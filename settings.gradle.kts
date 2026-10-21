pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
val ghUsername = localProperties.getProperty("gpr.user") ?: System.getenv("GH_PACKAGES_USER")
val ghPassword = localProperties.getProperty("gpr.key") ?: System.getenv("GH_PACKAGES_TOKEN")

dependencyResolutionManagement {
    repositories {
        // Local publish of light-keyboard (see docs/DASHBOARD-SUBMISSION.md)
        mavenLocal()
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages-Keyboard"
            url = uri("https://maven.pkg.github.com/lightphone/light-keyboard")
            credentials {
                username = ghUsername
                password = ghPassword
            }
        }
    }
}

rootProject.name = "fold-light"

includeBuild("plugin")
include(":lint-rules")
include(":sdk:shared")
include(":sdk:ui")
include(":sdk:client")

// Light Dashboard builds :tool with -DlightSdk.toolOnly=true — Fold lives here.
include(":tool")
project(":tool").projectDir = file("examples/fold")

// Local-only modules (skipped on Light's tool-only CI builds).
if (providers.systemProperty("lightSdk.toolOnly").orNull != "true") {
    include(":sdk:server")
    include(":sdk:emulator")
    include(":examples:authenticator")
    project(":examples:authenticator").projectDir = file("examples/authenticator")
}
