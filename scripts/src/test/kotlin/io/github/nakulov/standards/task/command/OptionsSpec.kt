package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class OptionsSpec :
    BehaviorSpec({
        shouldReturnTest(mapOf("--title" to "Этап 1", "--due" to "2026-12-01")) {
            Options.parse(
                args = listOf("--title", "Этап 1", "--due", "2026-12-01"),
                allowed = setOf("--title", "--due"),
                required = setOf("--title"),
                usage = "task x",
            )
        }

        withGivens(
            nameFn = { given -> "аргументы ${given.arg1}" },
            case(arg1 = listOf("--due", "2026-12-01")),
            case(arg1 = listOf("--title")),
            case(arg1 = listOf("--title", "a", "--title", "b")),
            case(arg1 = listOf("--title", "a", "--other", "b")),
        ) { args ->
            shouldThrowWithMessageTest<TaskError.Failure>("использование: task x") {
                Options.parse(
                    args = args,
                    allowed = setOf("--title", "--due"),
                    required = setOf("--title"),
                    usage = "task x",
                )
            }
        }

        shouldReturnTest(makeBodyText()) {
            Options.readFile(path = makeBodyPath(), what = "текст")
        }

        shouldThrowWithMessageTest<TaskError.Failure>("нет файла no-such.md — сначала запишите текст") {
            Options.readFile(path = "no-such.md", what = "текст")
        }
    })
