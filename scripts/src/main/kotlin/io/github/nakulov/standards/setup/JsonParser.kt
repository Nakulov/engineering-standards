package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError

/**
 * Разбор JSON в [JsonValue] (#43): объекты, массивы, строки, числа, true/false/null. Ошибка — [TaskError.Failure] с
 * файлом и строкой: неразобранный файл `setup` не перезаписывает. Комментариев JSON не допускает — тоже ошибка.
 */
class JsonParser(
    private val text: String,
    private val file: String,
) {
    private var position = 0

    fun parse(): JsonValue {
        val value = value()
        skipSpace()
        if (position < text.length) {
            fail(message = "ожидается конец файла")
        }
        return value
    }

    private fun value(): JsonValue {
        skipSpace()
        val char = text.getOrNull(index = position) ?: fail(message = "ожидается значение")
        return when {
            char == '{' -> objectValue()
            char == '[' -> arrayValue()
            char == '"' -> JsonString(value = string())
            char == '/' -> fail(message = "комментарии JSON не поддерживает — уберите их")
            char == '-' || char.isDigit() -> number()
            else -> literal()
        }
    }

    private fun objectValue(): JsonObject {
        position++
        val fields = linkedMapOf<String, JsonValue>()
        skipSpace()
        var closed = text.getOrNull(index = position) == '}'
        if (closed) {
            position++
        }
        while (!closed) {
            skipSpace()
            if (text.getOrNull(index = position) != '"') {
                fail(message = "ожидается ключ в кавычках")
            }
            val key = string()
            skipSpace()
            if (text.getOrNull(index = position) != ':') {
                fail(message = "ожидается «:» после ключа $key")
            }
            position++
            fields[key] = value()
            closed = separator(close = '}')
        }
        return JsonObject(fields = fields)
    }

    private fun arrayValue(): JsonArray {
        position++
        val items = mutableListOf<JsonValue>()
        skipSpace()
        var closed = text.getOrNull(index = position) == ']'
        if (closed) {
            position++
        }
        while (!closed) {
            items += value()
            closed = separator(close = ']')
        }
        return JsonArray(items = items)
    }

    /** После элемента: `,` — дальше, [close] — конец; `true`, если контейнер закрыт. */
    private fun separator(close: Char): Boolean {
        skipSpace()
        val char = text.getOrNull(index = position)
        if (char != ',' && char != close) {
            fail(message = "ожидается «,» или «$close»")
        }
        position++
        return char == close
    }

    private fun string(): String {
        position++
        val builder = StringBuilder()
        var char = next()
        while (char != '"') {
            when {
                char == '\\' -> builder.append(escape())
                char < ' ' -> fail(message = "перевод строки внутри строки — экранируйте \\n")
                else -> builder.append(char)
            }
            char = next()
        }
        return builder.toString()
    }

    private fun next(): Char {
        val char = text.getOrNull(index = position) ?: fail(message = "строка без закрывающей кавычки")
        position++
        return char
    }

    private fun escape(): Char {
        val char = next()
        return when (char) {
            '"', '\\', '/' -> char
            'n' -> '\n'
            't' -> '\t'
            'r' -> '\r'
            'b' -> '\b'
            'f' -> '\u000C'
            'u' -> unicode()
            else -> fail(message = "неизвестное экранирование \\$char")
        }
    }

    private fun unicode(): Char {
        val hex = text.substring(startIndex = position, endIndex = minOf(position + HEX_LENGTH, text.length))
        if (hex.length < HEX_LENGTH || !hex.all { char -> char in HEX_DIGITS }) {
            fail(message = "ожидается \\uXXXX")
        }
        position += HEX_LENGTH
        return hex.toInt(radix = 16).toChar()
    }

    private fun number(): JsonNumber {
        val match = NUMBER.matchAt(input = text, index = position) ?: fail(message = "ожидается число")
        position = match.range.last + 1
        return JsonNumber(raw = match.value)
    }

    private fun literal(): JsonValue {
        val word = LITERALS.keys.firstOrNull { word -> text.startsWith(prefix = word, startIndex = position) }
            ?: fail(message = "ожидается значение")
        position += word.length
        return LITERALS.getValue(key = word)
    }

    private fun skipSpace() {
        while (position < text.length && text[position] in SPACE) {
            position++
        }
    }

    private fun fail(message: String): Nothing {
        val line = text.take(n = position).count { char -> char == '\n' } + 1
        throw TaskError.Failure(message = "$file:$line: $message")
    }

    private companion object {
        const val HEX_LENGTH = 4
        const val HEX_DIGITS = "0123456789abcdefABCDEF"
        const val SPACE = " \t\r\n"
        val NUMBER = Regex(pattern = "-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
        val LITERALS = mapOf("true" to JsonBoolean(value = true), "false" to JsonBoolean(value = false), "null" to JsonNull)
    }
}
