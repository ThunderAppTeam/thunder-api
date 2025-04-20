plugins {
    id("thunder.spring")
    id("thunder.library")
}

tasks.test {
    val scannedSources = fileTree(rootDir) {
        include("settings.gradle.kts", "**/build.gradle.kts", "**/src/main/kotlin/**/*.kt")
        exclude("**/build/**", "**/.gradle/**", "build-logic/**")
    }
    inputs.files(scannedSources)
        .withPropertyName("scannedSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
