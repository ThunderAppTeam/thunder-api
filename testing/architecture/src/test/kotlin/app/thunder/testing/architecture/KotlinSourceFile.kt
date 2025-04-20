package app.thunder.testing.architecture

internal data class Declaration(
    val kind: String,
    val name: String,
    val visibility: String,
)

internal data class KotlinSourceFile(
    val relativePath: String,
    val module: ProjectModule,
    val content: String,
) {
    val packageName: String? = PACKAGE.find(content)?.groupValues?.get(1)?.replace("`", "")

    private val code: String = CodeSanitizer.strip(content)

    val imports: List<String> = IMPORT.findAll(code).map { it.groupValues[1].replace("`", "") }.toList()

    val referencedNames: Set<String> =
        imports.toSet() + QUALIFIED_NAME.findAll(PACKAGE_STATEMENT.replace(code, "")).map { it.value }.toSet()

    val declarations: List<Declaration> = DECLARATION.findAll(content).map {
        val modifiers = it.groupValues[1].split(Regex("\\s+"))
        Declaration(
            kind = it.groupValues[2],
            name = it.groupValues[3],
            visibility = listOf("internal", "private", "protected").firstOrNull(modifiers::contains) ?: "public",
        )
    }.toList()

    private companion object {
        val PACKAGE = Regex("^package\\s+([\\w.`]+)", RegexOption.MULTILINE)
        val PACKAGE_STATEMENT = Regex("^[ \\t]*package[ \\t]+[\\w.`]+", RegexOption.MULTILINE)
        val IMPORT = Regex("(?:^|;)[ \\t]*import[ \\t]+([\\w.`*]+)", RegexOption.MULTILINE)
        val QUALIFIED_NAME = Regex("(?<![\\w.`])(?:app\\.thunder|org\\.springframework|jakarta)(?:\\.\\w+)+")
        val DECLARATION = Regex(
            "^(?:@\\w+(?:\\([^)\\n]*\\))?\\s+)*" +
                "((?:(?:public|internal|private|protected|data|sealed|enum|value|abstract|open|annotation|inner|fun|inline)[ \\t]+)*)" +
                "(class|object|interface|typealias)[ \\t]+(\\w+)",
            RegexOption.MULTILINE,
        )
    }
}
