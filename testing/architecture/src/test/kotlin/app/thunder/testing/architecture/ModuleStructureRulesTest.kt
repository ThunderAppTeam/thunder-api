package app.thunder.testing.architecture

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ModuleStructureRulesTest {
    private val placementPattern = Regex("""api-[^:]+|core|infrastructure:[^:]+|libraries:[^:]+|testing:[^:]+""")
    private val corePackagePattern = Regex("""app\.thunder\.core\.[a-z][a-z0-9]*\.(domain|repository|client|command)""")

    private val allowedDependencies = mapOf(
        ModuleCategory.CORE to emptySet(),
        ModuleCategory.LIBRARIES to setOf(ModuleCategory.LIBRARIES),
        ModuleCategory.INFRASTRUCTURE to setOf(ModuleCategory.CORE, ModuleCategory.LIBRARIES),
        ModuleCategory.API to setOf(ModuleCategory.CORE, ModuleCategory.LIBRARIES, ModuleCategory.INFRASTRUCTURE),
        ModuleCategory.TESTING to emptySet(),
    )

    @Test
    @DisplayName("모든 Gradle 모듈은 api-*, core, infrastructure:*, libraries:*, testing:* 중 하나에 배치되고 api, core, infrastructure가 존재한다")
    fun modulesFollowPlacement() {
        // given
        val modules = ArchitectureSource.modules

        // when
        val violations = requireScanned("Gradle 모듈", modules) +
            modules.filterNot { placementPattern.matches(it.path) }.map {
                Violation(ArchitectureSource.SETTINGS_FILE, "허용되지 않은 모듈 경로 '${it.path}'")
            } +
            listOfNotNull(
                missing("api-* 모듈") { it.path.matches(Regex("""api-[^:]+""")) },
                missing("core 모듈") { it.path == "core" },
                missing("infrastructure:* 모듈") { it.path.matches(Regex("""infrastructure:[^:]+""")) },
            )

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("모든 운영 소스의 package는 소속 모듈의 루트 package 규칙을 따른다")
    fun sourcePackagesMatchModuleRoot() {
        // given
        val files = ArchitectureSource.productionFiles

        // when
        val violations = requireScanned("운영 소스 파일", files) +
            files.mapNotNull { file -> packageViolation(file)?.let { Violation(file.relativePath, it) } }

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("Gradle 모듈 의존은 core, libraries, infrastructure, api, testing 허용 방향 표를 따른다")
    fun moduleDependenciesFollowMatrix() {
        // given
        val modules = ArchitectureSource.modules

        // when
        val violations = requireScanned("Gradle 모듈", modules) + modules.flatMap { module ->
            val source = module.category ?: return@flatMap emptyList()
            module.projectDependencies.mapNotNull { target ->
                val targetCategory = ModuleCategory.of(target)
                when {
                    targetCategory == null -> Violation(module.relativeBuildFile, ":$target 은(는) 분류되지 않은 모듈이다")
                    targetCategory !in allowedDependencies.getValue(source) ->
                        Violation(module.relativeBuildFile, ":$target 의존 금지 ($source -> $targetCategory)")

                    else -> null
                }
            }
        }

        // then
        assertNoViolations(violations)
    }

    private fun missing(
        description: String,
        exists: (ProjectModule) -> Boolean,
    ): Violation? =
        if (ArchitectureSource.modules.any(exists)) null else Violation(ArchitectureSource.SETTINGS_FILE, "$description 이(가) 없다")

    private fun packageViolation(file: KotlinSourceFile): String? {
        val packageName = file.packageName ?: return "package 선언이 없다"
        val module = file.module
        val matches = when (module.category) {
            ModuleCategory.CORE -> corePackagePattern.matches(packageName)
            ModuleCategory.API -> isUnder(packageName, "app.thunder.api")
            ModuleCategory.INFRASTRUCTURE -> isUnder(packageName, "app.thunder.infrastructure.${module.directoryName}")
            ModuleCategory.LIBRARIES -> isUnder(packageName, "app.thunder.libraries.${module.directoryName}")
            ModuleCategory.TESTING -> isUnder(packageName, "app.thunder.testing.${module.directoryName}")
            null -> return "package 배치 규칙이 없는 모듈 '${module.path}'"
        }
        return if (matches) null else "모듈 '${module.path}' 에서 허용되지 않은 package '$packageName'"
    }

    private fun isUnder(
        packageName: String,
        root: String,
    ): Boolean = packageName == root || packageName.startsWith("$root.")
}
