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
