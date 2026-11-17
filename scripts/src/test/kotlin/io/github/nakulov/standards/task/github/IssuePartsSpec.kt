package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class IssuePartsSpec :
    BehaviorSpec({
        shouldReturnTest(makeMergedBody(), options = TestOptions(name = "дописать метки")) {
            IssueParts.merge(body = makeIssueBody(), parts = makeParts(), planLine = PLAN_LINE)
        }

        shouldReturnTest(makeRemergedBody(), options = TestOptions(name = "повтор заменяет содержимое меток")) {
            IssueParts.merge(body = makeMergedBody(), parts = makeNewParts(), planLine = PLAN_LINE)
        }

        shouldReturnTest(makeMergedBody(), options = TestOptions(name = "повтор без изменений")) {
            IssueParts.merge(body = makeMergedBody(), parts = makeParts(), planLine = PLAN_LINE)
        }

        shouldReturnTest(makeRemergedBody(), options = TestOptions(name = "повтор на теле с \\r\\n")) {
            val body = makeMergedBody().replace(oldValue = "\n", newValue = "\r\n")
            IssueParts.merge(body = body, parts = makeNewParts(), planLine = PLAN_LINE)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "в задаче нет раздела «Решено» — исправьте заголовок в файле частей",
        ) {
            IssueParts.merge(body = makeIssueBody(), parts = "## Решено\n- x\n", planLine = PLAN_LINE)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "в разделе «Отклонено» метка <!-- план --> без пары — поправьте тело задачи",
        ) {
            IssueParts.merge(body = makeBrokenBody(), parts = makeParts(), planLine = PLAN_LINE)
        }
    })
