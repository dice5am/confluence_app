pluginManagement {
    repositories {
        google()
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

rootProject.name = "confluence_app"

include(
    ":app",
    ":core-ui",
    ":data",
    ":feature-home",
    ":feature-chart",
    ":feature-alerts",
)
