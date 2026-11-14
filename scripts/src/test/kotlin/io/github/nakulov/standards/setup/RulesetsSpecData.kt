package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.TaskError
import java.io.File

private const val AREA = "защита"

fun makeScratch(): File {
    return File(System.getProperty("task.cache"), "rulesets")
}

fun makeRulesetsPath(): String {
    return "repos/{owner}/{repo}/rulesets"
}

fun makeRulesetsJq(): String {
    return ".[] | \"\\(.id)\\t\\(.name)\""
}

fun makeCheckRunsArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/commits/dev/check-runs?check_name=check", "--jq", ".total_count")
}

fun makeWorkflowsArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/actions/workflows", "--jq", ".total_count")
}

fun makeClassicArgs(): List<String> {
    return listOf("api", "repos/{owner}/{repo}/branches/dev/protection", "--jq", ".url")
}

fun makeDeleteClassicArgs(): List<String> {
    return listOf("api", "--method", "DELETE", "repos/{owner}/{repo}/branches/dev/protection")
}

/** Начало аргументов чтения ruleset'а; дальше — `--jq` сверки. */
fun makeRulesetPrefix(id: String): List<String> {
    return listOf("api", "repos/{owner}/{repo}/rulesets/$id", "--jq")
}

fun makeNotProtected(): String {
    return "gh: Branch not protected (HTTP 404)"
}

fun makeUpgradeFailure(): TaskError.Failure {
    val message = "gh: Upgrade to GitHub Pro or make this repository public to enable this feature. (HTTP 403)"
    return TaskError.Failure(message = message)
}

fun makeUnavailable(): Finding {
    val text = "приватный репозиторий на бесплатном плане — защиты веток и тегов нет"
    return Finding(status = Finding.Status.UNAVAILABLE, area = AREA, text = text)
}

fun makeEmptyRepositoryCheck(): List<Finding> {
    return listOf(
        Finding(status = Finding.Status.WAITING_CI, area = AREA, text = "обязательной проверки нет: CI не настроен"),
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = "нет ruleset standards/dev"),
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = "нет ruleset standards/long"),
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = "нет ruleset standards/tags"),
    )
}

fun makeStandardRows(): List<String> {
    return listOf("1\tstandards/dev", "2\tstandards/long", "3\tstandards/tags")
}

fun makeForeignRows(): List<String> {
    return makeStandardRows() + "7\tfeature long-term"
}

/** Ruleset `standards/dev` строками `--jq` сверки, без обязательной проверки. */
fun makeDevLines(threads: String): String {
    return listOf(
        "target=branch",
        "enforcement=active",
        "include=refs/heads/dev",
        "bypass=0",
        "rule=deletion",
        "rule=non_fast_forward",
        "rule=pull_request",
        "pull_request.required_approving_review_count=0",
        "pull_request.dismiss_stale_reviews_on_push=false",
        "pull_request.required_reviewers=[]",
        "pull_request.require_code_owner_review=false",
        "pull_request.require_last_push_approval=false",
        "pull_request.required_review_thread_resolution=$threads",
        "pull_request.allowed_merge_methods=[\"squash\"]",
    ).joinToString(separator = "\n")
}

fun makeLongLines(): String {
    return makeDevLines(threads = "true").replace(oldValue = "refs/heads/dev", newValue = "refs/heads/long/**")
        .replace(oldValue = "rule=deletion\n", newValue = "")
}

fun makeTagsLines(): String {
    return listOf(
        "target=tag",
        "enforcement=active",
        "include=refs/tags/v*",
        "bypass=0",
        "rule=deletion",
        "rule=update",
        "rule=non_fast_forward",
    ).joinToString(separator = "\n")
}

fun makeForeignCheck(): List<Finding> {
    val text = "workflow есть, а прогона check на dev нет — обязательная проверка должна называться check"
    return listOf(
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = text),
        Finding(status = Finding.Status.OK, area = AREA, text = "ruleset standards/dev"),
        Finding(status = Finding.Status.OK, area = AREA, text = "ruleset standards/long"),
        Finding(status = Finding.Status.OK, area = AREA, text = "ruleset standards/tags"),
        Finding(status = Finding.Status.NOTE, area = AREA, text = "ruleset вне стандарта: feature long-term"),
    )
}

fun makeDevDiffers(): List<Finding> {
    val text = "ruleset standards/dev отличается: pull_request.required_review_thread_resolution=true"
    return listOf(
        Finding(status = Finding.Status.WAITING_CI, area = AREA, text = "обязательной проверки нет: CI не настроен"),
        Finding(status = Finding.Status.DEVIATION, area = AREA, text = text),
    )
}

fun makeCreatedFindings(): List<Finding> {
    return listOf(
        Finding(status = Finding.Status.OK, area = AREA, text = "обязательная проверка check"),
        Finding(status = Finding.Status.FIXED, area = AREA, text = "создан ruleset standards/dev"),
        Finding(status = Finding.Status.FIXED, area = AREA, text = "создан ruleset standards/long"),
        Finding(status = Finding.Status.FIXED, area = AREA, text = "создан ruleset standards/tags"),
        Finding(
            status = Finding.Status.NEEDS_CONFIRM,
            area = AREA,
            text = "удалит классическую защиту dev (ruleset standards/dev уже действует)",
        ),
    )
}

fun makeClassicDeleted(): Finding {
    return Finding(status = Finding.Status.FIXED, area = AREA, text = "классическая защита dev удалена")
}

fun makeDevJson(): String {
    return "{\"name\":\"standards/dev\",\"target\":\"branch\",\"enforcement\":\"active\",\"bypass_actors\":[]," +
        "\"conditions\":{\"ref_name\":{\"include\":[\"refs/heads/dev\"],\"exclude\":[]}},\"rules\":[" +
        "{\"type\":\"deletion\"},{\"type\":\"non_fast_forward\"},{\"type\":\"pull_request\",\"parameters\":{" +
        "\"required_approving_review_count\":0,\"dismiss_stale_reviews_on_push\":false," +
        "\"require_code_owner_review\":false,\"require_last_push_approval\":false," +
        "\"required_review_thread_resolution\":true,\"allowed_merge_methods\":[\"squash\"]}}," +
        "{\"type\":\"required_status_checks\",\"parameters\":{\"strict_required_status_checks_policy\":true," +
        "\"required_status_checks\":[{\"context\":\"check\"}]}}]}"
}
