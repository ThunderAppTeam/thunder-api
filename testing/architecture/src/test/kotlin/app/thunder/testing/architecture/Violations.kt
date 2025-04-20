package app.thunder.testing.architecture

import org.assertj.core.api.Assertions.assertThat

internal data class Violation(
    val location: String,
    val detail: String,
) {
    override fun toString(): String = "$location: $detail"
}

internal fun requireScanned(
    description: String,
    scanned: Collection<*>,
): List<Violation> =
    if (scanned.isEmpty()) {
        listOf(Violation(ArchitectureSource.SETTINGS_FILE, "검사 대상이 없다: $description"))
    } else {
        emptyList()
    }

internal fun assertNoViolations(violations: List<Violation>) {
    assertThat(violations)
        .withFailMessage { "아키텍처 규칙 위반 ${violations.size}건\n" + violations.joinToString("\n") }
        .isEmpty()
}
