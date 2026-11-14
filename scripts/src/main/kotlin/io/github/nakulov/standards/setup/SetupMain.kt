package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.SystemConsole
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.GitHub
import io.github.nakulov.standards.task.shell.ProcessShell
import java.io.File
import kotlin.system.exitProcess

/**
 * Точка входа `setup`: её запускает `scripts/setup` через запускатель `task` и передаёт корень плагина в
 * `TASK_PLUGIN_ROOT`. Репозиторий — текущая папка. Порядок шагов: сначала файлы — первый коммит пустого репозитория
 * берёт их, затем ветка — защита и доска ставятся на существующую `dev`.
 */
fun main(args: Array<String>) {
    val shell = ProcessShell(directory = File(System.getProperty("user.dir")))
    val gitHub = GitHub(shell = shell, pause = { millis: Long -> Thread.sleep(millis) })
    val pluginRoot = File(System.getenv("TASK_PLUGIN_ROOT") ?: ".")
    val git = Git(shell = shell)
    val scratch = File(System.getProperty("java.io.tmpdir"), "engineering-standards")
    val labels = Labels.parse(text = File(pluginRoot, LABELS).readText(), file = LABELS)
    val board = BoardSetup.parse(text = File(pluginRoot, BOARD).readText(), file = BOARD)
    val setup = Setup(
        environment = Environment(gitHub = gitHub),
        steps = listOf(
            StandardFiles(git = git, pluginRoot = pluginRoot),
            GitignoreBlock(git = git, block = File(pluginRoot, GITIGNORE).readText()),
            InitialBranch(gitHub = gitHub, git = git, files = listOf(".github", ".gitignore", ".editorconfig")),
            RepositorySettings(gitHub = gitHub),
            Rulesets(gitHub = gitHub, scratch = scratch),
            Labels(gitHub = gitHub, standard = labels),
            BoardSetup(gitHub = gitHub, template = board, scratch = scratch),
        ),
        console = SystemConsole,
        debug = System.getenv("TASK_DEBUG") == "1",
    )
    exitProcess(setup.run(args = args.toList()))
}

private const val LABELS = "templates/github/labels.yml"
private const val BOARD = "templates/github/board.yml"
private const val GITIGNORE = "templates/project/gitignore"
