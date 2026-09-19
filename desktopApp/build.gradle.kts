import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val desktopPackageVersion = providers
    .gradleProperty("desktopVersion")
    .orElse(
        providers.fileContents(layout.projectDirectory.file("version.txt"))
            .asText
            .map(String::trim),
    )

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}
val postHogApiKey = System.getenv("POSTHOG_API_KEY")
    ?: localProperties.getProperty("posthog")
    ?: ""

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig")
    inputs.property("posthogApiKey", postHogApiKey)
    inputs.property("desktopVersion", desktopPackageVersion)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/yourssu/ssutime/desktop/DesktopBuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.yourssu.ssutime.desktop

            object DesktopBuildConfig {
                const val POSTHOG_API_KEY: String = "${postHogApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}"
                const val VERSION_NAME: String = "${desktopPackageVersion.get()}"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
    sourceSets.named("main") {
        kotlin.srcDir(generateBuildConfig.map { it.outputs.files })
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":data"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.multiplatform.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.compose.multiplatform.components.resources)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.json)
    implementation(libs.lms)
    implementation(libs.jna.platform)
    runtimeOnly(libs.slf4j.nop)
    testImplementation(kotlin("test"))
}

compose.resources {
    packageOfResClass = "com.yourssu.ssutime.desktop.ui.resources"
}

compose.desktop {
    application {
        mainClass = "com.yourssu.ssutime.desktop.MainKt"

        nativeDistributions {
            packageName = "SSUTime"
            packageVersion = desktopPackageVersion.get()
            modules(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.sql",
                "jdk.crypto.ec",
                "jdk.unsupported",
            )
            windows {
                iconFile.set(project.file("src/main/resources/icons/checkbox.ico"))
            }
        }
    }
}
