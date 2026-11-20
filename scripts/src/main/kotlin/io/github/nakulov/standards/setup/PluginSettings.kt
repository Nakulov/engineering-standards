package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import java.io.File

/**
 * Подключение плагина в `.claude/settings.json` проекта (#43, решения 3 и 5): стандарт владеет тремя ключами —
 * маркетплейс, `enabledPlugins` и разрешения `Bash(task:*)` и `Edit` на журналы плана в `plans/journal/` (#98:
 * журналы и сообщение коммита — без запроса доступа); недостающие разрешения дописываются, остальные ключи и
 * правила проекта не трогаются. Явный `false` проекта не переписывается — стандарт не отменяет решение проекта (#1).
 * Другой источник маркетплейса заменяется только с `--confirm`. В репозитории самого стандарта плагин подключается
 * через `--plugin-dir` — пишется только разрешение (решение 2).
 */
class PluginSettings(
    private val git: Git,
    private val pluginRoot: File,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val root = File(git.root())
        val file = File(root, SETTINGS)
        // Пустой файл (`touch`) — как пустой объект: ключи стандарта дописываются, а не сбой.
        val text = if (file.isFile) file.readText() else ""
        val parsed = if (text.isBlank()) JsonObject(fields = emptyMap()) else JsonParser(text = text, file = SETTINGS).parse()
        val settings = parsed as? JsonObject
            ?: throw TaskError.Failure(message = "$SETTINGS: ожидается объект {…} на верхнем уровне")
        val fields = LinkedHashMap(settings.fields)
        val plugin = if (pluginRoot.canonicalFile == root.canonicalFile) {
            listOf(finding(status = Finding.Status.NOTE, text = STANDARD_REPOSITORY))
        } else {
            listOf(marketplace(fields = fields, mode = mode), enabled(fields = fields, mode = mode))
        }
        val findings = plugin + permissions(fields = fields, mode = mode)
        if (fields != settings.fields) {
            file.parentFile.mkdirs()
            file.writeText(JsonObject(fields = fields).toText() + "\n")
        }
        return findings
    }

    private fun marketplace(
        fields: MutableMap<String, JsonValue>,
        mode: Setup.Mode,
    ): Finding {
        val marketplaces = objectAt(fields = fields, key = MARKETPLACES)
        val current = marketplaces.fields[NAME]
        if (current == MARKETPLACE) {
            return finding(status = Finding.Status.OK, text = "маркетплейс $NAME")
        }
        val was = current?.let { value -> compact(value = value) }
        val (status, text) = when {
            was == null && !mode.apply -> Finding.Status.DEVIATION to "нет маркетплейса $NAME"
            was == null -> Finding.Status.FIXED to "маркетплейс $NAME добавлен$COMMIT"
            !mode.apply -> Finding.Status.DEVIATION to "источник маркетплейса $NAME отличается: $was"
            !mode.confirmed -> Finding.Status.NEEDS_CONFIRM to
                "источник маркетплейса $NAME отличается: $was — заменит на github $REPOSITORY"
            else -> Finding.Status.FIXED to "источник маркетплейса $NAME заменён, было: $was$COMMIT"
        }
        if (status == Finding.Status.FIXED) {
            fields[MARKETPLACES] = JsonObject(fields = marketplaces.fields + (NAME to MARKETPLACE))
        }
        return finding(status = status, text = text)
    }

    private fun enabled(
        fields: MutableMap<String, JsonValue>,
        mode: Setup.Mode,
    ): Finding {
        val plugins = objectAt(fields = fields, key = ENABLED)
        return when {
            plugins.fields[PLUGIN] == JsonBoolean(value = true) ->
                finding(status = Finding.Status.OK, text = "плагин $PLUGIN включён")
            plugins.fields[PLUGIN] == JsonBoolean(value = false) -> finding(
                status = Finding.Status.DEVIATION,
                text = "плагин $PLUGIN выключен проектом (false) — стандарт не переписывает решение проекта",
            )
            !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "плагин $PLUGIN не включён")
            else -> {
                fields[ENABLED] = JsonObject(fields = plugins.fields + (PLUGIN to JsonBoolean(value = true)))
                finding(status = Finding.Status.FIXED, text = "плагин $PLUGIN включён$COMMIT")
            }
        }
    }

    private fun permissions(
        fields: MutableMap<String, JsonValue>,
        mode: Setup.Mode,
    ): List<Finding> {
        val permissions = objectAt(fields = fields, key = PERMISSIONS)
        val allow = permissions.fields[ALLOW] ?: JsonArray(items = emptyList())
        val items = (allow as? JsonArray)?.items
            ?: throw TaskError.Failure(message = "$SETTINGS: $PERMISSIONS.$ALLOW — ожидается массив […]")
        val missing = RULES.filter { rule -> JsonString(value = rule) !in items }
        if (mode.apply && missing.isNotEmpty()) {
            val updated = JsonArray(items = items + missing.map { rule -> JsonString(value = rule) })
            fields[PERMISSIONS] = JsonObject(fields = permissions.fields + (ALLOW to updated))
        }
        return RULES.map { rule ->
            when {
                rule !in missing -> finding(status = Finding.Status.OK, text = "разрешение $rule")
                !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "нет разрешения $rule")
                else -> finding(status = Finding.Status.FIXED, text = "разрешение $rule добавлено$COMMIT")
            }
        }
    }

    private fun objectAt(
        fields: Map<String, JsonValue>,
        key: String,
    ): JsonObject {
        val value = fields[key] ?: return JsonObject(fields = emptyMap())
        return value as? JsonObject ?: throw TaskError.Failure(message = "$SETTINGS: $key — ожидается объект {…}")
    }

    private fun compact(value: JsonValue): String {
        return value.toText().lines().joinToString(separator = " ") { line -> line.trim() }
    }

    private fun finding(
        status: Finding.Status,
        text: String,
    ): Finding {
        return Finding(status = status, area = AREA, text = "$SETTINGS: $text")
    }

    private companion object {
        const val AREA = "подключение"
        const val SETTINGS = ".claude/settings.json"
        const val NAME = "engineering-standards"
        const val PLUGIN = "engineering-standards@engineering-standards"
        const val REPOSITORY = "Nakulov/engineering-standards"
        const val MARKETPLACES = "extraKnownMarketplaces"
        const val ENABLED = "enabledPlugins"
        const val PERMISSIONS = "permissions"
        const val ALLOW = "allow"
        val RULES = listOf("Bash(task:*)", "Edit(/plans/journal/**)")
        const val COMMIT = " — закоммитьте через задачу"
        const val STANDARD_REPOSITORY =
            "репозиторий стандарта — маркетплейс и enabledPlugins не пишутся: подключение через claude --plugin-dir ."
        val MARKETPLACE = JsonObject(
            fields = mapOf(
                "source" to JsonObject(
                    fields = mapOf("source" to JsonString(value = "github"), "repo" to JsonString(value = REPOSITORY)),
                ),
            ),
        )
    }
}
