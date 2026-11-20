package io.github.nakulov.standards.task

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а пары файлов — блоки между маркерами в них копии 1 в 1:
// `plan:*` — `skills/decompose/SKILL.md` и `agents/plan-reviewer.md` (#46),
// `execute:*` — `skills/execute/SKILL.md` и `agents/plan-executor.md` (#48): правило в скилле и в агенте не расходится.
// Разбор — в MarkersSpecData.
class MarkersSpec :
    BehaviorSpec({
        shouldReturnTest(emptyList<String>(), options = TestOptions(name = "блоки plan:* и execute:* — копии 1 в 1")) {
            findMarkerMismatches()
        }

        shouldReturnTest(
            listOf(
                "execute:unit",
                "plan:coverage",
                "plan:decomposition",
                "plan:granularity",
                "plan:no-stubs",
                "plan:types",
            ),
            options = TestOptions(name = "в скиллах все блоки plan:* и execute:*"),
        ) {
            findSkillMarkers()
        }
    })
