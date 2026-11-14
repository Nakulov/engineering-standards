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

fun makeRepoWithoutOrigin(): File {
    val root = makeTempDirectory()
    git(directory = root, args = listOf("init", "--quiet"))
    return root
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
