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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DataAgregate"
include(":app")

// Core Modules
include(":core:network")
include(":core:domain")
include(":core:ui")
include(":core:database")

// Feature Modules
include(":feature:home")
include(":feature:news_feed")
include(":feature:explore")
include(":feature:categories")
include(":feature:category_articles")
include(":feature:profile")
include(":feature:article_reader")
include(":feature:saved")
