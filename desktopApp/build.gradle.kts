import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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
    implementation(libs.compose.multiplatform.components.resources)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.json)
    implementation(libs.lms.desktop)
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
            targetFormats(TargetFormat.Msi)
            packageName = "SSUTime"
            packageVersion = "1.1.13"
            modules(
                "java.instrument",
                "java.management",
                "java.naming",
                "java.sql",
                "jdk.crypto.ec",
                "jdk.unsupported",
            )
        }
    }
}
