package app.thunder.testing.architecture

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BoundaryRulesTest {
    private val infrastructureRoot = "app.thunder.infrastructure"
    private val librariesForbiddenPrefixes = listOf("app.thunder.core", "app.thunder.api", infrastructureRoot)

    @Test
    @DisplayName("infrastructure 모듈은 다른 infrastructure 모듈의 패키지를 가져오지 않는다")
    fun infrastructureModulesAreIsolated() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.INFRASTRUCTURE)
        val moduleNames = ArchitectureSource.modulesOf(ModuleCategory.INFRASTRUCTURE).map { it.directoryName }

        // when
        val violations = requireScanned("infrastructure 운영 소스 파일", files) + files.flatMap { file ->
            val others = moduleNames - file.module.directoryName
            file.referencedNames.filter { import -> others.any { import == "$infrastructureRoot.$it" || import.startsWith("$infrastructureRoot.$it.") } }
                .map { Violation(file.relativePath, "다른 infrastructure 모듈을 가져옴: $it") }
        }

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("api 런타임 모듈은 infrastructure에서 InfrastructureConfiguration으로 끝나는 이름만 가져온다")
    fun apiImportsOnlyInfrastructureConfigurations() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.API)

        // when
        val violations = requireScanned("api 운영 소스 파일", files) + files.flatMap { file ->
            file.referencedNames.filter { it.startsWith("$infrastructureRoot.") && !it.endsWith("InfrastructureConfiguration") }
                .map { Violation(file.relativePath, "InfrastructureConfiguration 이 아닌 import $it") }
        }

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("libraries 모듈은 core, api, infrastructure 패키지를 가져오지 않는다")
    fun librariesAreIndependent() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.LIBRARIES)

        // when
        val violations = files.flatMap { file ->
            file.referencedNames.filter { import -> librariesForbiddenPrefixes.any { import == it || import.startsWith("$it.") } }
                .map { Violation(file.relativePath, "금지된 import $it") }
        }

        // then
        assertNoViolations(violations)
    }
}
