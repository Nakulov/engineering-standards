package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.git.Git
import java.io.File

/**
 * «Убрать план» перед PR в `dev` (#53): план ветки копируется в `plans/done/` основной папки (в `.gitignore` —
 * остаётся у пользователя) и удаляется из ветки коммитом «Убрать план: <название>». Иначе план попадёт в `dev`.
 */
class PlanArchive(
    private val git: Git,
) {
    fun run(console: Console) {
        val plans = git.addedPaths(base = BASE).filter { path -> isPlan(path = path) }
        if (plans.isEmpty()) {
            return
        }
        val root = File(git.root())
        val done = File(git.mainRoot(), DONE)
        done.mkdirs()
        val titles = plans.map { path ->
            val source = File(root, path)
            source.copyTo(target = File(done, source.name), overwrite = true)
            title(source = source)
        }
        git.removeAndCommit(paths = plans, message = "Убрать план: ${titles.joinToString(separator = "; ")}")
        console.line(text = "план убран из ветки, копия — ${done.path}")
    }

    private fun isPlan(path: String): Boolean {
        return path.startsWith(prefix = "plans/") &&
            path.endsWith(suffix = ".md") &&
            path.count { char -> char == '/' } == 1
    }

    private fun title(source: File): String {
        return source.readLines().firstOrNull { line -> line.startsWith(prefix = "# ") }
            ?.removePrefix(prefix = "# ")
            ?.trim()
            ?: source.nameWithoutExtension
    }

    private companion object {
        const val BASE = "origin/dev"
        const val DONE = "plans/done"
    }
}
