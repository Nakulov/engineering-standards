package io.github.nakulov.standards.setup

import java.io.File

private const val SETTINGS = ".claude/settings.json"

fun writeSettings(
    root: File,
    text: String,
) {
    val file = File(root, SETTINGS)
    file.parentFile.mkdirs()
    file.writeText(text)
}

fun readSettings(root: File): String {
    return File(root, SETTINGS).readText()
}

fun makeSettingsFinding(
    status: Finding.Status,
    text: String,
): Finding {
    return Finding(status = status, area = "подключение", text = "$SETTINGS: $text")
}

fun makeFullSettings(): String {
    return makeStandardKeys(enabled = "true", source = makeGitHubSource())
}

fun makeDisabledSettings(): String {
    return makeStandardKeys(enabled = "false", source = makeGitHubSource())
}

fun makeOtherSourceSettings(): String {
    return makeStandardKeys(enabled = "true", source = listOf("\"source\": \"directory\"", "\"path\": \"/tmp/standards\""))
}

private fun makeGitHubSource(): List<String> {
    return listOf("\"source\": \"github\"", "\"repo\": \"Nakulov/engineering-standards\"")
}

/** Три ключа стандарта в формате печати; [source] — две строки источника маркетплейса. */
private fun makeStandardKeys(
    enabled: String,
    source: List<String>,
): String {
    val lines = listOf(
        "{",
        "  \"extraKnownMarketplaces\": {",
        "    \"engineering-standards\": {",
        "      \"source\": {",
        "        ${source[0]},",
        "        ${source[1]}",
        "      }",
        "    }",
        "  },",
        "  \"enabledPlugins\": {",
        "    \"engineering-standards@engineering-standards\": $enabled",
        "  },",
        "  \"permissions\": {",
        "    \"allow\": [",
        "      \"Bash(task:*)\"",
        "    ]",
        "  }",
        "}",
    )
    return lines.joinToString(separator = "\n", postfix = "\n")
}

/** Ключи проекта: модель и своё правило разрешения. */
fun makeProjectSettings(): String {
    return """
        {
          "model": "opus",
          "permissions": {
            "allow": [
              "Bash(./gradlew:*)"
            ]
          }
        }
    """.trimIndent() + "\n"
}

/** Ключи проекта на месте, ключи стандарта — в конце объекта, разрешение — в массиве проекта. */
fun makeProjectSettingsWithStandard(): String {
    return """
        {
          "model": "opus",
          "permissions": {
            "allow": [
              "Bash(./gradlew:*)",
              "Bash(task:*)"
            ]
          },
          "extraKnownMarketplaces": {
            "engineering-standards": {
              "source": {
                "source": "github",
                "repo": "Nakulov/engineering-standards"
              }
            }
          },
          "enabledPlugins": {
            "engineering-standards@engineering-standards": true
          }
        }
    """.trimIndent() + "\n"
}

fun makePermissionOnlySettings(): String {
    return """
        {
          "permissions": {
            "allow": [
              "Bash(task:*)"
            ]
          }
        }
    """.trimIndent() + "\n"
}

fun makeBrokenSettings(): String {
    return "{\n  \"model\": \"opus\",\n  oops\n}\n"
}

fun makeSettingsAddedFindings(): List<Finding> {
    return listOf(
        makeSettingsFinding(
            status = Finding.Status.FIXED,
            text = "маркетплейс engineering-standards добавлен — закоммитьте через задачу",
        ),
        makeSettingsFinding(
            status = Finding.Status.FIXED,
            text = "плагин engineering-standards@engineering-standards включён — закоммитьте через задачу",
        ),
        makeSettingsFinding(
            status = Finding.Status.FIXED,
            text = "разрешение Bash(task:*) добавлено — закоммитьте через задачу",
        ),
    )
}

fun makeSettingsMissingFindings(): List<Finding> {
    return listOf(
        makeSettingsFinding(status = Finding.Status.DEVIATION, text = "нет маркетплейса engineering-standards"),
        makeSettingsFinding(
            status = Finding.Status.DEVIATION,
            text = "плагин engineering-standards@engineering-standards не включён",
        ),
        makeSettingsFinding(status = Finding.Status.DEVIATION, text = "нет разрешения Bash(task:*)"),
    )
}

fun makeSettingsUnchangedFindings(): List<Finding> {
    return listOf(
        makeSettingsFinding(status = Finding.Status.OK, text = "маркетплейс engineering-standards"),
        makeSettingsFinding(
            status = Finding.Status.OK,
            text = "плагин engineering-standards@engineering-standards включён",
        ),
        makeSettingsFinding(status = Finding.Status.OK, text = "разрешение Bash(task:*)"),
    )
}

fun makeDisabledFindings(): List<Finding> {
    val unchanged = makeSettingsUnchangedFindings()
    val disabled = makeSettingsFinding(
        status = Finding.Status.DEVIATION,
        text = "плагин engineering-standards@engineering-standards выключен проектом (false) — " +
            "стандарт не переписывает решение проекта",
    )
    return listOf(unchanged[0], disabled, unchanged[2])
}

fun makeOtherSourceFindings(): List<Finding> {
    val unchanged = makeSettingsUnchangedFindings()
    val other = makeSettingsFinding(
        status = Finding.Status.NEEDS_CONFIRM,
        text = "источник маркетплейса engineering-standards отличается: " +
            "{ \"source\": { \"source\": \"directory\", \"path\": \"/tmp/standards\" } } — " +
            "заменит на github Nakulov/engineering-standards",
    )
    return listOf(other, unchanged[1], unchanged[2])
}

fun makeStandardRepositoryFindings(): List<Finding> {
    return listOf(
        makeSettingsFinding(
            status = Finding.Status.NOTE,
            text = "репозиторий стандарта — маркетплейс и enabledPlugins не пишутся: " +
                "подключение через claude --plugin-dir .",
        ),
        makeSettingsFinding(
            status = Finding.Status.FIXED,
            text = "разрешение Bash(task:*) добавлено — закоммитьте через задачу",
        ),
    )
}
