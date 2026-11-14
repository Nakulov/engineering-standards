package io.github.nakulov.standards.task.cli

/**
 * Вывод в терминал: строки — в stdout, ошибки — в stderr.
 */
object SystemConsole : Console {
    override fun line(text: String) {
        println(text)
    }

    override fun error(text: String) {
        System.err.println(text)
    }
}
