package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.TaskError

fun makePublishedBody(): String {
    return "Closes #13\n\n" +
        "## Что меняет PR\n\nКоманда `task pr`.\n\n## Проверки\n**Автоматически:**\n" +
        "- `./gradlew test` — прошла\n- `scripts/task --help` — прошла\n**Вручную:**\n- [ ] Черновик открыт.\n"
}

fun makeFailedCheckRefusal(): TaskError.Refusal {
    return TaskError.Refusal(message = makeFailedCheckMessage())
}
