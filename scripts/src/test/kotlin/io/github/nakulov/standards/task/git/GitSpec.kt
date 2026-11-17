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

        shouldReturnTest(true, options = TestOptions(name = "корень репозитория из подпапки")) {
            val clone = makeClone()
            File(clone, "sub").mkdirs()
            Git(shell = ProcessShell(directory = File(clone, "sub"))).root() == clone.canonicalPath
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

        shouldReturnTest(true, options = TestOptions(name = "push отправляет ветку в origin")) {
            val git = Git(shell = ProcessShell(directory = makeClone()))
            git.createBranch(name = "long/5-v", base = "origin/dev")
            git.push(name = "long/5-v")
            git.hasRemoteBranch(name = "long/5-v")
        }

        shouldReturnTest(null, options = TestOptions(name = "базы ветки нет — PR в dev")) {
            Git(shell = ProcessShell(directory = makeClone())).base(branch = "task/6-u")
        }

        shouldReturnTest("long/5-v", options = TestOptions(name = "база ветки в настройке git")) {
            val git = Git(shell = ProcessShell(directory = makeClone()))
            git.setBase(branch = "task/6-u", base = "long/5-v")
            git.base(branch = "task/6-u")
        }

        shouldReturnTest("второй коммит", options = TestOptions(name = "dev перематывается до origin/dev")) {
            val clone = makeCloneBehindOrigin()
            val git = Git(shell = ProcessShell(directory = clone))
            git.fetch()
            git.updateDev()
            readHeadSubject(directory = clone)
        }

        shouldReturnTest(listOf(false, true), options = TestOptions(name = "коммиты: пустой репозиторий и клон")) {
            val empty = Git(shell = ProcessShell(directory = makeRepoWithoutOrigin())).hasCommits()
            val clone = Git(shell = ProcessShell(directory = makeClone())).hasCommits()
            listOf(empty, clone)
        }

        shouldReturnTest(makeFirstCommitResult(), options = TestOptions(name = "первый коммит — файлы стандарта")) {
            val repository = makeEmptyRepoWithFiles()
            val git = Git(shell = ProcessShell(directory = repository))
            git.firstCommit(branch = "dev", message = "Начальная настройка", paths = listOf(".gitignore", ".github"))
            listOf(git.currentBranch(), readHeadSubject(directory = repository)) + readHeadFiles(directory = repository)
        }

        shouldReturnTest(
            listOf("a.txt", "b.txt", "c.txt", "plans/x.md"),
            options = TestOptions(name = "изменённые пути"),
        ) {
            Git(shell = ProcessShell(directory = makeCloneWithChanges())).changedPaths().sorted()
        }

        shouldReturnTest(
            listOf("Добавить c", "b.txt", "plans/x.md"),
            options = TestOptions(name = "коммит только путей"),
        ) {
            val clone = makeCloneWithChanges()
            val git = Git(shell = ProcessShell(directory = clone))
            git.commitPaths(paths = listOf("a.txt", "c.txt"), messageFile = makeMessageFile(directory = clone))
            listOf(readHeadSubject(directory = clone)) + git.changedPaths().sorted()
        }

        shouldReturnTest("Добавить c", options = TestOptions(name = "коммит путей из подпапки")) {
            val clone = makeCloneWithChanges()
            Git(shell = ProcessShell(directory = File(clone, "plans")))
                .commitPaths(paths = listOf("a.txt", "c.txt"), messageFile = makeMessageFile(directory = clone))
            readHeadSubject(directory = clone)
        }

        shouldReturnTest(
            listOf("a.txt", "r.txt", "r2.txt"),
            options = TestOptions(name = "изменённые пути: git rm и git mv — каждый путь"),
        ) {
            Git(shell = ProcessShell(directory = makeCloneWithIndexChanges())).changedPaths().sorted()
        }

        shouldReturnTest(
            emptyList<String>(),
            options = TestOptions(name = "коммит удаления и переименования из индекса"),
        ) {
            val clone = makeCloneWithIndexChanges()
            val git = Git(shell = ProcessShell(directory = clone))
            git.commitPaths(
                paths = listOf("a.txt", "r.txt", "r2.txt"),
                messageFile = makeMessageFile(directory = clone),
            )
            git.changedPaths()
        }

        shouldReturnTest(listOf("c.txt", "plans/x.md"), options = TestOptions(name = "коммит удаления без git rm")) {
            val clone = makeCloneWithChanges()
            val git = Git(shell = ProcessShell(directory = clone))
            git.commitPaths(paths = listOf("a.txt", "b.txt"), messageFile = makeMessageFile(directory = clone))
            git.changedPaths().sorted()
        }

        shouldReturnTest(true, options = TestOptions(name = "основная папка из worktree")) {
            val worktree = makeWorktree()
            val main = Git(shell = ProcessShell(directory = worktree)).mainRoot()
            File(main).canonicalPath == File(worktree.parentFile, "clone").canonicalPath
        }

        shouldReturnTest(listOf("plans/p.md"), options = TestOptions(name = "добавленные в ветке пути")) {
            Git(shell = ProcessShell(directory = makeCloneWithPlanBranch())).addedPaths(base = "origin/dev")
        }

        shouldReturnTest(listOf("Убрать план: П"), options = TestOptions(name = "удаление и коммит")) {
            val clone = makeCloneWithPlanBranch()
            Git(shell = ProcessShell(directory = clone))
                .removeAndCommit(paths = listOf("plans/p.md"), message = "Убрать план: П")
            listOf(readHeadSubject(directory = clone)) +
                readHeadFiles(directory = clone).filter { path -> path.startsWith(prefix = "plans/") }
        }

        shouldReturnTest(true, options = TestOptions(name = "где открыта ветка")) {
            val worktree = makeWorktree()
            val clone = File(worktree.parentFile, "clone")
            val opened = Git(shell = ProcessShell(directory = clone)).openedAt(branch = "task/1-x")
            File(checkNotNull(opened)).canonicalPath == worktree.canonicalPath
        }

        shouldReturnTest(listOf(true, false), options = TestOptions(name = "предок ветки")) {
            val git = Git(shell = ProcessShell(directory = makeCloneWithPlanBranch()))
            listOf(
                git.isAncestor(ancestor = "origin/dev", descendant = "task/7-p"),
                git.isAncestor(ancestor = "task/7-p", descendant = "origin/dev"),
            )
        }

        shouldReturnTest(
            listOf(true, true, false),
            options = TestOptions(name = "коммит во временном worktree и push"),
        ) {
            val clone = makeIdentityClone()
            val git = Git(shell = ProcessShell(directory = clone))
            val temp = File(git.commonDir(), "task-plan/q").path
            git.addWorktree(path = temp, ref = "origin/dev")
            File(temp, "plans").mkdirs()
            File(temp, "plans/q.md").writeText("# План Q\n")
            val changed = git.hasChanges(path = temp)
            git.commitAll(path = temp, message = "Добавить план: Q")
            git.pushHead(path = temp, branch = "task/8-q")
            git.removeWorktree(path = temp)
            git.fetch()
            listOf(changed, git.hasRemoteBranch(name = "task/8-q"), File(temp).exists())
        }

        shouldReturnTest("Добавить план: Q", options = TestOptions(name = "локальная ветка перематывается на коммит")) {
            val clone = makeIdentityClone()
            val git = Git(shell = ProcessShell(directory = clone))
            val temp = File(git.commonDir(), "task-plan/q").path
            git.addWorktree(path = temp, ref = "origin/dev")
            File(temp, "q.md").writeText("q\n")
            git.commitAll(path = temp, message = "Добавить план: Q")
            val commit = git.head(path = temp)
            git.removeWorktree(path = temp)
            git.moveBranch(branch = "task/8-q", commit = commit)
            git.switchTo(name = "task/8-q")
            readHeadSubject(directory = clone)
        }
    })
