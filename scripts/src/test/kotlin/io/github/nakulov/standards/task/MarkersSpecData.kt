package io.github.nakulov.standards.task

import java.io.File

/** Скилл и агент, у которых блоки `<префикс>:*` — копии 1 в 1. */
private class Copies(val skill: String, val agent: String, val prefix: String)

private val COPIES = listOf(
    Copies(skill = "skills/decompose/SKILL.md", agent = "agents/plan-reviewer.md", prefix = "plan"),
    Copies(skill = "skills/execute/SKILL.md", agent = "agents/plan-executor.md", prefix = "execute"),
)

// Блок: `<!-- >>> <префикс>:<имя> -->` + перевод строки + текст + `<!-- <<< <префикс>:<имя> -->`; `\1` — то же имя.
private val BLOCK = Regex(
    pattern = "<!-- >>> ([a-z]+:[a-z-]+) -->\\n(.*?)<!-- <<< \\1 -->",
    option = RegexOption.DOT_MATCHES_ALL,
)

/** Имена блоков, чей текст в скилле и агенте расходится или есть только в одном файле. */
fun findMarkerMismatches(): List<String> = COPIES.flatMap { copies ->
    val skill = blocks(path = copies.skill, prefix = copies.prefix)
    val agent = blocks(path = copies.agent, prefix = copies.prefix)
    (skill.keys + agent.keys).sorted().filter { name -> skill[name] != agent[name] }
}

/** Имена блоков всех скиллов по алфавиту — пустой скилл не даёт ложно-зелёного сравнения. */
fun findSkillMarkers(): List<String> =
    COPIES.flatMap { copies -> blocks(path = copies.skill, prefix = copies.prefix).keys }.sorted()

private fun blocks(path: String, prefix: String): Map<String, String> {
    val text = File(System.getProperty("task.repoRoot"), path).readText()
    return BLOCK.findAll(input = text)
        .associate { match -> match.groupValues[1] to match.groupValues[2] }
        .filterKeys { name -> name.startsWith(prefix = "$prefix:") }
}
