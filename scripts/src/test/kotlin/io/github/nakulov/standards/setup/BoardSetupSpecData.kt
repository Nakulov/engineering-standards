package io.github.nakulov.standards.setup

import java.io.File

private const val AREA = "доска"

fun makeTemplate(): BoardSetup.Template {
    return BoardSetup.Template(owner = "owner", number = 12)
}

fun makeTemplateError(): String {
    return "board.yml: нужны строки «owner: <владелец>» и «number: <номер доски>»"
}

fun makeBoardScratch(): File {
    return File(System.getProperty("task.cache"), "board")
}

fun makeRepositoryJq(): String {
    return ".data.repository | [.id, .owner.id, .name] | @tsv"
}

fun makeTemplateJq(): String {
    return ".data.repositoryOwner.projectV2 | " +
        "[.id, ([.workflows.nodes[] | select(.enabled) | .name] | join(\";\"))] | @tsv"
}

fun makeCopyJq(): String {
    return ".data.copyProjectV2.projectV2 | [.id, .url] | @tsv"
}

fun makeOptionsJq(): String {
    return ".data.node.options[] | [.id, .name, .color, .description] | @tsv"
}

fun makeRepositoryVariables(): Map<String, String> {
    return mapOf("owner" to "{owner}", "name" to "{repo}")
}

/** Шаблон: id и включённые workflow. */
fun makeTemplateRow(): String {
    return "PVT_template\tAuto-add sub-issues to project;Item closed"
}

fun makeBoardUrl(): String {
    return "https://github.com/users/owner/projects/10"
}

/** Доска: id, url, id поля Status, статусы, включённые workflow. */
fun makeStandardBoardRow(): String {
    return "PVT_board\t${makeBoardUrl()}\tPVTSSF_status\t" +
        "a=Backlog;b=Groomed;c=In progress;d=In review;e=Done\tAuto-add sub-issues to project;Item closed"
}

/** Нет In review, есть Blocked, «Item closed» выключен. */
fun makeDriftBoardRow(): String {
    return "PVT_board\t${makeBoardUrl()}\tPVTSSF_status\t" +
        "a=Backlog;b=Groomed;c=In progress;x=Blocked;e=Done\tAuto-add sub-issues to project"
}

/** Статусы поля строками `@tsv`: у Blocked описание с кавычками и табуляцией. */
fun makeOptionRows(): String {
    return listOf(
        "a\tBacklog\tGRAY\t",
        "b\tGroomed\tBLUE\t",
        "c\tIn progress\tYELLOW\t",
        "x\tBlocked\tRED\tждёт \"внешнего\"\\tответа",
        "e\tDone\tGREEN\t",
    ).joinToString(separator = "\n")
}

fun makeBoardFinding(
    status: Finding.Status,
    text: String,
): Finding {
    return Finding(status = status, area = AREA, text = text)
}

fun makeNoBoardFinding(): Finding {
    return makeBoardFinding(status = Finding.Status.DEVIATION, text = "нет доски, связанной с репозиторием")
}

fun makeCopyRow(): String {
    return "PVT_new\thttps://github.com/users/owner/projects/13"
}

fun makeCopyFields(): List<String> {
    return listOf("template=PVT_template", "owner=U_owner", "title=app")
}

fun makeLinkFields(): List<String> {
    return listOf("project=PVT_new", "repository=R_repo")
}

fun makeCreatedFinding(): Finding {
    val text = "создана доска «app» (https://github.com/users/owner/projects/13) — копия шаблона, " +
        "связана с репозиторием"
    return makeBoardFinding(status = Finding.Status.FIXED, text = text)
}

fun makeDriftFindings(): List<Finding> {
    return listOf(
        makeBoardFinding(status = Finding.Status.FIXED, text = "нет статусов: In review"),
        makeBoardFinding(
            status = Finding.Status.NOTE,
            text = "статусы вне стандарта: Blocked — не удаляются: на них могут стоять задачи",
        ),
        makeBoardFinding(
            status = Finding.Status.DEVIATION,
            text = "выключены workflow: Item closed — включите: ${makeBoardUrl()}/workflows",
        ),
    )
}

fun makeTwoBoardsFinding(): Finding {
    return makeBoardFinding(
        status = Finding.Status.DEVIATION,
        text = "с репозиторием связано досок: 2 — task работает с одной, лишние отвяжите",
    )
}

/** Тело `updateProjectV2Field`: статусы стандарта по порядку (текущие — со своими id, новый — без), затем лишние. */
fun makeStatusesBody(): String {
    val options = listOf(
        "{\"id\":\"a\",\"name\":\"Backlog\",\"color\":\"GRAY\",\"description\":\"\"}",
        "{\"id\":\"b\",\"name\":\"Groomed\",\"color\":\"BLUE\",\"description\":\"\"}",
        "{\"id\":\"c\",\"name\":\"In progress\",\"color\":\"YELLOW\",\"description\":\"\"}",
        "{\"name\":\"In review\",\"color\":\"GRAY\",\"description\":\"\"}",
        "{\"id\":\"e\",\"name\":\"Done\",\"color\":\"GREEN\",\"description\":\"\"}",
        "{\"id\":\"x\",\"name\":\"Blocked\",\"color\":\"RED\",\"description\":\"ждёт \\\"внешнего\\\"\\tответа\"}",
    )
    return "{\"query\":\"${makeUpdateMutation()}\",\"variables\":{\"field\":\"PVTSSF_status\"," +
        "\"options\":[${options.joinToString(separator = ",")}]}}"
}

fun makeUpdateMutation(): String {
    return "mutation(\$field:ID!,\$options:[ProjectV2SingleSelectFieldOptionInput!]){" +
        "updateProjectV2Field(input:{fieldId:\$field,singleSelectOptions:\$options}){projectV2Field{" +
        "... on ProjectV2SingleSelectField{id}}}}"
}
