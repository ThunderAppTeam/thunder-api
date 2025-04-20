pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "thunder-api"

include(
    "api-thunder",
    "core",
    "libraries:errors",
    "infrastructure:postgresql",
    "infrastructure:aws",
    "infrastructure:firebase",
    "infrastructure:client",
    "testing:architecture",
)
