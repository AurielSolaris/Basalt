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

rootProject.name = "Basalt"

include(":app")
include(":core:design")
include(":core:dotmatrix")
include(":core:time")
include(":core:data")
include(":feature:alarm")
include(":feature:clock")
include(":feature:timer")
include(":feature:stopwatch")
include(":feature:bedtime")
include(":feature:settings")
include(":widget")
