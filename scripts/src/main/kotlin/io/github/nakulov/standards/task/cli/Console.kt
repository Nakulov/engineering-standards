package io.github.nakulov.standards.task.cli

/**
 * Вывод команды: строка на действие — в stdout, ошибка — в stderr.
 */
interface Console {
    fun line(text: String)

    fun error(text: String)
}
