package io.github.nakulov.standards.setup

private const val AREA = "метки"

fun makeStandardLabels(): List<Labels.Label> {
    return listOf(
        Labels.Label(name = "type:task", color = "0E8A16", description = "Тип: задача — изменение с решением и PR"),
        Labels.Label(name = "type:bug", color = "D73A4A", description = "Тип: баг — работает не так, как должно"),
    )
}

/** `labels.yml` с комментарием и пустой строкой — их разбор пропускает. */
fun makeLabelsText(): String {
    return "# Метки типа задачи\n\n" +
        "- name: type:task\n  color: 0E8A16\n  description: \"Тип: задача — изменение с решением и PR\"\n" +
        "- name: type:bug\n  color: D73A4A\n  description: \"Тип: баг — работает не так, как должно\"\n"
}

fun makeBadLineMessage(): String {
    return "labels.yml: «color: 0E8A16» — ожидается «- name: …» или «  ключ: значение»"
}

/** Описание с «: » без кавычек — невалидный YAML. */
fun makeUnquotedText(): String {
    return "- name: type:task\n  color: 0E8A16\n  description: Тип: задача"
}

fun makeUnquotedMessage(): String {
    return "labels.yml: «  description: Тип: задача» — значение с «: » или «#» — в кавычках, без \" и \\"
}

fun makeLabelsPath(): String {
    return "repos/{owner}/{repo}/labels"
}

fun makeLabelsJq(): String {
    return ".[] | \"\\(.name)\\t\\(.color)\\t\\(.description // \"\")\""
}

fun makeUsageArgs(encoded: String): List<String> {
    return listOf("api", "repos/{owner}/{repo}/issues?state=all&per_page=1&labels=$encoded", "--jq", "length")
}

/** Метки типа по стандарту (цвет в другом регистре), стандартные GitHub и тематическая. */
fun makeExistingRows(): List<String> {
    return listOf(
        "type:task\t0e8a16\tТип: задача — изменение с решением и PR",
        "type:bug\tD73A4A\tТип: баг — работает не так, как должно",
        "bug\td73a4a\tSomething isn't working",
        "good first issue\t7057ff\tGood for newcomers",
        "process\tFBCA04\t",
    )
}

/** Нет `type:bug`, у `type:task` другой цвет, стандартные метки GitHub, старая метка эпика. */
fun makeDriftRows(): List<String> {
    return listOf(
        "type:task\tFFFFFF\tТип: задача — изменение с решением и PR",
        "bug\td73a4a\tSomething isn't working",
        "good first issue\t7057ff\tGood for newcomers",
        "epic\tB60205\tКрупная тема",
    )
}

fun makeCheckFindings(): List<Finding> {
    return listOf(
        Finding(status = Finding.Status.OK, area = AREA, text = "метки типа соответствуют стандарту"),
        Finding(
            status = Finding.Status.DEVIATION,
            area = AREA,
            text = "стандартная метка GitHub bug (не используется) — удаляется",
        ),
        Finding(
            status = Finding.Status.DEVIATION,
            area = AREA,
            text = "стандартная метка GitHub good first issue (стоит на задачах) — удаляется",
        ),
        makeForeignNote(names = "process"),
    )
}

fun makeApplyFindings(): List<Finding> {
    return listOf(
        Finding(status = Finding.Status.FIXED, area = AREA, text = "type:task отличается цветом или описанием"),
        Finding(status = Finding.Status.FIXED, area = AREA, text = "нет метки type:bug"),
        makeBugDeleted(),
        Finding(
            status = Finding.Status.NEEDS_CONFIRM,
            area = AREA,
            text = "удалит стандартную метку GitHub good first issue (стоит на задачах)",
        ),
        makeForeignNote(names = "epic"),
    )
}

fun makeConfirmedFindings(): List<Finding> {
    return listOf(
        Finding(status = Finding.Status.OK, area = AREA, text = "метки типа соответствуют стандарту"),
        makeBugDeleted(),
        Finding(
            status = Finding.Status.FIXED,
            area = AREA,
            text = "стандартная метка GitHub good first issue удалена (стоит на задачах)",
        ),
        makeForeignNote(names = "process"),
    )
}

fun makeBugDeleted(): Finding {
    val text = "стандартная метка GitHub bug удалена (не используется)"
    return Finding(status = Finding.Status.FIXED, area = AREA, text = text)
}

fun makeForeignNote(names: String): Finding {
    val text = "вне стандарта: $names — тип задачи только type:*, перенос старой метки — вручную: " +
        "gh label edit <старая> --name type:…"
    return Finding(status = Finding.Status.NOTE, area = AREA, text = text)
}

fun makeCreateBugArgs(): List<String> {
    return listOf(
        "api",
        "--method",
        "POST",
        "repos/{owner}/{repo}/labels",
        "-f",
        "name=type:bug",
        "-f",
        "color=D73A4A",
        "-f",
        "description=Тип: баг — работает не так, как должно",
    )
}

fun makePatchTaskArgs(): List<String> {
    return listOf(
        "api",
        "--method",
        "PATCH",
        "repos/{owner}/{repo}/labels/type%3Atask",
        "-f",
        "color=0E8A16",
        "-f",
        "description=Тип: задача — изменение с решением и PR",
    )
}

fun makeDeleteArgs(encoded: String): List<String> {
    return listOf("api", "--method", "DELETE", "repos/{owner}/{repo}/labels/$encoded")
}
