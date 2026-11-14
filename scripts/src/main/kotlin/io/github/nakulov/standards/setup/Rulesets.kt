package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import java.io.File

/**
 * Защита ruleset'ами (#8, решение 6): `dev`, long-term ветки `long/…`, теги `v…`. Ruleset задаётся шаблоном имени
 * и существует до появления ветки; защита тегов есть только у ruleset'ов. Обязательная проверка `check` входит
 * в ruleset, только когда её прогон уже есть на `dev`: без CI она повесила бы все PR. Классическая защита `dev`
 * удаляется после ruleset'а и только с `--confirm`. Приватный репозиторий на бесплатном плане — «недоступно» по
 * ответу 403 (#8, решение 11).
 */
class Rulesets(
    private val gitHub: GitHub,
    private val scratch: File,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val rows = try {
            gitHub.listRest(path = "$REPOSITORY/rulesets", jq = ROWS_JQ)
        } catch (error: TaskError.Failure) {
            if (UPGRADE !in error.message.orEmpty()) {
                throw error
            }
            return listOf(Finding(status = Finding.Status.UNAVAILABLE, area = AREA, text = UNAVAILABLE_TEXT))
        }
        val existing = rows.associate { row ->
            row.substringAfter(delimiter = "\t") to row.substringBefore(delimiter = "\t")
        }
        val check = requiredCheck()
        val checks = if (check.status == Finding.Status.OK) listOf(CHECK) else emptyList()
        val synced = standard(checks = checks).map { ruleset ->
            sync(ruleset = ruleset, id = existing[ruleset.name], mode = mode)
        }
        val foreign = existing.keys.filter { name -> name !in NAMES }.map { name ->
            Finding(status = Finding.Status.NOTE, area = AREA, text = "ruleset вне стандарта: $name")
        }
        return listOf(check) + synced + foreign + classic(mode = mode)
    }

    private fun requiredCheck(): Finding {
        val runs = countOrZero(path = "$REPOSITORY/commits/dev/check-runs?check_name=$CHECK", jq = ".total_count")
        if (runs > 0) {
            return Finding(status = Finding.Status.OK, area = AREA, text = "обязательная проверка $CHECK")
        }
        val workflows = countOrZero(path = "$REPOSITORY/actions/workflows", jq = ".total_count")
        if (workflows == 0) {
            val text = "обязательной проверки нет: CI не настроен"
            return Finding(status = Finding.Status.WAITING_CI, area = AREA, text = text)
        }
        return Finding(
            status = Finding.Status.DEVIATION,
            area = AREA,
            text = "workflow есть, а прогона $CHECK на dev нет — обязательная проверка должна называться $CHECK",
        )
    }

    /** Число из ответа; нет ветки `dev` (404, 422) — 0. */
    private fun countOrZero(
        path: String,
        jq: String,
    ): Int {
        val value = try {
            gitHub.read(args = listOf("api", path, "--jq", jq))
        } catch (error: TaskError.Failure) {
            if (MISSING.none { code -> code in error.message.orEmpty() }) {
                throw error
            }
            "0"
        }
        return value.toInt()
    }

    private fun sync(
        ruleset: Ruleset,
        id: String?,
        mode: Setup.Mode,
    ): Finding {
        if (id == null) {
            if (!mode.apply) {
                return Finding(status = Finding.Status.DEVIATION, area = AREA, text = "нет ruleset ${ruleset.name}")
            }
            send(ruleset = ruleset, method = "POST", path = "$REPOSITORY/rulesets")
            return Finding(status = Finding.Status.FIXED, area = AREA, text = "создан ruleset ${ruleset.name}")
        }
        val actual = gitHub.read(args = listOf("api", "$REPOSITORY/rulesets/$id", "--jq", FLATTEN_JQ)).lines().toSet()
        val expected = ruleset.lines()
        val extraRules = actual.filter { line -> line.startsWith(prefix = RULE) && line !in expected }
        val differences = (expected - actual) + extraRules
        if (differences.isEmpty()) {
            return Finding(status = Finding.Status.OK, area = AREA, text = "ruleset ${ruleset.name}")
        }
        if (!mode.apply) {
            val text = "ruleset ${ruleset.name} отличается: ${differences.sorted().joinToString(separator = ", ")}"
            return Finding(status = Finding.Status.DEVIATION, area = AREA, text = text)
        }
        send(ruleset = ruleset, method = "PUT", path = "$REPOSITORY/rulesets/$id")
        return Finding(status = Finding.Status.FIXED, area = AREA, text = "ruleset ${ruleset.name} исправлен")
    }

    /** Тело ruleset'а — файлом: `--input` читает JSON, а у слоя `Shell` нет stdin. */
    private fun send(
        ruleset: Ruleset,
        method: String,
        path: String,
    ) {
        scratch.mkdirs()
        val file = File(scratch, "ruleset-${ruleset.name.substringAfter(delimiter = "/")}.json")
        file.writeText(ruleset.json())
        try {
            gitHub.write(args = listOf("api", "--method", method, path, "--input", file.path))
        } finally {
            file.delete()
        }
    }

    private fun classic(mode: Setup.Mode): List<Finding> {
        val protected = try {
            gitHub.read(args = listOf("api", "$REPOSITORY/branches/dev/protection", "--jq", ".url"))
            true
        } catch (error: TaskError.Failure) {
            if (MISSING.none { code -> code in error.message.orEmpty() }) {
                throw error
            }
            false
        }
        val finding = when {
            !protected -> null
            !mode.apply -> Finding(
                status = Finding.Status.DEVIATION,
                area = AREA,
                text = "классическая защита dev — заменяется ruleset $DEV",
            )
            !mode.confirmed -> Finding(
                status = Finding.Status.NEEDS_CONFIRM,
                area = AREA,
                text = "удалит классическую защиту dev (ruleset $DEV уже действует)",
            )
            else -> {
                gitHub.write(args = listOf("api", "--method", "DELETE", "$REPOSITORY/branches/dev/protection"))
                Finding(status = Finding.Status.FIXED, area = AREA, text = "классическая защита dev удалена")
            }
        }
        return listOfNotNull(finding)
    }

    /**
     * Ruleset стандарта. Значения [rules] — литералы JSON; [checks] — имена обязательных проверок, пусто — без
     * правила `required_status_checks`.
     */
    private data class Ruleset(
        val name: String,
        val target: String,
        val include: String,
        val rules: Map<String, Map<String, String>>,
        val checks: List<String>,
    ) {
        fun json(): String {
            val items = allRules().map { (type, parameters) ->
                val fields = parameters.entries.joinToString(separator = ",") { (key, value) -> "\"$key\":$value" }
                if (parameters.isEmpty()) "{\"type\":\"$type\"}" else "{\"type\":\"$type\",\"parameters\":{$fields}}"
            }
            return "{\"name\":\"$name\",\"target\":\"$target\",\"enforcement\":\"active\",\"bypass_actors\":[]," +
                "\"conditions\":{\"ref_name\":{\"include\":[\"$include\"],\"exclude\":[]}}," +
                "\"rules\":[${items.joinToString(separator = ",")}]}"
        }

        /** Строки сверки в том же виде, что выдаёт [FLATTEN_JQ]. */
        fun lines(): Set<String> {
            val head = setOf("target=$target", "enforcement=active", "include=$include", "bypass=0")
            val body = allRules().flatMap { (type, parameters) ->
                val values = parameters.map { (key, value) ->
                    val shown = if (key == STATUS_CHECKS) checks.joinToString(separator = ",") else value
                    "$type.$key=$shown"
                }
                listOf("$RULE$type") + values
            }
            return head + body
        }

        private fun allRules(): Map<String, Map<String, String>> {
            if (checks.isEmpty()) {
                return rules
            }
            val contexts = checks.joinToString(separator = ",") { check -> "{\"context\":\"$check\"}" }
            val parameters = mapOf("strict_required_status_checks_policy" to "true", STATUS_CHECKS to "[$contexts]")
            return rules + (STATUS_CHECKS to parameters)
        }
    }

    private companion object {
        const val AREA = "защита"
        const val REPOSITORY = "repos/{owner}/{repo}"
        const val CHECK = "check"
        const val DEV = "standards/dev"
        const val LONG = "standards/long"
        const val TAGS = "standards/tags"
        const val RULE = "rule="
        const val STATUS_CHECKS = "required_status_checks"
        const val UPGRADE = "Upgrade to GitHub Pro"
        const val UNAVAILABLE_TEXT = "приватный репозиторий на бесплатном плане — защиты веток и тегов нет"
        const val ROWS_JQ = ".[] | \"\\(.id)\\t\\(.name)\""
        val MISSING = listOf("HTTP 404", "HTTP 422")
        val NAMES = setOf(DEV, LONG, TAGS)
        val PULL_REQUEST = linkedMapOf(
            "required_approving_review_count" to "0",
            "dismiss_stale_reviews_on_push" to "false",
            "require_code_owner_review" to "false",
            "require_last_push_approval" to "false",
            "required_review_thread_resolution" to "true",
            "allowed_merge_methods" to "[\"squash\"]",
        )

        /** Ruleset из ответа API — строками `ключ=значение`; параметры правил — `тип.параметр=JSON`. */
        val FLATTEN_JQ = """
            "target=\(.target)", "enforcement=\(.enforcement)",
            "include=\(.conditions.ref_name.include | join(","))", "bypass=\(.bypass_actors | length)",
            (.rules[] | "rule=\(.type)", (.type as ${'$'}t | (.parameters // {}) | to_entries[]
              | "\(${'$'}t).\(.key)=\(if .key == "required_status_checks"
                then (.value | map(.context) | join(",")) else (.value | tojson) end)"))
        """.trimIndent()

        fun standard(checks: List<String>): List<Ruleset> {
            return listOf(
                Ruleset(
                    name = DEV,
                    target = "branch",
                    include = "refs/heads/dev",
                    rules = mapOf(
                        "deletion" to emptyMap(),
                        "non_fast_forward" to emptyMap(),
                        "pull_request" to PULL_REQUEST,
                    ),
                    checks = checks,
                ),
                Ruleset(
                    name = LONG,
                    target = "branch",
                    include = "refs/heads/long/**",
                    rules = mapOf("non_fast_forward" to emptyMap(), "pull_request" to PULL_REQUEST),
                    checks = checks,
                ),
                Ruleset(
                    name = TAGS,
                    target = "tag",
                    include = "refs/tags/v*",
                    rules = mapOf("deletion" to emptyMap(), "update" to emptyMap(), "non_fast_forward" to emptyMap()),
                    checks = emptyList(),
                ),
            )
        }
    }
}
