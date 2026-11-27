package io.github.nakulov.standards.task.cli

import io.github.nakulov.standards.plugin.makeTempDir
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

const val UNKNOWN_COMMAND = "nope"

/** Результат запуска `scripts/task`: код выхода, stdout, stderr. */
data class LauncherRun(
    val code: Int,
    val out: String,
    val err: String,
)

/** Ответ `Task` на неизвестную команду: не зависит от списка команд и их опций. */
fun makeUnknownCommandRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "неизвестная команда nope — см. task --help\n")
}

fun makeSetupHelpRun(): LauncherRun {
    return LauncherRun(
        code = 0,
        out = "setup — настройка репозитория текущей папки по стандарту. Запуск:\n\n" +
            "  setup — применить недостающее; удаляющие шаги только печатаются\n" +
            "  setup --confirm — применить, включая удаляющие шаги\n" +
            "  setup --check — только сверить: 0 — соответствует, 1 — отклонения\n",
        err = "",
    )
}

fun makeNoKotlinRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "Нет Kotlin — установите: brew install kotlin\n")
}

fun makeOldKotlinRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "Kotlin 2.1.20, нужен ≥ 2.2 — обновите: brew upgrade kotlin\n")
}

fun makeSetupLauncher(): String {
    return System.getProperty("task.setupLauncher")
}

fun makeSystemPath(): String {
    return System.getenv("PATH")
}

/** PATH без Kotlin: только системные утилиты. */
fun makeBarePath(): String {
    return "/usr/bin:/bin"
}

/** PATH, где `kotlinc` — заглушка, которая сообщает версию 2.1.20. */
fun makeOldKotlinPath(): String {
    val bin = File(System.getProperty("task.cache"), "old-kotlin")
    bin.mkdirs()
    val kotlinc = File(bin, "kotlinc")
    kotlinc.writeText("#!/bin/sh\necho 'info: kotlinc-jvm 2.1.20 (JRE 17.0.21)' >&2\n")
    kotlinc.setExecutable(true)
    return "${bin.absolutePath}:${makeBarePath()}"
}

/** Корень репозитория, из которого запущены тесты: `-C` в него — свой корень, без перенаправления. */
fun makeRepoRoot(): String {
    return System.getProperty(/* key = */ "task.repoRoot")
}

/**
 * Запуск `scripts/task` с таймаутом: вывод — в файлы, иначе сломанное перенаправление `-C` (копия запускает себя
 * бесконечно) повесит чтение вывода, и тест зависнет, а не упадёт.
 */
fun runLauncher(
    path: String,
    args: List<String>,
    launcher: String = System.getProperty("task.launcher"),
    directory: File? = null,
    timeout: Duration = 3.minutes,
): LauncherRun {
    val logs = makeTempDir(prefix = "launcher")
    val out = File(/* parent = */ logs, /* child = */ "out")
    val err = File(/* parent = */ logs, /* child = */ "err")
    val builder = ProcessBuilder(listOf(launcher) + args).directory(directory)
        .redirectOutput(out)
        .redirectError(err)
    builder.environment()["PATH"] = path
    builder.environment()["XDG_CACHE_HOME"] = System.getProperty("task.cache")
    val process = builder.start()
    val isFinished = process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)
    if (!isFinished) {
        process.destroyForcibly()
    }
    check(isFinished) { "task ${args.joinToString(separator = " ")} не завершился за $timeout" }
    return LauncherRun(code = process.exitValue(), out = out.readText(), err = err.readText())
}
