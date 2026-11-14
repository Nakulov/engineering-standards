package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.SystemConsole
import io.github.nakulov.standards.task.github.GitHub
import io.github.nakulov.standards.task.shell.ProcessShell
import java.io.File
import kotlin.system.exitProcess

/**
 * Точка входа `setup`: её запускает `scripts/setup` через запускатель `task` и передаёт корень плагина в
 * `TASK_PLUGIN_ROOT`. Репозиторий — текущая папка.
 */
fun main(args: Array<String>) {
    val shell = ProcessShell(directory = File(System.getProperty("user.dir")))
    val gitHub = GitHub(shell = shell, pause = { millis: Long -> Thread.sleep(millis) })
    val pluginRoot = File(System.getenv("TASK_PLUGIN_ROOT") ?: ".")
    val labelsFile = File(pluginRoot, LABELS)
    val setup = Setup(
        environment = Environment(gitHub = gitHub),
        steps = listOf(
            RepositorySettings(gitHub = gitHub),
            Rulesets(gitHub = gitHub, scratch = File(System.getProperty("java.io.tmpdir"), "engineering-standards")),
            Labels(gitHub = gitHub, standard = Labels.parse(text = labelsFile.readText(), file = LABELS)),
        ),
        console = SystemConsole,
        debug = System.getenv("TASK_DEBUG") == "1",
    )
    exitProcess(setup.run(args = args.toList()))
}

private const val LABELS = "templates/github/labels.yml"
