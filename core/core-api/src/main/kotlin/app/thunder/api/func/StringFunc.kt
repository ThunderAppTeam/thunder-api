package app.thunder.api.func

fun String.nullIfBlank(): String? {
    return this.takeIf { it.isNotBlank() }
}