package io.github.nakulov.standards.plugin

import io.github.nakulov.standards.task.cli.LauncherRun
import java.io.File
import java.nio.file.Files

/** Пустая папка в кеше тестов модуля. */
fun makeTempDir(prefix: String): File {
    val base = File(System.getProperty(/* key = */ "task.cache"), /* child = */ "plugin")
    base.mkdirs()
    return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ prefix).toFile()
}

/** Вывод `git` в папке [directory]; ненулевой код — ошибка теста. */
fun runGit(
    directory: File,
    vararg args: String,
): String {
    val process = ProcessBuilder(listOf("git") + args).directory(directory).redirectErrorStream(true).start()
    val out = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0) { "git ${args.joinToString(separator = " ")}: $out" }
    return out
}

fun makeGitProject(): File {
    val project = makeTempDir(prefix = "project")
    runGit(directory = project, "init", "-q")
    return project
}

/** Ключ записи корня: основная папка репозитория, `/` → `-` — как в хуке и обёртке. */
fun makeRootKey(project: File): String {
    val common = runGit(directory = project, "rev-parse", "--path-format=absolute", "--git-common-dir").trim()
    return File(/* pathname = */ common).parent.replace(oldChar = '/', newChar = '-')
}

/** Копия `hooks/session-start` в новой папке плагина и [rules] — имя файла → текст; пусто — без папки `rules/`. */
fun makeHookPlugin(rules: Map<String, String>): File {
    val plugin = makeTempDir(prefix = "plugin")
    val hook = File(plugin, /* child = */ "hooks/session-start")
    File(System.getProperty(/* key = */ "task.repoRoot"), /* child = */ "hooks/session-start").copyTo(target = hook)
    hook.setExecutable(true)
    rules.forEach { (name, text) ->
        val rule = File(plugin, "rules/$name")
        rule.parentFile.mkdirs()
        rule.writeText(text)
    }
    return plugin
}

fun makeEnvFile(): File {
    val file = File(makeTempDir(prefix = "env"), /* child = */ "env")
    file.writeText(text = "")
    return file
}

/** Кеш — обычный файл: папку записей в нём не создать. */
fun makeBlockedCache(): File {
    val file = File(makeTempDir(prefix = "blocked"), /* child = */ "cache")
    file.writeText(text = "")
    return file
}

fun makePathLine(plugin: File): String {
    return "export PATH=\"${plugin.path}/scripts:\$PATH\"\n"
}

fun makeRootLine(plugin: File): String {
    return "Корень плагина: `${plugin.path}`\n"
}

fun makeRootRecord(plugin: File): String {
    return plugin.path + "\n"
}

fun makeRootOnlyRun(plugin: File): LauncherRun {
    return LauncherRun(code = 0, out = makeRootLine(plugin), err = "")
}

fun makeRulesRun(plugin: File): LauncherRun {
    return LauncherRun(code = 0, out = makeRootLine(plugin) + "Правило А\n\nПравило Б\n\n", err = "")
}

fun makeNoEnvFileRun(plugin: File): LauncherRun {
    val out = "`task` не в PATH — вызывай ${plugin.path}/scripts/task\n" + makeRootLine(plugin)
    return LauncherRun(code = 0, out = out, err = "")
}

fun makeNoCacheRun(
    cache: File,
    plugin: File,
): LauncherRun {
    val err = "engineering-standards: корень плагина не записан в ${cache.path}/engineering-standards/roots\n"
    return LauncherRun(code = 0, out = makeRulesRun(plugin).out, err = err)
}

fun readRootRecord(
    cache: File,
    project: File,
): String {
    return File(cache, "engineering-standards/roots/${makeRootKey(project)}").readText()
}

/** Запуск хука: [envFile] `null` — без `CLAUDE_ENV_FILE`. */
fun runHook(
    plugin: File,
    project: File,
    envFile: File?,
    cache: File,
): LauncherRun {
    val builder = ProcessBuilder(File(plugin, /* child = */ "hooks/session-start").path).directory(project)
    val environment = builder.environment()
    environment.remove(key = "CLAUDE_ENV_FILE")
    environment["CLAUDE_PROJECT_DIR"] = project.path
    environment["XDG_CACHE_HOME"] = cache.path
    if (envFile != null) {
        environment["CLAUDE_ENV_FILE"] = envFile.path
    }
    val process = builder.start()
    process.outputStream.close()
    val out = process.inputStream.bufferedReader().readText()
    val err = process.errorStream.bufferedReader().readText()
    return LauncherRun(code = process.waitFor(), out = out, err = err)
}
