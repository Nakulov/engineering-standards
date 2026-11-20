package io.github.nakulov.standards.task

import java.io.File

private const val SKILL = "skills/decompose/SKILL.md"
private const val AGENT = "agents/plan-reviewer.md"

// Блок: `<!-- >>> plan:<имя> -->` + перевод строки + текст + `<!-- <<< plan:<имя> -->`; `\1` — то же имя.
private val BLOCK = Regex(
    pattern = "<!-- >>> (plan:[a-z-]+) -->\\n(.*?)<!-- <<< \\1 -->",
    option = RegexOption.DOT_MATCHES_ALL,
)

/** Имена блоков `plan:*`, чей текст в скилле и агенте ревью расходится или есть только в одном файле. */
fun findMarkerMismatches(): List<String> {
    val skill = blocks(path = SKILL)
    val agent = blocks(path = AGENT)
    return (skill.keys + agent.keys).sorted().filter { name -> skill[name] != agent[name] }
}

/** Имена блоков `plan:*` скилла по алфавиту — пустой скилл не даёт ложно-зелёного сравнения. */
fun findSkillMarkers(): List<String> = blocks(path = SKILL).keys.sorted()

private fun blocks(path: String): Map<String, String> {
    val text = File(System.getProperty("task.repoRoot"), path).readText()
    return BLOCK.findAll(input = text).associate { match -> match.groupValues[1] to match.groupValues[2] }
}
