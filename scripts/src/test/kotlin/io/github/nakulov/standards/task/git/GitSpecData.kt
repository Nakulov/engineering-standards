package io.github.nakulov.standards.task.git

import java.io.File
import java.nio.file.Files

fun makeNoOriginMessage(): String {
    return "git fetch: fatal: 'origin' does not appear to be a git repository"
}

/** Клон с `origin` (bare) и одним коммитом в `dev`. */
fun makeClone(): File {
    val root = makeTempDirectory()
    git(directory = root, args = listOf("init", "--quiet", "--bare", "--initial-branch=dev", "origin.git"))
    git(directory = root, args = listOf("clone", "--quiet", "origin.git", "clone"))
    val clone = File(root, "clone")
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "dev"))
    git(directory = clone, args = listOf("commit", "--quiet", "--allow-empty", "--message", "начало"))
    git(directory = clone, args = listOf("push", "--quiet", "origin", "dev"))
    return clone
}

/** Worktree клона на ветке `task/1-x`. */
fun makeWorktree(): File {
    val clone = makeClone()
    val worktree = File(clone.parentFile, "worktree")
    git(directory = clone, args = listOf("worktree", "add", "--quiet", "-b", "task/1-x", worktree.path))
    return worktree
}

fun makeDirtyClone(): File {
    val clone = makeClone()
    File(clone, "new.txt").writeText("новый файл\n")
    return clone
}

/** Клон, где ветка `task/3-z` есть только в `origin`. */
fun makeCloneWithRemoteBranch(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/3-z"))
    git(directory = clone, args = listOf("push", "--quiet", "origin", "task/3-z"))
    git(directory = clone, args = listOf("switch", "--quiet", "dev"))
    git(directory = clone, args = listOf("branch", "--quiet", "-D", "task/3-z"))
    return clone
}

/** Клон на ветке задачи; в `origin/dev` есть коммит «второй коммит», которого нет в локальном `dev`. */
fun makeCloneBehindOrigin(): File {
    val clone = makeClone()
    val other = File(clone.parentFile, "other")
    git(directory = clone.parentFile, args = listOf("clone", "--quiet", "origin.git", "other"))
    git(directory = other, args = listOf("commit", "--quiet", "--allow-empty", "--message", "второй коммит"))
    git(directory = other, args = listOf("push", "--quiet", "origin", "dev"))
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/4-w"))
    return clone
}

/** Клон на ветке `task/9-f`; в `origin/task/9-f` после `fetch` — коммит «блок слит» с файлом `f.txt` (#61). */
fun makeCloneOnBranchBehindOrigin(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/9-f"))
    git(directory = clone, args = listOf("push", "--quiet", "--set-upstream", "origin", "task/9-f"))
    val other = File(clone.parentFile, "other")
    git(directory = clone.parentFile, args = listOf("clone", "--quiet", "--branch", "task/9-f", "origin.git", "other"))
    File(other, "f.txt").writeText("f\n")
    git(directory = other, args = listOf("add", "f.txt"))
    git(directory = other, args = listOf("commit", "--quiet", "--message", "блок слит"))
    git(directory = other, args = listOf("push", "--quiet", "origin", "task/9-f"))
    git(directory = clone, args = listOf("fetch", "--quiet", "origin"))
    return clone
}

/** Как [makeCloneOnBranchBehindOrigin], но в локальной `task/9-f` ещё свой коммит: ветка разошлась с `origin`. */
fun makeCloneOnDivergedBranch(): File {
    val clone = makeCloneOnBranchBehindOrigin()
    git(directory = clone, args = listOf("commit", "--quiet", "--allow-empty", "--message", "свой коммит"))
    return clone
}

fun makeNotFastForwardMessage(): String {
    return "git merge: fatal: Not possible to fast-forward, aborting."
}

/** Клон с личностью в настройке — для коммитов во временном worktree. */
fun makeIdentityClone(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("config", "user.name", "task"))
    git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
    return clone
}

/** Клон с личностью на ветке `task/7-p`: в ней добавлен `plans/p.md` («# План П»). */
fun makeCloneWithPlanBranch(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("config", "user.name", "task"))
    git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/7-p"))
    File(clone, "plans").mkdirs()
    File(clone, "plans/p.md").writeText("# План П\n")
    git(directory = clone, args = listOf("add", "plans/p.md"))
    git(directory = clone, args = listOf("commit", "--quiet", "--message", "Добавить план: П"))
    return clone
}

/**
 * Клон с неотслеживаемыми файлами: `.gitignore` игнорирует `local.properties`, `conf/` и `build/`; `notes.txt` не
 * игнорируется. Рядом с клоном — список `include` (`local.properties`, `conf/`, `notes.txt`) в формате
 * `.worktreeinclude`.
 */
fun makeCloneWithIgnoredFiles(): File {
    val clone = makeClone()
    File(clone, ".gitignore").writeText("local.properties\nconf/\nbuild/\n")
    File(clone, "local.properties").writeText("sdk.dir=/sdk\n")
    File(clone, "conf").mkdirs()
    File(clone, "conf/a.xml").writeText("<a/>\n")
    File(clone, "build").mkdirs()
    File(clone, "build/out.txt").writeText("out\n")
    File(clone, "notes.txt").writeText("заметки\n")
    File(clone.parentFile, "include").writeText("local.properties\nconf/\nnotes.txt\n")
    return clone
}

/** Клон с личностью в настройке: `a.txt` изменён, `b.txt` удалён, `c.txt` и `plans/x.md` — новые. */
fun makeCloneWithChanges(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("config", "user.name", "task"))
    git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
    File(clone, "a.txt").writeText("1\n")
    File(clone, "b.txt").writeText("1\n")
    git(directory = clone, args = listOf("add", "a.txt", "b.txt"))
    git(directory = clone, args = listOf("commit", "--quiet", "--message", "файлы"))
    File(clone, "a.txt").writeText("2\n")
    File(clone, "b.txt").delete()
    File(clone, "c.txt").writeText("новый\n")
    File(clone, "plans").mkdirs()
    File(clone, "plans/x.md").writeText("# План X\n")
    return clone
}

/** Клон с личностью: `a.txt` удалён через `git rm`, `r.txt` переименован через `git mv` в `r2.txt`. */
fun makeCloneWithIndexChanges(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("config", "user.name", "task"))
    git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
    File(clone, "a.txt").writeText("1\n")
    File(clone, "r.txt").writeText("1\n")
    git(directory = clone, args = listOf("add", "a.txt", "r.txt"))
    git(directory = clone, args = listOf("commit", "--quiet", "--message", "файлы"))
    git(directory = clone, args = listOf("rm", "--quiet", "a.txt"))
    git(directory = clone, args = listOf("mv", "r.txt", "r2.txt"))
    return clone
}

/** Файл сообщения коммита рядом с клоном. */
fun makeMessageFile(directory: File): String {
    val file = File(directory.parentFile, "message.txt")
    file.writeText("Добавить c\n\nЗадача плана: Задача 1\nФайлы:\n- `a.txt` — изменён\n- `c.txt` — создан\n")
    return file.path
}

fun readHeadSubject(directory: File): String {
    val process = ProcessBuilder(listOf("git", "log", "-1", "--format=%s")).directory(directory).start()
    return process.inputStream.bufferedReader().readText().trim()
}

fun makeRepoWithoutOrigin(): File {
    val root = makeTempDirectory()
    git(directory = root, args = listOf("init", "--quiet"))
    return root
}

/** Пустой репозиторий (ветка по умолчанию `main`) с файлами стандарта и файлом проекта, без коммитов. */
fun makeEmptyRepoWithFiles(): File {
    val root = makeRepoWithoutOrigin()
    git(directory = root, args = listOf("symbolic-ref", "HEAD", "refs/heads/main"))
    git(directory = root, args = listOf("config", "user.name", "task"))
    git(directory = root, args = listOf("config", "user.email", "task@example.invalid"))
    File(root, ".gitignore").writeText("build/\n")
    File(root, ".github").mkdirs()
    File(root, ".github/pull_request_template.md").writeText("Closes #\n")
    File(root, "notes.txt").writeText("файл проекта\n")
    return root
}

fun makeFirstCommitResult(): List<String> {
    return listOf("dev", "Начальная настройка", ".github/pull_request_template.md", ".gitignore")
}

fun readHeadFiles(directory: File): List<String> {
    val process = ProcessBuilder(listOf("git", "ls-tree", "-r", "--name-only", "HEAD")).directory(directory).start()
    return process.inputStream.bufferedReader().readText().lines().filter { line -> line.isNotBlank() }
}

/** Клон с long-веткой `long/7-big` — локальной и в `origin`; рядом `long/70-other`: номер с тем же началом. */
fun makeCloneWithLongBranch(): File {
    val clone = makeClone()
    git(directory = clone, args = listOf("branch", "--quiet", "long/70-other"))
    git(directory = clone, args = listOf("branch", "--quiet", "long/7-big"))
    git(directory = clone, args = listOf("push", "--quiet", "origin", "long/70-other", "long/7-big"))
    return clone
}

/** Как [makeCloneWithLongBranch], но локальной `long/7-big` нет — только в `origin`. */
fun makeCloneWithRemoteLongBranch(): File {
    val clone = makeCloneWithLongBranch()
    git(directory = clone, args = listOf("branch", "--quiet", "-D", "long/7-big"))
    return clone
}

/**
 * Клон с личностью на ветке `task/7-final` от `dev`; в `origin/long/7-big` — коммит «блок слит» с `b.txt`.
 * [conflict] — на final свой `b.txt`: слияние long с конфликтом.
 */
fun makeFinalBranchBehindLong(conflict: Boolean): File {
    val clone = makeIdentityClone()
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "long/7-big"))
    File(clone, "b.txt").writeText("long\n")
    git(directory = clone, args = listOf("add", "b.txt"))
    git(directory = clone, args = listOf("commit", "--quiet", "--message", "блок слит"))
    git(directory = clone, args = listOf("push", "--quiet", "origin", "long/7-big"))
    git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/7-final", "dev"))
    if (conflict) {
        File(clone, "b.txt").writeText("final\n")
        git(directory = clone, args = listOf("add", "b.txt"))
        git(directory = clone, args = listOf("commit", "--quiet", "--message", "правка по ревью"))
    }
    return clone
}

/** Временная папка в `build/` модуля: её убирает `./gradlew clean`. */
private fun makeTempDirectory(): File {
    val base = File(System.getProperty("task.cache"), "git")
    base.mkdirs()
    return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "repo").toFile()
}

private fun git(
    directory: File,
    args: List<String>,
) {
    val identity = listOf("-c", "user.name=task", "-c", "user.email=task@example.invalid")
    val process = ProcessBuilder(listOf("git") + identity + args).directory(directory).redirectErrorStream(true).start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "git ${args.joinToString(separator = " ")}: $output" }
}
