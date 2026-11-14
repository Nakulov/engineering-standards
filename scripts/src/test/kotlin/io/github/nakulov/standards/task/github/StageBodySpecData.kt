package io.github.nakulov.standards.task.github

import java.io.File

/** Настоящий шаблон этапа: спека ловит расхождение шаблона и `task stage create`. */
fun makeStageTemplate(): String {
    return File(System.getProperty("task.repoRoot"), "templates/github/stage.md").readText()
}
