package io.github.nakulov.standards.setup

fun makeUserArgs(): List<String> {
    return listOf("api", "user", "--include")
}

fun makeAdminArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}", "--jq", ".permissions.admin")
}

/** Ответ `gh api user --include`: заголовки, пустая строка, тело. */
fun makeHeaders(scopes: String): String {
    return "HTTP/2.0 200 OK\nContent-Type: application/json\nX-Oauth-Scopes: $scopes\n\n{\"login\":\"owner\"}"
}

fun makeNoProjectMessage(): String {
    return "у токена gh нет прав project — выполните: gh auth refresh -s project"
}

fun makeNoScopesMessage(): String {
    return "у токена gh нет прав repo, project — выполните: gh auth refresh -s repo -s project"
}

fun makeNotAdminMessage(): String {
    return "нет прав администратора репозитория — настройки меняет его владелец"
}
