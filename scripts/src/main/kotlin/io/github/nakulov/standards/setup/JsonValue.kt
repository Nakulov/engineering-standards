package io.github.nakulov.standards.setup

/**
 * Значение JSON для `.claude/settings.json` (#43): свой формат без библиотек — запускатель компилирует голым `kotlinc`
 * (#13). Объект хранит порядок ключей, число — исходный текст: после записи файл отличается только правкой стандарта.
 */
sealed interface JsonValue {
    /** Текст с отступом 2, без перевода строки в конце. */
    fun toText(): String {
        return render(value = this, indent = "")
    }
}

data class JsonObject(
    val fields: Map<String, JsonValue>,
) : JsonValue

data class JsonArray(
    val items: List<JsonValue>,
) : JsonValue

data class JsonString(
    val value: String,
) : JsonValue

data class JsonNumber(
    val raw: String,
) : JsonValue

data class JsonBoolean(
    val value: Boolean,
) : JsonValue

data object JsonNull : JsonValue

private const val STEP = "  "

private fun render(
    value: JsonValue,
    indent: String,
): String {
    val inner = indent + STEP
    return when (value) {
        is JsonObject -> if (value.fields.isEmpty()) {
            "{}"
        } else {
            value.fields.entries.joinToString(separator = ",\n", prefix = "{\n", postfix = "\n$indent}") { entry ->
                "$inner${quote(text = entry.key)}: ${render(value = entry.value, indent = inner)}"
            }
        }
        is JsonArray -> if (value.items.isEmpty()) {
            "[]"
        } else {
            value.items.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n$indent]") { item ->
                inner + render(value = item, indent = inner)
            }
        }
        is JsonString -> quote(text = value.value)
        is JsonNumber -> value.raw
        is JsonBoolean -> value.value.toString()
        JsonNull -> "null"
    }
}

private fun quote(text: String): String {
    val builder = StringBuilder("\"")
    text.forEach { char ->
        when {
            char == '"' -> builder.append("\\\"")
            char == '\\' -> builder.append("\\\\")
            char == '\n' -> builder.append("\\n")
            char == '\r' -> builder.append("\\r")
            char == '\t' -> builder.append("\\t")
            char < ' ' -> builder.append("\\u%04x".format(char.code))
            else -> builder.append(char)
        }
    }
    return builder.append('"').toString()
}
