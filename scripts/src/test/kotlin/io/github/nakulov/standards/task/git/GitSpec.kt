package io.github.nakulov.standards.task.git

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.ProcessShell
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File

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

        shouldReturnTest("task/2-y", options = TestOptions(name = "новая ветка от origin/dev")) {
            val git = Git(shell = ProcessShell(directory = makeClone()))
            git.createBranch(name = "task/2-y", base = "origin/dev")
            git.currentBranch()
        }

        shouldReturnTest(listOf(false, true), options = TestOptions(name = "ветка только в origin")) {
            val git = Git(shell = ProcessShell(directory = makeCloneWithRemoteBranch()))
            listOf(git.hasLocalBranch(name = "task/3-z"), git.hasRemoteBranch(name = "task/3-z"))
        }

        shouldReturnTest("task/3-z", options = TestOptions(name = "переключение на ветку из origin")) {
            val git = Git(shell = ProcessShell(directory = makeCloneWithRemoteBranch()))
            git.switchTo(name = "task/3-z")
            git.currentBranch()
        }

        shouldReturnTest(listOf("", "dev"), options = TestOptions(name = "worktree уходит с ветки, dev не тронут")) {
            val worktree = makeWorktree()
            Git(shell = ProcessShell(directory = worktree)).detach(ref = "origin/dev")
            listOf(
                Git(shell = ProcessShell(directory = worktree)).currentBranch(),
                Git(shell = ProcessShell(directory = File(worktree.parentFile, "clone"))).currentBranch(),
            )
        }

        shouldReturnTest(false, options = TestOptions(name = "ветка удалена")) {
            val worktree = makeWorktree()
            val git = Git(shell = ProcessShell(directory = worktree))
            git.detach(ref = "origin/dev")
            git.deleteBranch(name = "task/1-x")
            git.hasLocalBranch(name = "task/1-x")
        }

        shouldReturnTest("второй коммит", options = TestOptions(name = "dev перематывается до origin/dev")) {
            val clone = makeCloneBehindOrigin()
            val git = Git(shell = ProcessShell(directory = clone))
            git.fetch()
            git.updateDev()
            readHeadSubject(directory = clone)
        }
    })
