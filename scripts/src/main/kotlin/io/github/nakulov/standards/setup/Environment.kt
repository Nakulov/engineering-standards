package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub

/**
 * Проверка до первого шага: у токена `gh` есть нужные права, у пользователя — права администратора репозитория.
 * Kotlin и JDK проверяет запускатель: без них `setup` не запустился бы.
 */
class Environment(
    private val gitHub: GitHub,
) {
    fun verify() {
        val headers = gitHub.read(args = listOf("api", "user", "--include")).lines()
        val scopes = headers.firstOrNull { line -> line.startsWith(prefix = SCOPES_HEADER, ignoreCase = true) }
            ?.substringAfter(delimiter = ":")?.split(",")?.map { scope -> scope.trim() }.orEmpty()
        val missing = REQUIRED_SCOPES.filter { scope -> scope !in scopes }
        if (missing.isNotEmpty()) {
            val names = missing.joinToString(separator = ", ")
            val flags = missing.joinToString(separator = " ") { scope -> "-s $scope" }
            throw TaskError.Refusal(message = "у токена gh нет прав $names — выполните: gh auth refresh $flags")
        }
        val admin = gitHub.read(args = listOf("api", "repos/{owner}/{repo}", "--jq", ".permissions.admin"))
        if (admin != "true") {
            throw TaskError.Refusal(message = "нет прав администратора репозитория — настройки меняет его владелец")
        }
    }

    private companion object {
        const val SCOPES_HEADER = "X-OAuth-Scopes"
        val REQUIRED_SCOPES = listOf("repo", "project")
    }
}
