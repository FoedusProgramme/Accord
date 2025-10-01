@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

includeBuild("media3") {
    dependencySubstitution {
        substitute(module("androidx.media3:media3-common")).using(project(":lib-common"))
        substitute(module("androidx.media3:media3-common-ktx")).using(project(":lib-common-ktx"))
        substitute(module("androidx.media3:media3-exoplayer")).using(project(":lib-exoplayer"))
        substitute(module("androidx.media3:media3-exoplayer-midi")).using(project(":lib-decoder-midi"))
        substitute(module("androidx.media3:media3-session")).using(project(":lib-session"))
    }
}

rootProject.name = "Accord"
include(
    ":app",
    ":Cupertino",
    ":libPhonograph",
    ":hificore",
    ":misc:audiofxfwd",
    ":misc:audiofxstub",
    ":misc:alacdecoder"
)
