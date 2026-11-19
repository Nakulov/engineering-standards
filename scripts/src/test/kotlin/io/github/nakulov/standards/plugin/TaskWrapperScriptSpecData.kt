package io.github.nakulov.standards.plugin

import io.github.nakulov.standards.task.cli.LauncherRun
import java.io.File

/** Копия `scripts/task-wrapper` под именем `task`. */
fun makeWrapperCopy(): File {
    val wrapper = File(makeTempDir(prefix = "bin"), "task")
    File(System.getProperty("task.repoRoot"), "scripts/task-wrapper").copyTo(target = wrapper)
    wrapper.setExecutable(true)
    return wrapper
}

private fun writeScript(
    file: File,
    text: String,
) {
    file.parentFile.mkdirs()
    file.writeText(text)
    file.setExecutable(true)
}

/** Репозиторий стандарта: `plugin.json` нашего плагина и свой `scripts/task`. */
fun makeStandardRepo(): File {
    val repo = makeGitProject()
    val manifest = File(repo, ".claude-plugin/plugin.json")
    manifest.parentFile.mkdirs()
    manifest.writeText("{\n  \"name\": \"engineering-standards\"\n}\n")
    writeScript(file = File(repo, "scripts/task"), text = "#!/bin/sh\necho \"стандарт \$*\"\n")
    return repo
}

/** Корень установленного плагина: `scripts/task` печатает аргументы и выходит с кодом 3. */
fun makeStubPlugin(): File {
    val plugin = makeTempDir(prefix = "installed")
    writeScript(file = File(plugin, "scripts/task"), text = "#!/bin/sh\necho \"плагин \$*\"\nexit 3\n")
    return plugin
}

fun writeRootRecord(
    cache: File,
    project: File,
    root: File,
) {
    val record = File(cache, "engineering-standards/roots/${makeRootKey(project = project)}")
    record.parentFile.mkdirs()
    record.writeText("${root.path}\n")
}

/** Копия задачи: пустой коммит и `git worktree add` во временную папку. */
fun makeWorktree(project: File): File {
    runGit(directory = project, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "--allow-empty", "-qm", "init")
    val worktree = File(makeTempDir(prefix = "worktree"), "copy")
    runGit(directory = project, "worktree", "add", "-q", worktree.path)
    return worktree
}

fun makePluginRun(): LauncherRun {
    return LauncherRun(code = 3, out = "плагин --help\n", err = "")
}

fun makeNoRecordRun(): LauncherRun {
    return LauncherRun(
        code = 2,
        out = "",
        err = "task: плагин для этого проекта не найден — запустите claude в папке проекта\n",
    )
}

fun makeNotGitRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "task: не репозиторий git — перейдите в папку проекта\n")
}

/** `task --help` через обёртку в [directory]; git не поднимается выше кеша тестов — там репозиторий стандарта. */
fun runWrapper(
    wrapper: File,
    directory: File,
    cache: File,
): LauncherRun {
    val builder = ProcessBuilder(wrapper.path, "--help").directory(directory)
    builder.environment()["XDG_CACHE_HOME"] = cache.path
    builder.environment()["GIT_CEILING_DIRECTORIES"] = System.getProperty("task.cache")
    val process = builder.start()
    val out = process.inputStream.bufferedReader().readText()
    val err = process.errorStream.bufferedReader().readText()
    return LauncherRun(code = process.waitFor(), out = out, err = err)
}
