package io.github.nakulov.standards.task.cli

import java.io.File

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

fun runLauncher(
    path: String,
    args: List<String>,
    launcher: String = System.getProperty("task.launcher"),
): LauncherRun {
    val builder = ProcessBuilder(listOf(launcher) + args)
    builder.environment()["PATH"] = path
    builder.environment()["XDG_CACHE_HOME"] = System.getProperty("task.cache")
    val process = builder.start()
    val out = process.inputStream.bufferedReader().readText()
    val err = process.errorStream.bufferedReader().readText()
    return LauncherRun(code = process.waitFor(), out = out, err = err)
}
