package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а оглавление банка `docs/README.md` — единственный вход скиллов и агентов
// в банк: битая ссылка или раздел без строки тихо отключают раздел. Разбор — в FindBankIndexErrors и FindBankSections.
class BankIndexSpec : BehaviorSpec({
    shouldReturnTest(emptyList<String>(), options = TestOptions(name = "оглавление без ошибок")) {
        findBankIndexErrors()
    }

    shouldReturnTest(expected = true, options = TestOptions(name = "разделы банка найдены")) {
        findBankSections().isNotEmpty()
    }
})
