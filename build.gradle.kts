plugins {
    base
    jacoco
}

repositories {
    mavenCentral()
}

subprojects.forEach { evaluationDependsOn(it.path) }

val minimumLineCoverage = "0.95".toBigDecimal()

val coveredProjects = subprojects.filter { it.file("src/main").exists() }
val mainSourceSets = coveredProjects.map { it.the<SourceSetContainer>()["main"] }
val testTasks = subprojects.flatMap { it.tasks.withType<Test>() }
val executionDataFiles = files(testTasks.mapNotNull { it.extensions.getByType<JacocoTaskExtension>().destinationFile })
    .filter { it.exists() }

val testCoverageReport by tasks.registering(JacocoReport::class) {
    group = "verification"
    description = "Combines every module's test coverage into one report."
    executionData.from(executionDataFiles)
    dependsOn(testTasks)
    sourceDirectories.from(mainSourceSets.map { it.allSource.srcDirs })
    classDirectories.from(mainSourceSets.map { it.output.classesDirs })
    reports {
        xml.required = true
        html.required = true
    }
}

val testCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    group = "verification"
    description = "Fails when the combined line coverage is below the minimum."
    executionData.from(executionDataFiles)
    dependsOn(testTasks)
    sourceDirectories.from(mainSourceSets.map { it.allSource.srcDirs })
    classDirectories.from(mainSourceSets.map { it.output.classesDirs })
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = minimumLineCoverage
            }
        }
    }
    dependsOn(testCoverageReport)
}

tasks.check {
    dependsOn(testCoverageVerification)
}
