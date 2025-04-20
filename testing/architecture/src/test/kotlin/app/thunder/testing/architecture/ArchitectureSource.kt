package app.thunder.testing.architecture

import java.nio.file.Files
import java.nio.file.Path

internal object ArchitectureSource {
    private val EXCLUDED_DIRECTORIES = setOf(".git", ".gradle", ".idea", "build")
    private val INCLUDE_CALL = Regex("include\\s*\\(([^)]*)\\)", RegexOption.DOT_MATCHES_ALL)
    private val QUOTED = Regex("\"([^\"]+)\"")

    val projectRoot: Path = generateSequence(Path.of(System.getProperty("user.dir")).toAbsolutePath()) {
        it.parent
    }.first { Files.exists(it.resolve("settings.gradle.kts")) }

    const val SETTINGS_FILE = "settings.gradle.kts"

    val modules: List<ProjectModule> by lazy {
        val settings = Files.readString(projectRoot.resolve(SETTINGS_FILE))
            .lines()
            .joinToString("\n") { it.substringBefore("//") }
        INCLUDE_CALL.findAll(settings)
            .flatMap { call -> QUOTED.findAll(call.groupValues[1]).map { it.groupValues[1].trimStart(':') } }
            .distinct()
            .map { path ->
                val directory = projectRoot.resolve(path.replace(':', '/'))
                ProjectModule(path, directory, relativePath(directory.resolve("build.gradle.kts")))
            }
            .toList()
    }

    val productionFiles: List<KotlinSourceFile> by lazy {
        modules.flatMap { module ->
            val root = module.mainSourceRoot.toFile()
            if (!root.isDirectory) {
                emptyList()
            } else {
                root.walkTopDown()
                    .onEnter { it.name !in EXCLUDED_DIRECTORIES }
                    .filter { it.isFile && it.extension == "kt" }
                    .map { KotlinSourceFile(relativePath(it.toPath()), module, it.readText()) }
                    .sortedBy { it.relativePath }
                    .toList()
            }
        }
    }

    fun modulesOf(category: ModuleCategory): List<ProjectModule> = modules.filter { it.category == category }

    fun filesOf(vararg categories: ModuleCategory): List<KotlinSourceFile> =
        productionFiles.filter { it.module.category in categories }

    private fun relativePath(path: Path): String = projectRoot.relativize(path).toString().replace('\\', '/')
}
