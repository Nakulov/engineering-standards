package io.github.nakulov.standards.plugin

import io.github.nakulov.standards.task.cli.LauncherRun
import java.io.File

const val STANDARD_COPY = ".worktrees/copy"
const val NESTED_PROJECT = "app"
const val MISSING_DIRECTORY = "missing"

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
    makeStandardManifest(repo = repo)
    writeScript(file = File(repo, "scripts/task"), text = "#!/bin/sh\necho \"стандарт \$*\"\n")
    return repo
}

/**
 * Копия задачи в [STANDARD_COPY] репозитория стандарта [repo]: свой `plugin.json` и `scripts/task`, печатает «копия».
 * Файлы пишутся в папку копии: в тестовом репозитории они не закоммичены и в ворктри не попадут.
 */
fun makeStandardCopy(repo: File): File {
    makeInitialCommit(project = repo)
    runGit(directory = repo, "worktree", "add", "-q", STANDARD_COPY)
    val copy = File(repo, STANDARD_COPY)
    makeStandardManifest(repo = copy)
    writeScript(file = File(copy, "scripts/task"), text = "#!/bin/sh\necho \"копия \$*\"\n")
    return copy
}

/** `plugin.json` нашего плагина в [repo] — по нему обёртка узнаёт репозиторий стандарта. */
private fun makeStandardManifest(repo: File): File {
    val manifest = File(repo, ".claude-plugin/plugin.json")
    manifest.parentFile.mkdirs()
    manifest.writeText("{\n  \"name\": \"engineering-standards\"\n}\n")
    return manifest
}

/** Корень установленного плагина: `scripts/task` печатает аргументы и выходит с кодом 3. */
fun makeStubPlugin(): File {
    val plugin = makeTempDir(prefix = "installed")
    writeScript(file = File(plugin, "scripts/task"), text = "#!/bin/sh\necho \"плагин \$*\"\nexit 3\n")
    return plugin
}

/** Проект [NESTED_PROJECT] внутри папки [parent] — для запуска `-C` из папки вне git. */
fun makeNestedProject(parent: File): File {
    val project = File(parent, NESTED_PROJECT)
    project.mkdirs()
    runGit(directory = project, "init", "-q")
    return project
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
    makeInitialCommit(project = project)
    val worktree = File(makeTempDir(prefix = "worktree"), "copy")
    runGit(directory = project, "worktree", "add", "-q", worktree.path)
    return worktree
}

/** Пустой коммит в [project] — без него `git worktree add` не создаст копию; результат — вывод `git`. */
private fun makeInitialCommit(project: File): String {
    return runGit(
        directory = project,
        "-c",
        "user.name=t",
        "-c",
        "user.email=t@t",
        "commit",
        "--allow-empty",
        "-qm",
        "init",
    )
}

fun makePluginRun(): LauncherRun {
    return LauncherRun(code = 3, out = "плагин --help\n", err = "")
}

fun makeCopyRun(): LauncherRun {
    return LauncherRun(code = 0, out = "копия -C $STANDARD_COPY --help\n", err = "")
}

fun makeNestedProjectRun(): LauncherRun {
    return LauncherRun(code = 3, out = "плагин -C $NESTED_PROJECT --help\n", err = "")
}

fun makeMissingDirectoryRun(): LauncherRun {
    return LauncherRun(code = 3, out = "плагин -C $MISSING_DIRECTORY --help\n", err = "")
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

/**
 * `task <options> --help` через обёртку в [directory]; git не поднимается выше кеша тестов — там репозиторий
 * стандарта.
 */
fun runWrapper(
    wrapper: File,
    directory: File,
    cache: File,
    vararg options: String,
): LauncherRun {
    val builder = ProcessBuilder(listOf(wrapper.path) + options + "--help").directory(directory)
    builder.environment()["XDG_CACHE_HOME"] = cache.path
    builder.environment()["GIT_CEILING_DIRECTORIES"] = System.getProperty("task.cache")
    val process = builder.start()
    val out = process.inputStream.bufferedReader().readText()
    val err = process.errorStream.bufferedReader().readText()
    return LauncherRun(code = process.waitFor(), out = out, err = err)
}
