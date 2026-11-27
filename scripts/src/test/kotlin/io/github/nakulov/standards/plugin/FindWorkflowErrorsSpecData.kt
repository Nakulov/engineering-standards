package io.github.nakulov.standards.plugin

const val WORKFLOW_WITH_ALL_SKILLS = "1. `/groom <N>` — груминг. 2. `/decompose <N>` — план."
const val WORKFLOW_WITH_UNKNOWN_COMMAND = "1. `/groom <N>`. 2. `/decompose <N>`. 3. `/plan <N>`."
const val WORKFLOW_WITHOUT_DECOMPOSE = "1. `/groom <N>` — груминг."
const val WORKFLOW_WITH_REPEATED_UNKNOWN_COMMAND = "`/groom <N>`, `/decompose <N>`, `/plan <N>` и снова `/plan <тема>`."
const val WORKFLOW_WITH_TASK_COMMAND = "`/groom <N>`, `/decompose <N>`, затем `task -C .worktrees/<slug> done`."
const val WORKFLOW_WITH_PATHS = "`/groom <N>`, `/decompose <N>`, файл `docs/commands/README.md` и путь /etc/hosts."
const val WORKFLOW_WITH_UNKNOWN_COMMAND_WITHOUT_DECOMPOSE = "1. `/groom <N>`. 2. `/plan <N>`."
const val EMPTY_WORKFLOW = ""
const val UNKNOWN_COMMAND_ERROR = "`/plan` в docs/workflow.md — нет skills/plan/SKILL.md"
const val UNKNOWN_GROOM_ERROR = "`/groom` в docs/workflow.md — нет skills/groom/SKILL.md"
const val UNKNOWN_DECOMPOSE_ERROR = "`/decompose` в docs/workflow.md — нет skills/decompose/SKILL.md"
const val MISSING_DECOMPOSE_ERROR = "скилл decompose не упомянут в docs/workflow.md"
const val MISSING_GROOM_ERROR = "скилл groom не упомянут в docs/workflow.md"

fun makeSkills(): List<String> {
    return listOf("decompose", "groom")
}

fun makeUnknownCommandErrors(): List<String> {
    return listOf(UNKNOWN_COMMAND_ERROR)
}

fun makeMissingDecomposeErrors(): List<String> {
    return listOf(MISSING_DECOMPOSE_ERROR)
}

fun makeMixedErrors(): List<String> {
    return listOf(UNKNOWN_COMMAND_ERROR, MISSING_DECOMPOSE_ERROR)
}

fun makeAllSkillsMissingErrors(): List<String> {
    return listOf(MISSING_DECOMPOSE_ERROR, MISSING_GROOM_ERROR)
}

fun makeAllCommandsUnknownErrors(): List<String> {
    return listOf(UNKNOWN_GROOM_ERROR, UNKNOWN_DECOMPOSE_ERROR)
}
