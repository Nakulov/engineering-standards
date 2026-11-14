package io.github.nakulov.standards.setup

/**
 * Результат шага `setup` — одна строка отчёта: что сверялось ([area]) и что с ним ([text]).
 */
data class Finding(
    val status: Status,
    val area: String,
    val text: String,
) {
    fun line(): String {
        return "[${status.label}] $area: $text"
    }

    /**
     * [failing] — статус даёт код выхода 1. «Недоступно» и «ждёт CI» не роняют проверку: их не исправить запуском
     * `setup`, а вечный красный статус перестают замечать (#8).
     */
    enum class Status(
        val label: String,
        val failing: Boolean,
    ) {
        OK(label = "ok", failing = false),
        FIXED(label = "исправлено", failing = false),
        DEVIATION(label = "отклонение", failing = true),
        NEEDS_CONFIRM(label = "нужен --confirm", failing = true),
        UNAVAILABLE(label = "недоступно", failing = false),
        WAITING_CI(label = "ждёт CI", failing = false),
        NOTE(label = "заметка", failing = false),
    }
}
