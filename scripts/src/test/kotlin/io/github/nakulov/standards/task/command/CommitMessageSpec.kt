package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class CommitMessageSpec :
    BehaviorSpec({
        shouldReturnTest(makeCommitMessage()) {
            CommitMessage.parse(text = makeCommitText(), plan = makePlanPath())
        }

        shouldReturnTest(makeCommitMessage()) {
            CommitMessage.parse(text = makeCommitText(), plan = null)
        }

        shouldReturnTest(makeNoPlanTaskCommitMessage()) {
            CommitMessage.parse(text = makeNoPlanTaskCommitText(), plan = null)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("первая строка сообщения пуста — напишите, что сделано") {
            CommitMessage.parse(text = makeNoTitleText(), plan = makePlanPath())
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "в ветке план `plans/commit-commands.md` — нет строки «Задача плана: …», добавьте заголовок задачи плана",
        ) {
            CommitMessage.parse(text = makeNoPlanTaskText(), plan = makePlanPath())
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "раздел «Файлы:» пуст — перечислите файлы строками «- `путь` — что изменено»",
        ) {
            CommitMessage.parse(text = makeNoFilesText(), plan = makePlanPath())
        }
    })
