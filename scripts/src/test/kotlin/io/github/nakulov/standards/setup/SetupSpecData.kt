package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError

fun makeCheckMode(): Setup.Mode {
    return Setup.Mode(apply = false, confirmed = false)
}

fun makeApplyMode(): Setup.Mode {
    return Setup.Mode(apply = true, confirmed = false)
}

fun makeConfirmedMode(): Setup.Mode {
    return Setup.Mode(apply = true, confirmed = true)
}

fun makeUsageError(): String {
    return "использование: setup [--check | --confirm]"
}

fun makeOkFinding(): Finding {
    return Finding(status = Finding.Status.OK, area = "настройки", text = "соответствуют стандарту")
}

fun makeWaitingFinding(): Finding {
    val text = "обязательной проверки нет: CI не настроен"
    return Finding(status = Finding.Status.WAITING_CI, area = "защита", text = text)
}

fun makeUnavailableFinding(): Finding {
    return Finding(status = Finding.Status.UNAVAILABLE, area = "защита", text = "приватный репозиторий")
}

fun makeFixedFinding(): Finding {
    return Finding(status = Finding.Status.FIXED, area = "настройки", text = "has_wiki: true → false")
}

fun makeNeedsConfirmFinding(): Finding {
    return Finding(status = Finding.Status.NEEDS_CONFIRM, area = "метки", text = "удалит стандартную метку GitHub bug")
}

fun makeScopeRefusal(): TaskError.Refusal {
    return TaskError.Refusal(message = "у токена gh нет прав project — выполните: gh auth refresh -s project")
}

fun makeCrashMessage(): String {
    return "сбой скрипта: java.lang.IllegalStateException: нет ответа — подробности: TASK_DEBUG=1"
}
