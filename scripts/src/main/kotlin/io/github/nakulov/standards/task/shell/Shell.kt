package io.github.nakulov.standards.task.shell

/**
 * Узкий слой внешних команд (`git`, `gh`): в спеках подменяется, логика тестируется без сети и репозитория.
 */
interface Shell {
    fun run(command: List<String>): ShellResult
}
