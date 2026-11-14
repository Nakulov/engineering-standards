package io.github.nakulov.standards.task.git

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.ProcessShell
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека работает с настоящим `git` во временных репозиториях — worktree на подмене честно не проверить
// (#13, решение 7). Подготовка репозиториев — в GitSpecData.
class GitSpec :
    BehaviorSpec({
        shouldReturnTest(false, options = TestOptions(name = "основной клон — не worktree")) {
            Git(shell = ProcessShell(directory = makeClone())).isWorktree()
        }

        shouldReturnTest(true, options = TestOptions(name = "worktree")) {
            Git(shell = ProcessShell(directory = makeWorktree())).isWorktree()
        }

        shouldReturnTest(true, options = TestOptions(name = "чистое дерево")) {
            Git(shell = ProcessShell(directory = makeClone())).isClean()
        }

        shouldReturnTest(false, options = TestOptions(name = "новый файл — дерево грязное")) {
            Git(shell = ProcessShell(directory = makeDirtyClone())).isClean()
        }

        shouldReturnTest("dev", options = TestOptions(name = "ветка основного клона")) {
            Git(shell = ProcessShell(directory = makeClone())).currentBranch()
        }

        shouldReturnTest("task/1-x", options = TestOptions(name = "ветка worktree")) {
            Git(shell = ProcessShell(directory = makeWorktree())).currentBranch()
        }

        shouldReturnTest(Unit, options = TestOptions(name = "fetch из origin")) {
            Git(shell = ProcessShell(directory = makeClone())).fetch()
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeNoOriginMessage()) {
            Git(shell = ProcessShell(directory = makeRepoWithoutOrigin())).fetch()
        }
    })
