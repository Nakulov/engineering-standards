package io.github.nakulov.standards.task.command

import java.io.File
import java.nio.file.Files

const val PLAN_BRANCH = "long/53-commit-commands"
const val PLAN_LINE = "План: ветка `long/53-commit-commands`, `plans/commit-commands.md`"

/** Корень репозитория с `plans/commit-commands.md` и `parts.md`, общий `.git` — во временной папке модуля. */
fun makePlanRoot(): File {
    val base = File(System.getProperty("task.cache"), "plan")
    base.mkdirs()
    val root = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
    File(root, "plans").mkdirs()
    File(root, "plans/commit-commands.md").writeText("# Команды коммита\n")
    File(root, "parts.md").writeText("## Отклонено\n- вариант — причина\n")
    File(root, ".git").mkdirs()
    return root
}

fun makePlanArgs(): List<String> {
    return listOf("53", "commit-commands", "--long", "--issue-parts", "parts.md")
}

fun makePlanIssueBody(): String {
    return "## Проблема\nТекст.\n\n**Отклонено:**\n- груминг\n"
}

fun makePlanTemp(root: File): String {
    return File(File(root, ".git"), "task-plan/commit-commands").path
}
