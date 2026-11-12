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

rootProject.name = "SoundBox"

include(":app")

include(":core:parser")
include(":core:data")
include(":core:tts")
include(":core:design")

include(":feature:onboarding")
include(":feature:home")
include(":feature:history")
include(":feature:settings")
include(":feature:qr")
include(":feature:reports")
include(":feature:help")
include(":feature:diagnostics")
