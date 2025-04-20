package app.thunder.testing.architecture

import java.nio.file.Files
import java.nio.file.Path

internal enum class ModuleCategory {
    API,
    CORE,
    INFRASTRUCTURE,
    LIBRARIES,
    TESTING,
    ;

    companion object {
        fun of(modulePath: String): ModuleCategory? {
            val head = modulePath.substringBefore(':')
            return when {
                ':' !in modulePath && head.startsWith("api-") -> API
                head == "core" -> CORE
                head == "infrastructure" -> INFRASTRUCTURE
                head == "libraries" -> LIBRARIES
                head == "testing" -> TESTING
                else -> null
            }
        }
    }
}

internal data class ProjectModule(
    val path: String,
    val directory: Path,
    val relativeBuildFile: String,
) {
    val directoryName: String = path.substringAfterLast(':')
    val category: ModuleCategory? = ModuleCategory.of(path)
    val mainSourceRoot: Path = directory.resolve("src/main/kotlin")

    val projectDependencies: List<String> by lazy {
        val buildFile = directory.resolve("build.gradle.kts")
        if (!Files.isRegularFile(buildFile)) {
            emptyList()
        } else {
            PROJECT_REFERENCE.findAll(Files.readString(buildFile)).map { it.groupValues[1] }.distinct().toList()
        }
    }

    private companion object {
        val PROJECT_REFERENCE = Regex("""project\(\s*(?:path\s*=\s*)?"\s*:?([^"]+)"\s*(?:,[^)]*)?\)""")
    }
}
