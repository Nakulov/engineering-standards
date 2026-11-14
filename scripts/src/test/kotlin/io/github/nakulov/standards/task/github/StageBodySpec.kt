package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

class StageBodySpec :
    BehaviorSpec({
        shouldReturnTest(emptyList<String>()) {
            StageBody.emptySections(template = makeStageTemplate(), text = makeResource(path = "stage/full.md"))
        }

        shouldReturnTest(listOf("Критерий завершения", "Не входит", "Эпики и порядок")) {
            StageBody.emptySections(template = makeStageTemplate(), text = makeResource(path = "stage/partial.md"))
        }

        shouldReturnTest(listOf("Цель", "Критерий завершения", "Не входит", "Эпики и порядок")) {
            StageBody.emptySections(template = makeStageTemplate(), text = "")
        }
    })
