package io.github.nakulov.standards.task

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а файлы шаблонов `templates/**` — в них нет конкретного содержимого
// (#35: итоги груминга #6 попали в шаблон задачи и прошли ревью и `diff`). Чтение и поиск — в TemplatesSpecData.
// Переносится в проверку шаблонов CI (#10).
class TemplatesSpec :
    BehaviorSpec({
        shouldReturnTest(emptyMap<String, List<String>>(), options = TestOptions(name = "нет дат и #N вне комментариев")) {
            findConcreteContent()
        }
    })
