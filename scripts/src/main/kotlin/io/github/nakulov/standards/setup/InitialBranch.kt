package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.GitHub

/**
 * Основная ветка `dev` (#8, решение 10). Пустой репозиторий — первый коммит в `dev` с файлами стандарта делает сам
 * `setup`: PR делать не во что, а защиту и основную ветку на несуществующую ветку GitHub не ставит. Основная ветка с
 * одним начальным коммитом (README при создании) заменяется на `dev` — с `--confirm`, это удаление. Ветку с историей
 * `setup` не трогает: переход — решение человека.
 */
class InitialBranch(
    private val gitHub: GitHub,
    private val git: Git,
    private val files: List<String>,
) : Setup.Step {
    override fun run(mode: Setup.Mode): List<Finding> {
        val branches = gitHub.read(args = listOf("api", "$REPOSITORY/branches?per_page=1", "--jq", "length"))
        if (branches == "0") {
            return listOf(firstCommit(mode = mode))
        }
        val current = gitHub.read(args = listOf("api", REPOSITORY, "--jq", ".default_branch"))
        if (current == DEV) {
            return listOf(Finding(status = Finding.Status.OK, area = AREA, text = "основная ветка $DEV"))
        }
        val commits = gitHub.read(args = listOf("api", "$REPOSITORY/commits?sha=$current&per_page=2", "--jq", "length"))
        if (commits != "1" || hasDev()) {
            val text = "основная ветка $current, по стандарту $DEV — в ней история, переход делает человек"
            return listOf(Finding(status = Finding.Status.DEVIATION, area = AREA, text = text))
        }
        val finding = when {
            !mode.apply -> Finding(
                status = Finding.Status.DEVIATION,
                area = AREA,
                text = "основная ветка $current с одним начальным коммитом — заменяется на $DEV",
            )
            !mode.confirmed -> Finding(
                status = Finding.Status.NEEDS_CONFIRM,
                area = AREA,
                text = "заменит основную ветку $current (один начальный коммит) на $DEV и удалит $current",
            )
            else -> {
                replace(current = current)
                val text = "$DEV создана от $current и стала основной, $current удалена — " +
                    "локально: git fetch --prune && git switch $DEV"
                Finding(status = Finding.Status.FIXED, area = AREA, text = text)
            }
        }
        return listOf(finding)
    }

    private fun firstCommit(mode: Setup.Mode): Finding {
        if (!mode.apply) {
            val text = "репозиторий пустой — setup сделает первый коммит в $DEV с файлами стандарта"
            return Finding(status = Finding.Status.DEVIATION, area = AREA, text = text)
        }
        if (git.hasCommits()) {
            val text = "на сервере коммитов нет, а локально есть — " +
                "отправьте их сами (ветка $DEV), затем запустите setup"
            return Finding(status = Finding.Status.DEVIATION, area = AREA, text = text)
        }
        git.firstCommit(branch = DEV, message = FIRST_COMMIT, paths = files)
        git.push(name = DEV)
        setDefault()
        val text = "первый коммит в $DEV — файлы стандарта; $DEV — основная ветка"
        return Finding(status = Finding.Status.FIXED, area = AREA, text = text)
    }

    private fun hasDev(): Boolean {
        return try {
            gitHub.read(args = listOf("api", "$REPOSITORY/branches/$DEV", "--jq", ".name"))
            true
        } catch (error: TaskError.Failure) {
            if ("HTTP 404" !in error.message.orEmpty()) {
                throw error
            }
            false
        }
    }

    private fun replace(current: String) {
        val sha = gitHub.read(args = listOf("api", "$REPOSITORY/git/ref/heads/$current", "--jq", ".object.sha"))
        val fields = listOf("-f", "ref=refs/heads/$DEV", "-f", "sha=$sha")
        gitHub.write(args = listOf("api", "--method", "POST", "$REPOSITORY/git/refs") + fields)
        setDefault()
        gitHub.write(args = listOf("api", "--method", "DELETE", "$REPOSITORY/git/refs/heads/$current"))
    }

    private fun setDefault() {
        gitHub.write(args = listOf("api", "--method", "PATCH", REPOSITORY, "-f", "default_branch=$DEV"))
    }

    private companion object {
        const val AREA = "ветки"
        const val REPOSITORY = "repos/{owner}/{repo}"
        const val DEV = "dev"
        const val FIRST_COMMIT = "Начальная настройка по стандарту"
    }
}
