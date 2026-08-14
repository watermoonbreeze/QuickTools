rootProject.name = "QuickTools"

pluginManagement {
    repositories {


        maven(url = uri("https://jitpack.io"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/repositories/google"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/groups/public"))
        maven(url = uri("https://maven.aliyun.com/repository/jcenter"))
        maven(url = uri("https://maven.aliyun.com/repository/gradle-plugin"))
        maven(url = uri("https://maven.aliyun.com/repository/public/"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/repositories/center"))


        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {


        maven(url = uri("https://jitpack.io"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/repositories/google"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/groups/public"))
        maven(url = uri("https://maven.aliyun.com/repository/jcenter"))
        maven(url = uri("https://maven.aliyun.com/repository/public/"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/repositories/center"))
        
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":desktopApp")
include(":shared")