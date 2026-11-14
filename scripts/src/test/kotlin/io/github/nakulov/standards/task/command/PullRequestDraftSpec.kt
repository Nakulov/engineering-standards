package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class PullRequestDraftSpec :
    BehaviorSpec({
        shouldReturnTest(makeParsedDraft()) {
            PullRequestDraft.of(
                args = makeDraftArgs(),
                head = "task/13-x",
                base = "dev",
                link = "Closes #13",
                draft = true,
                usage = "task pr …",
            )
        }

        withGivens(
            nameFn = { given -> "аргументы ${given.arg1}" },
            case(arg1 = listOf("--title", "Заголовок")),
            case(arg1 = listOf("--body-file", makeBodyPath())),
            case(arg1 = listOf("--title", "Заголовок", "--body-file", makeBodyPath(), "--check")),
            case(arg1 = listOf("--title", "Заголовок", "--body-file", makeBodyPath(), "--draft", "x")),
        ) { args ->
            shouldThrowWithMessageTest<TaskError.Failure>("использование: task pr …") {
                PullRequestDraft.of(
                    args = args,
                    head = "task/13-x",
                    base = "dev",
                    link = "Closes #13",
                    draft = true,
                    usage = "task pr …",
                )
            }
        }

        shouldThrowWithMessageTest<TaskError.Failure>("нет файла no-such.md — сначала запишите текст PR по шаблону") {
            PullRequestDraft.of(
                args = listOf("--title", "Заголовок", "--body-file", "no-such.md"),
                head = "task/13-x",
                base = "dev",
                link = "Closes #13",
                draft = true,
                usage = "task pr …",
            )
        }
    })
