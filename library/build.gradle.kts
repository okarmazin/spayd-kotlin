// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "tools.kmp"
version = "0.0.1"

kotlin {
    explicitApi()
    jvm()
    js {
        browser()
        nodejs()
    }
    wasmWasi {
        nodejs()
    }
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    tvosX64()
    tvosArm64()
    macosX64()
    linuxX64()
    linuxArm64()
    mingwX64()

    sourceSets {
        val commonMain by getting {
            dependencies {}
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
    }
}

android {
    namespace = "tools.kmp.spayd"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "spayd", version.toString())

    pom {
        name = "QR Platba"
        description =
            "Zero-dependency SPAYD (Short Payment Descriptor) library for Kotlin Multiplatform. https://qr-platba.cz/"
        inceptionYear = "2025"
        url.set("https://github.com/okarmazin/spayd-kotlin")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("okarmazin")
                name.set("Ondřej Karmazín")
                url.set("https://github.com/okarmazin")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/okarmazin/spayd-kotlin.git")
            developerConnection.set("scm:git:git://github.com/okarmazin/spayd-kotlin.git")
            url.set("https://github.com/okarmazin/spayd-kotlin")
        }
    }
}
