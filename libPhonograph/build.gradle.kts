@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uk.akane.libphonograph"
    compileSdk = 36

    defaultConfig {
        testFixtures.enable = true
        minSdk = 21
        consumerProguardFiles("libPhonograph/libPhonograph/consumer-rules.pro")
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("libPhonograph/libPhonograph/src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("libPhonograph/libPhonograph/src/main/java"))
            res.setSrcDirs(listOf("libPhonograph/libPhonograph/src/main/res"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "libPhonograph/libPhonograph/proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.media3.common)
    implementation(libs.kotlinx.coroutines.android)
}
