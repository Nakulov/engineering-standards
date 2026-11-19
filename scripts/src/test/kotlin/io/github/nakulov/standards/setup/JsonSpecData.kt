package io.github.nakulov.standards.setup

/** Текст в формате печати: ключи не по алфавиту, вложенность, все типы, кириллица, экранирование. */
fun makeFormattedJson(): String {
    return """
        {
          "model": "opus",
          "permissions": {
            "allow": [
              "Bash(./gradlew:*)",
              "Bash(task:*)"
            ],
            "deny": []
          },
          "enabledPlugins": {
            "b@m": true,
            "a@m": false
          },
          "заметка": "строка с \"кавычками\" и \\",
          "limits": [
            1,
            -2.5e3,
            null
          ]
        }
    """.trimIndent()
}

/** Тот же JSON в одну строку, без отступов. */
fun makeCompactJson(): String {
    return "{\"model\":\"opus\",\"permissions\":{\"allow\":[\"Bash(./gradlew:*)\",\"Bash(task:*)\"],\"deny\":[]}," +
        "\"enabledPlugins\":{\"b@m\":true,\"a@m\":false},\"заметка\":\"строка с \\\"кавычками\\\" и \\\\\"," +
        "\"limits\":[1,-2.5e3,null]}"
}

fun makeParsedValue(): JsonValue {
    val items = listOf(JsonBoolean(value = true), JsonNull, JsonNumber(raw = "-2.5e3"))
    return JsonObject(fields = linkedMapOf("b" to JsonNumber(raw = "1"), "a" to JsonArray(items = items)))
}
