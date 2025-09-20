import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "uk.akane.cupertino"
    compileSdk = 36

    defaultConfig {
        minSdk = 31
        consumerProguardFiles("Cupertino/Cupertino/consumer-rules.pro")
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile("Cupertino/Cupertino/src/main/AndroidManifest.xml")
            java.setSrcDirs(listOf("Cupertino/Cupertino/src/main/java"))
            res.setSrcDirs(listOf("Cupertino/Cupertino/src/main/res"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "Cupertino/Cupertino/proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.hiddenapibypass)
}
