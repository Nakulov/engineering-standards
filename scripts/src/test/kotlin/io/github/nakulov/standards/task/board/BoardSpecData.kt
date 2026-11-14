package io.github.nakulov.standards.task.board

fun makeVariables(): Map<String, String> {
    return mapOf("id" to "I_issue13")
}

fun makeItemRows(): List<String> {
    return listOf("PVTI_13\tPVT_10\tPVTSSF_status\tGroomed\tBacklog=b1;Groomed=g1;In progress=p1;In review=r1;Done=d1")
}

fun makeTwoRows(): List<String> {
    return listOf(
        "PVTI_13\tPVT_10\tPVTSSF_status\tGroomed\tGroomed=g1",
        "PVTI_13b\tPVT_8\tPVTSSF_other\tBacklog\tBacklog=b2",
    )
}

fun makeItem(): BoardItem {
    return BoardItem(
        id = "PVTI_13",
        projectId = "PVT_10",
        fieldId = "PVTSSF_status",
        status = "Groomed",
        options = mapOf(
            "Backlog" to "b1",
            "Groomed" to "g1",
            "In progress" to "p1",
            "In review" to "r1",
            "Done" to "d1",
        ),
    )
}

fun makeMoveArgs(): List<String> {
    return listOf(
        "project",
        "item-edit",
        "--id",
        "PVTI_13",
        "--project-id",
        "PVT_10",
        "--field-id",
        "PVTSSF_status",
        "--single-select-option-id",
        "p1",
    )
}

fun makeMissingStatusMessage(): String {
    return "на доске нет статуса «Testing» — статусы по стандарту: Backlog, Groomed, In progress, In review, Done"
}

fun makeRepoArgs(): List<String> {
    return listOf("repo", "view", "--json", "owner,name", "--jq", ".owner.login + \"\\t\" + .name")
}

fun makeRepoVariables(): Map<String, String> {
    return mapOf("owner" to "owner", "name" to "repo")
}

fun makeItemsPath(): String {
    return "users/owner/projectsV2/10/items"
}

fun makeAddArgs(): List<String> {
    return listOf("api", "--method", "POST", "users/owner/projectsV2/10/items", "-f", "type=Issue", "-F", "id=9013")
}

fun makeInvisibleMessage(): String {
    return "#13 добавлена, но не видна в списке доски — проверьте доску вручную"
}

fun makeNoBoardMessage(): String {
    return "у репозитория нет связанной доски — свяжите с репозиторием одну доску"
}
