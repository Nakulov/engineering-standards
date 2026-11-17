package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class CommitMessageSpec :
    BehaviorSpec({
        shouldReturnTest(makeCommitMessage()) {
            CommitMessage.parse(text = makeCommitText())
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("первая строка сообщения пуста — напишите, что сделано") {
            CommitMessage.parse(text = makeNoTitleText())
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "нет строки «Задача плана: …» — добавьте заголовок задачи плана",
        ) {
            CommitMessage.parse(text = makeNoPlanTaskText())
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "раздел «Файлы:» пуст — перечислите файлы строками «- `путь` — что изменено»",
        ) {
            CommitMessage.parse(text = makeNoFilesText())
        }
    })
