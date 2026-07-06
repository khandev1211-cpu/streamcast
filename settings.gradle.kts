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

rootProject.name = "streamcast"

include(":app")
include(":core:player")
include(":core:network")
include(":core:database")
include(":feature:library")
include(":feature:iptv")
include(":feature:live")
include(":feature:subtitles")
include(":ui:theme")
include(":settings")
