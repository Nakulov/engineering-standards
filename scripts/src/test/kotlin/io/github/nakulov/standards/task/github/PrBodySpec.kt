package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class PrBodySpec :
    BehaviorSpec({
        shouldReturnTest(makeResource(path = "pr/composed.md")) {
            PrBody.compose(
                text = makeResource(path = "pr/body.md"),
                link = "Part of #13",
                checks = listOf("./gradlew :scripts:test"),
            )
        }

        shouldReturnTest(makeNoChecksBody()) {
            PrBody.compose(text = makeResource(path = "pr/body.md"), link = "Closes #13", checks = emptyList())
        }

        shouldReturnTest(makeNoLinkBody()) {
            PrBody.compose(text = makeResource(path = "pr/no-link.md"), link = "Closes #13", checks = emptyList())
        }

        // Вне стандарта: вход — настоящий шаблон PR, а не образец; ловит расхождение шаблона и task pr.
        // Разбор результата — в PrBodySpecData.
        shouldReturnTest(listOf("Closes #13"), options = TestOptions(name = "шаблон PR: связь с задачей одна")) {
            readTemplateLinks()
        }

        // Вне стандарта: см. выше.
        shouldReturnTest(makeTemplateChecks(), options = TestOptions(name = "шаблон PR: проверки на месте")) {
            readTemplateChecks()
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeNoAutomaticMessage()) {
            PrBody.compose(text = makeResource(path = "pr/no-automatic.md"), link = "Closes #13", checks = emptyList())
        }
    })
