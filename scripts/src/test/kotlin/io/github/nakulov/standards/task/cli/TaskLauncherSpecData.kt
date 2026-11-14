package io.github.nakulov.standards.task.cli

import java.io.File

/** Результат запуска `scripts/task`: код выхода, stdout, stderr. */
data class LauncherRun(
    val code: Int,
    val out: String,
    val err: String,
)

fun makeHelpRun(): LauncherRun {
    return LauncherRun(
        code = 0,
        out = "task — механика процесса: ветки, PR, доска. Команды:\n\n" +
            "  task start <N> <slug> [--long | --from long/<M>-…] — ветка task/<N>-<slug> от origin/dev, " +
            "статус In progress\n" +
            "  task pr --title «…» --body-file <файл> [--check «команда»]… [--part] | --ready — " +
            "черновик PR по шаблону; --ready — к ревью\n" +
            "  task done — после слияния PR: свежий dev, ветка удалена, статус Done\n" +
            "  task final <N> --title «…» --body-file <файл> [--check «команда»]… — итоговый PR long/<N>-… → dev\n" +
            "  task stage create --title «…» --body-file <файл> [--due ГГГГ-ММ-ДД] — веха этапа по шаблону\n" +
            "  task new <тип> --title «…» --body-file <файл> [--parent N] [--blocked-by N,N] — " +
            "задача по шаблону типа в Backlog\n",
        err = "",
    )
}

fun makeNoKotlinRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "Нет Kotlin — установите: brew install kotlin\n")
}

fun makeOldKotlinRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "Kotlin 2.1.20, нужен ≥ 2.2 — обновите: brew upgrade kotlin\n")
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
): LauncherRun {
    val builder = ProcessBuilder(listOf(System.getProperty("task.launcher")) + args)
    builder.environment()["PATH"] = path
    builder.environment()["XDG_CACHE_HOME"] = System.getProperty("task.cache")
    val process = builder.start()
    val out = process.inputStream.bufferedReader().readText()
    val err = process.errorStream.bufferedReader().readText()
    return LauncherRun(code = process.waitFor(), out = out, err = err)
}
