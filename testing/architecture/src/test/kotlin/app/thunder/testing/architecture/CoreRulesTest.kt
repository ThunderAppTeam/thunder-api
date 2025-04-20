package app.thunder.testing.architecture

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CoreRulesTest {
    private val forbiddenPrefixes = listOf(
        "org.springframework",
        "jakarta",
        "app.thunder.infrastructure",
        "app.thunder.api",
        "app.thunder.libraries",
    )
    private val repositoryPackage = Regex("""app\.thunder\.core\.[^.]+\.repository""")
    private val clientPackage = Regex("""app\.thunder\.core\.[^.]+\.client""")
    private val corePackageDomain = Regex("""app\.thunder\.core\.([^.]+)\.[^.]+""")

    @Test
    @DisplayName("core 운영 코드는 Spring, jakarta, infrastructure, api, libraries 패키지를 가져오지 않는다")
    fun coreIsFrameworkFree() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.CORE)

        // when
        val violations = requireScanned("core 운영 소스 파일", files) + files.flatMap { file ->
            file.referencedNames.filter { import -> forbiddenPrefixes.any { import == it || import.startsWith("$it.") } }
                .map { Violation(file.relativePath, "금지된 import $it") }
        }

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("core repository 패키지는 interface XRepository만, client 패키지는 interface XClient만 선언한다")
    fun coreContractsAreNamedInterfaces() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.CORE)

        val contractFiles = files.filter {
            repositoryPackage.matches(it.packageName.orEmpty()) || clientPackage.matches(it.packageName.orEmpty())
        }

        // when
        val violations = requireScanned("core 운영 소스 파일", files) +
            requireScanned("core repository 또는 client 패키지 파일", contractFiles) + files.flatMap { file ->
            val packageName = file.packageName.orEmpty()
            val suffix = when {
                repositoryPackage.matches(packageName) -> "Repository"
                clientPackage.matches(packageName) -> "Client"
                else -> return@flatMap emptyList()
            }
            if (file.declarations.isEmpty()) {
                listOf(Violation(file.relativePath, "interface X$suffix 선언이 없다"))
            } else {
                file.declarations.filterNot { it.kind == "interface" && it.visibility == "public" && it.name.length > suffix.length && it.name.endsWith(suffix) }
                    .map { Violation(file.relativePath, "${it.visibility} ${it.kind} ${it.name} 은(는) interface X$suffix 가 아니다") }
            }
        }

        // then
        assertNoViolations(violations)
    }

    @Test
    @DisplayName("core 파일은 다른 domain의 타입을 해당 domain의 domain 패키지에서만 가져온다")
    fun coreCrossDomainImportsUseDomainPackageOnly() {
        // given
        val files = ArchitectureSource.filesOf(ModuleCategory.CORE)
        val domainFiles = files.filter { corePackageDomain.matches(it.packageName.orEmpty()) }

        // when
        val violations = requireScanned("core 운영 소스 파일", files) +
            requireScanned("app.thunder.core.<domain>.<kind> package 의 core 파일", domainFiles) + files.flatMap { file ->
            val ownDomain = corePackageDomain.matchEntire(file.packageName.orEmpty())?.groupValues?.get(1)
                ?: return@flatMap emptyList()
            file.referencedNames.mapNotNull { import ->
                val otherDomain = import.removePrefix("app.thunder.core.").takeIf { it != import }?.substringBefore('.')
                when {
                    otherDomain == null || otherDomain == ownDomain -> null
                    import.startsWith("app.thunder.core.$otherDomain.domain.") -> null
                    else -> Violation(file.relativePath, "다른 domain '$otherDomain' 의 domain 밖을 가져옴: $import")
                }
            }
        }

        // then
        assertNoViolations(violations)
    }
}
