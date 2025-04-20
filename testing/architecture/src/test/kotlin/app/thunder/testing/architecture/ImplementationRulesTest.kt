package app.thunder.testing.architecture

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ImplementationRulesTest {
    private val contractImport = Regex("""app\.thunder\.core\.[^.]+\.(?:repository|client)\.(.+)""")

    @Test
    @DisplayName("운영 소스는 Port 또는 Adapter로 끝나는 최상위 선언을 두지 않는다")
    fun noLegacyPortOrAdapterNames() {
        // given
        val files = ArchitectureSource.productionFiles

        // when
        val violations = requireScanned("운영 소스 파일", files) + files.flatMap { file ->
            file.declarations.filter { it.name.endsWith("Port") || it.name.endsWith("Adapter") }
                .map { Violation(file.relativePath, "${it.kind} ${it.name} 은(는) 금지된 이름이다") }
        }

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("infrastructure 구현 파일은 core 계약 하나만 가져오고 internal class 계약명Impl 로 구현한다")
    fun infrastructureImplementsExactlyOneContract() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.INFRASTRUCTURE)
        val implementations = files.filter { file -> file.referencedNames.any(contractImport::matches) }

        // when
        val violations = requireScanned("infrastructure 운영 소스 파일", files) +
            requireScanned("core 계약을 가져오는 infrastructure 구현 파일", implementations) +
            implementations.flatMap { file ->
                val contracts = file.referencedNames.mapNotNull { contractImport.matchEntire(it)?.groupValues?.get(1) }.distinct()
                val single = contracts.singleOrNull()
                    ?: return@flatMap listOf(Violation(file.relativePath, "core 계약을 ${contracts.size}개 가져옴: ${contracts.joinToString()}"))
                val hasImpl = file.declarations.any { it.kind == "class" && it.visibility == "internal" && it.name == "${single}Impl" }
                if (hasImpl) emptyList() else listOf(Violation(file.relativePath, "internal class ${single}Impl 선언이 없다"))
            }

        // then
        assertNoViolations(violations)
    }
}
