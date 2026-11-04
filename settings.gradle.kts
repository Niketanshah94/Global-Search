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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FindPal"

include(":app")
include(":core:model")
include(":core:matching")
include(":core:data")
include(":core:sources")
include(":core:indexing")
include(":core:ui")
include(":feature:search")
include(":feature:settings")
include(":benchmark")
