import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val desktopPackageVersion = providers
    .gradleProperty("desktopVersion")
    .orElse("1.1.13")

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
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
