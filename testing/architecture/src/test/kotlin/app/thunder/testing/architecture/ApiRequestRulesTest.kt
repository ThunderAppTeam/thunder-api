package app.thunder.testing.architecture

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ApiRequestRulesTest {
    private val booleanConstructorParameter =
        Regex("""((?:@[\w:.]+(?:\([^)]*\))?\s+)*)\b(?:val|var)\s+(\w+)\s*:\s*Boolean\b(?=\s*[,)])""")
    private val requiredJsonProperty = Regex("""@(?:param:)?JsonProperty\([^)]*\brequired\s*=\s*true""")

    @Test
    @DisplayName("요청 DTO의 non-null Boolean 생성자 필드에는 @JsonProperty(required = true)가 있어 빠진 값을 false로 받지 않는다")
    fun requestBooleanFieldsAreRequired() {
        // given
        val requestFiles = ArchitectureSource.filesOf(ModuleCategory.API)
            .filter { it.packageName?.endsWith(".controller.request") == true }
        val fields = requestFiles.flatMap { file ->
            booleanConstructorParameter.findAll(CodeSanitizer.strip(file.content)).map { file to it }.toList()
        }

        // when
        val violations = requireScanned("controller.request 패키지의 요청 DTO 파일", requestFiles) +
            requireScanned("요청 DTO의 non-null Boolean 생성자 필드", fields) +
            fields.filterNot { (_, field) -> requiredJsonProperty.containsMatchIn(field.groupValues[1]) }
                .map { (file, field) ->
                    Violation(file.relativePath, "${field.groupValues[2]} 에 @JsonProperty(required = true)가 없다")
                }

        // then
        assertNoViolations(violations)
    }
}
