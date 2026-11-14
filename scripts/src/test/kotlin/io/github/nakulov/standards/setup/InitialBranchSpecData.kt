package io.github.nakulov.standards.setup

private const val AREA = "ветки"

fun makeStandardPaths(): List<String> {
    return listOf(".github", ".gitignore", ".editorconfig")
}

fun makeBranchNotFound(): String {
    return "gh: Branch not found (HTTP 404)"
}

fun makeFirstCommitMessage(): String {
    return "Начальная настройка по стандарту"
}

fun makeBranchesArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/branches?per_page=1", "--jq", "length")
}

fun makeDefaultBranchArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}", "--jq", ".default_branch")
}

fun makeCommitsArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/commits?sha=main&per_page=2", "--jq", "length")
}

fun makeDevBranchArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/branches/dev", "--jq", ".name")
}

fun makeMainShaArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/git/ref/heads/main", "--jq", ".object.sha")
}

fun makeCreateDevArgs(): List<String> {
    return listOf("api", "--method", "POST", "repos/{owner}/{repo}/git/refs") +
        listOf("-f", "ref=refs/heads/dev", "-f", "sha=abc123")
}

fun makeSetDefaultArgs(): List<String> {
    return listOf("api", "--method", "PATCH", "repos/{owner}/{repo}", "-f", "default_branch=dev")
}

fun makeDeleteMainArgs(): List<String> {
    return listOf("api", "--method", "DELETE", "repos/{owner}/{repo}/git/refs/heads/main")
}

fun makeBranchFinding(
    status: Finding.Status,
    text: String,
): Finding {
    return Finding(status = status, area = AREA, text = text)
}

fun makeEmptyCheck(): Finding {
    return makeBranchFinding(
        status = Finding.Status.DEVIATION,
        text = "репозиторий пустой — setup сделает первый коммит в dev с файлами стандарта",
    )
}

fun makeFirstCommitFinding(): Finding {
    val text = "первый коммит в dev — файлы стандарта; dev — основная ветка"
    return makeBranchFinding(status = Finding.Status.FIXED, text = text)
}

fun makeLocalCommitsFinding(): Finding {
    return makeBranchFinding(
        status = Finding.Status.DEVIATION,
        text = "на сервере коммитов нет, а локально есть — отправьте их сами (ветка dev), затем запустите setup",
    )
}

fun makeReplaceFinding(status: Finding.Status): Finding {
    val text = if (status == Finding.Status.FIXED) {
        "dev создана от main и стала основной, main удалена — локально: git fetch --prune && git switch dev"
    } else {
        "заменит основную ветку main (один начальный коммит) на dev и удалит main"
    }
    return makeBranchFinding(status = status, text = text)
}

fun makeHistoryFinding(): Finding {
    return makeBranchFinding(
        status = Finding.Status.DEVIATION,
        text = "основная ветка main, по стандарту dev — в ней история, переход делает человек",
    )
}
