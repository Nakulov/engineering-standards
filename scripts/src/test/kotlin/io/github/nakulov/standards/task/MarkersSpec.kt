package io.github.nakulov.standards.task

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а файлы `skills/decompose/SKILL.md` и `agents/plan-reviewer.md` —
// блоки между маркерами `plan:*` в них копии 1 в 1 (#46): правило в скилле и в агенте ревью не расходится.
// Разбор — в MarkersSpecData.
class MarkersSpec :
    BehaviorSpec({
        shouldReturnTest(emptyList<String>(), options = TestOptions(name = "блоки plan:* — копии 1 в 1")) {
            findMarkerMismatches()
        }

        shouldReturnTest(
            listOf("plan:coverage", "plan:decomposition", "plan:granularity", "plan:no-stubs", "plan:types"),
            options = TestOptions(name = "в скилле все блоки plan:*"),
        ) {
            findSkillMarkers()
        }
    })
