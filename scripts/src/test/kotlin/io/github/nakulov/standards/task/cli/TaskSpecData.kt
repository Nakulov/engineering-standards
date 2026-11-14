package io.github.nakulov.standards.task.cli

fun makeHelpTitle(): String {
    return "task — механика процесса: ветки, PR, доска. Команды:"
}

fun makeNoCommandsLine(): String {
    return "  (команды появятся в следующих PR задачи #13)"
}

fun makeStartUsage(): String {
    return "<N> <slug> — взять задачу в работу"
}

fun makeStartHelpLine(): String {
    return "  task start <N> <slug> — взять задачу в работу"
}

fun makeUnknownCommandError(): String {
    return "неизвестная команда nope — см. task --help"
}

fun makeRefusalMessage(): String {
    return "#13 в статусе Backlog — сначала груминг"
}

fun makeRefusal(): TaskError.Refusal {
    return TaskError.Refusal(message = makeRefusalMessage())
}

fun makeCrashMessage(): String {
    return "сбой скрипта: java.lang.IllegalStateException: нет ответа — подробности: TASK_DEBUG=1"
}

fun makeFailureMessage(): String {
    return "gh: HTTP 502 — повторите позже"
}

fun makeFailure(): TaskError.Failure {
    return TaskError.Failure(message = makeFailureMessage())
}

fun makeFailureTraceStart(): String {
    return "io.github.nakulov.standards.task.cli.TaskError\$Failure: gh: HTTP 502 — повторите позже"
}
