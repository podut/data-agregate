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

rootProject.name = "DataAgregate-Workspace"

// Include Android modules from sub-folder
val androidProjectDir = "DataAgregate"

include(":app")
project(":app").projectDir = file("$androidProjectDir/app")

include(":core:database", ":core:domain", ":core:network", ":core:ui")
project(":core:database").projectDir = file("$androidProjectDir/core/database")
project(":core:domain").projectDir = file("$androidProjectDir/core/domain")
project(":core:network").projectDir = file("$androidProjectDir/core/network")
project(":core:ui").projectDir = file("$androidProjectDir/core/ui")

include(
    ":feature:article_reader",
    ":feature:categories",
    ":feature:category_articles",
    ":feature:explore",
    ":feature:home",
    ":feature:news_feed",
    ":feature:profile",
    ":feature:saved"
)
project(":feature:article_reader").projectDir = file("$androidProjectDir/feature/article_reader")
project(":feature:categories").projectDir = file("$androidProjectDir/feature/categories")
project(":feature:category_articles").projectDir = file("$androidProjectDir/feature/category_articles")
project(":feature:explore").projectDir = file("$androidProjectDir/feature/explore")
project(":feature:home").projectDir = file("$androidProjectDir/feature/home")
project(":feature:news_feed").projectDir = file("$androidProjectDir/feature/news_feed")
project(":feature:profile").projectDir = file("$androidProjectDir/feature/profile")
project(":feature:saved").projectDir = file("$androidProjectDir/feature/saved")
