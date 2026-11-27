package io.github.nakulov.standards.plugin

import java.io.File

// Вне стандарта: функция читает файлы репозитория прямо в тесте, без интерфейса —
// проверяются файлы, а не код.
// Ошибки памятки `docs/workflow.md` репозитория; скиллы — папки `skills/` со своим `SKILL.md`, по алфавиту.
fun findRepoWorkflowErrors(): List<String> {
    val root = File(System.getProperty(/* key = */ "task.repoRoot"))
    val skills = File(root, /* child = */ "skills").listFiles()
        .orEmpty()
        .filter { dir -> File(dir, /* child = */ "SKILL.md").isFile }
        .map { dir -> dir.name }
        .sorted()
    val workflow = File(root, /* child = */ "docs/workflow.md").readText()
    return findWorkflowErrors(workflow, skills)
}
