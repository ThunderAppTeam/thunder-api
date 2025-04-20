package app.thunder.testing.architecture

internal object CodeSanitizer {
    private enum class Mode { CODE, STRING, RAW }

    private class Frame(val mode: Mode, var depth: Int = 0)

    fun strip(source: String): String {
        val out = StringBuilder(source.length)
        val stack = ArrayDeque<Frame>().apply { addLast(Frame(Mode.CODE)) }
        var i = 0
        while (i < source.length) {
            val top = stack.last()
            val c = source[i]
            when (top.mode) {
                Mode.CODE -> i = stepCode(source, i, c, top, stack, out)
                Mode.STRING -> i = stepString(source, i, c, stack, out)
                Mode.RAW -> i = stepRaw(source, i, c, stack, out)
            }
        }
        return out.toString()
    }

    private fun stepCode(source: String, i: Int, c: Char, top: Frame, stack: ArrayDeque<Frame>, out: StringBuilder): Int {
        val inTemplate = stack.size > 1
        return when {
            source.startsWith("//", i) -> skipLineComment(source, i)
            source.startsWith("/*", i) -> skipBlockComment(source, i, out)
            source.startsWith("\"\"\"", i) -> {
                stack.addLast(Frame(Mode.RAW))
                i + 3
            }
            c == '"' -> {
                stack.addLast(Frame(Mode.STRING))
                i + 1
            }
            c == '\'' -> skipCharLiteral(source, i, out)
            c == '{' && inTemplate -> {
                top.depth++
                i + 1
            }
            c == '}' && inTemplate -> {
                if (top.depth == 0) stack.removeLast() else top.depth--
                i + 1
            }
            else -> {
                out.append(c)
                i + 1
            }
        }
    }

    private fun stepString(source: String, i: Int, c: Char, stack: ArrayDeque<Frame>, out: StringBuilder): Int = when {
        c == '\\' -> i + 2
        c == '"' -> {
            stack.removeLast()
            i + 1
        }
        c == '\n' -> {
            stack.removeLast()
            out.append('\n')
            i + 1
        }
        c == '$' && source.startsWith("\${", i) -> {
            stack.addLast(Frame(Mode.CODE))
            i + 2
        }
        else -> i + 1
    }

    private fun stepRaw(source: String, i: Int, c: Char, stack: ArrayDeque<Frame>, out: StringBuilder): Int = when {
        source.startsWith("\"\"\"", i) -> {
            var end = i + 3
            while (end < source.length && source[end] == '"') end++
            stack.removeLast()
            end
        }
        c == '\n' -> {
            out.append('\n')
            i + 1
        }
        c == '$' && source.startsWith("\${", i) -> {
            stack.addLast(Frame(Mode.CODE))
            i + 2
        }
        else -> i + 1
    }

    private fun skipLineComment(source: String, i: Int): Int {
        val end = source.indexOf('\n', i)
        return if (end < 0) source.length else end
    }

    private fun skipBlockComment(source: String, start: Int, out: StringBuilder): Int {
        var depth = 0
        var i = start
        while (i < source.length) {
            when {
                source.startsWith("/*", i) -> {
                    depth++
                    i += 2
                }
                source.startsWith("*/", i) -> {
                    depth--
                    i += 2
                    if (depth == 0) {
                        out.append(' ')
                        return i
                    }
                }
                else -> {
                    if (source[i] == '\n') out.append('\n')
                    i++
                }
            }
        }
        return i
    }

    private fun skipCharLiteral(source: String, start: Int, out: StringBuilder): Int {
        var i = start + 1
        while (i < source.length && source[i] != '\n') {
            when (source[i]) {
                '\\' -> i += 2
                '\'' -> return i + 1
                else -> i++
            }
        }
        out.append(source[start])
        return start + 1
    }
}
