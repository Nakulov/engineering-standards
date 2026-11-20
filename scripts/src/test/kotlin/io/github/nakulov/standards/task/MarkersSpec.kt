package io.github.nakulov.standards.task

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а пары файлов — блоки между маркерами в них копии 1 в 1:
// `plan:*` — `skills/decompose/SKILL.md` и `agents/plan-reviewer.md` (#46),
// `execute:*` — `skills/execute/SKILL.md` и `agents/plan-executor.md` (#48): правило в скилле и в агенте не расходится,
// `rules:*` — `rules/bash-commands.md` и 4 агента с Bash (#98): правила плагина грузятся только основной сессии.
// Разбор — в MarkersSpecData.
class MarkersSpec :
    BehaviorSpec({
        shouldReturnTest(
            emptyList<String>(),
            options = TestOptions(name = "блоки plan:*, execute:* и rules:* — копии 1 в 1"),
        ) {
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
                "rules:bash-commands",
            ),
            options = TestOptions(name = "в источниках все блоки plan:*, execute:* и rules:*"),
        ) {
            findSourceMarkers()
        }
    })
