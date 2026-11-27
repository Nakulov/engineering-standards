package io.github.nakulov.standards.task

import java.io.File

/** Источник и копия, у которых блоки [prefix] — копии 1 в 1: [prefix] — имя блока или его часть до `:`. */
private class Copies(val source: String, val copy: String, val prefix: String)

/** Правило плагина → агенты с его копией: правила плагина грузятся только основной сессии. */
private val RULE_COPIES = mapOf(
    "bash-commands" to listOf("plan-executor", "implementation-reviewer", "test-reviewer", "standards-reviewer"),
    "bank" to listOf("plan-executor", "standards-reviewer"),
)

private val COPIES = listOf(
    Copies(source = "skills/decompose/SKILL.md", copy = "agents/plan-reviewer.md", prefix = "plan"),
    Copies(source = "skills/execute/SKILL.md", copy = "agents/plan-executor.md", prefix = "execute"),
) + RULE_COPIES.flatMap { (rule, agents) ->
    agents.map { agent -> Copies(source = "rules/$rule.md", copy = "agents/$agent.md", prefix = "rules:$rule") }
} + listOf("implementation-reviewer", "test-reviewer", "standards-reviewer").map { reviewer ->
    Copies(source = "agents/plan-executor.md", copy = "agents/$reviewer.md", prefix = "agents")
}

// Блок: `<!-- >>> <префикс>:<имя> -->` + перевод строки + текст + `<!-- <<< <префикс>:<имя> -->`; `\1` — то же имя.
private val BLOCK = Regex(
    pattern = "<!-- >>> ([a-z]+:[a-z-]+) -->\\n(.*?)<!-- <<< \\1 -->",
    option = RegexOption.DOT_MATCHES_ALL,
)

/** Имена блоков, чей текст в источнике и копии расходится или есть только в одном файле. */
fun findMarkerMismatches(): List<String> = COPIES.flatMap { copies ->
    val source = blocks(path = copies.source, prefix = copies.prefix)
    val copy = blocks(path = copies.copy, prefix = copies.prefix)
    (source.keys + copy.keys).sorted().filter { name -> source[name] != copy[name] }
}

/** Имена блоков всех источников по алфавиту, без повторов — пустой источник не даёт ложно-зелёного сравнения. */
fun findSourceMarkers(): List<String> =
    COPIES.flatMap { copies -> blocks(path = copies.source, prefix = copies.prefix).keys }.distinct().sorted()

private fun blocks(path: String, prefix: String): Map<String, String> {
    val text = File(System.getProperty("task.repoRoot"), path).readText()
    return BLOCK.findAll(input = text)
        .associate { match -> match.groupValues[1] to match.groupValues[2] }
        .filterKeys { name -> name == prefix || name.startsWith(prefix = "$prefix:") }
}
