package io.github.nakulov.standards.plugin

// Команда скилла — `/<имя>` в начале кода в обратных кавычках: группа 1 — имя.
private const val COMMAND_PATTERN = "`/([a-z][a-z-]*)"

// Ошибки памятки `docs/workflow.md` против имён скиллов: команда без скилла и скилл, не упомянутый командой.
// Порядок: команды — по первому появлению в тексте, скиллы — по списку.
fun findWorkflowErrors(
    workflow: String,
    skills: List<String>,
): List<String> {
    val commands = Regex(pattern = COMMAND_PATTERN).findAll(input = workflow)
        .map { match -> match.groupValues[1] }
        .distinct()
        .toList()
    val unknownCommands = commands.filterNot { command -> command in skills }
        .map { command -> "`/$command` в docs/workflow.md — нет skills/$command/SKILL.md" }
    val missingSkills = skills.filterNot { skill -> skill in commands }
        .map { skill -> "скилл $skill не упомянут в docs/workflow.md" }
    return unknownCommands + missingSkills
}
