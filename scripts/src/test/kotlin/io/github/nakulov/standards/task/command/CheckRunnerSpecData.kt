package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.shell.ShellResult

fun makeCheckCommand(): List<String> {
    return listOf("sh", "-c", "./gradlew test")
}

fun makeFailedCheck(): ShellResult {
    return ShellResult(code = 1, out = "> Task :test FAILED\n", err = "\nBUILD FAILED in 3s\n\n")
}

fun makeFailedCheckMessage(): String {
    return "проверка «./gradlew test» не прошла — исправьте: BUILD FAILED in 3s"
}

fun makePassedCheck(): ShellResult {
    return ShellResult(code = 0, out = "BUILD SUCCESSFUL\n", err = "")
}
