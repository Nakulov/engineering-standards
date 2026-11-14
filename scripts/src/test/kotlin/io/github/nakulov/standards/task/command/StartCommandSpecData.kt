package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.BoardItem
import io.github.nakulov.standards.task.github.IssueInfo

fun makeTaskInfo(): IssueInfo {
    return IssueInfo(number = 13, open = true, type = "type:task", openBlockers = emptyList())
}

fun makeResearchInfo(): IssueInfo {
    return IssueInfo(number = 13, open = true, type = "type:research", openBlockers = emptyList())
}

fun makeEpicInfo(): IssueInfo {
    return IssueInfo(number = 13, open = true, type = "type:epic", openBlockers = emptyList())
}

fun makeClosedInfo(): IssueInfo {
    return IssueInfo(number = 13, open = false, type = "type:task", openBlockers = emptyList())
}

fun makeBlockedInfo(): IssueInfo {
    return IssueInfo(number = 13, open = true, type = "type:task", openBlockers = listOf(3, 8))
}

fun makeGroomedItem(): BoardItem {
    return makeItem(status = "Groomed")
}

fun makeInProgressItem(): BoardItem {
    return makeItem(status = "In progress")
}

fun makeBacklogItem(): BoardItem {
    return makeItem(status = "Backlog")
}

fun makeDoneItem(): BoardItem {
    return makeItem(status = "Done")
}

fun makeDirtyMessage(): String {
    return "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их"
}

fun makeUsageMessage(): String {
    return "использование: task start <N> <slug> [--long] — ветка task/<N>-<slug> от origin/dev, статус In progress"
}

private fun makeItem(status: String): BoardItem {
    return BoardItem(
        id = "PVTI_13",
        projectId = "PVT_10",
        fieldId = "PVTSSF_status",
        status = status,
        options = mapOf("Groomed" to "g1", "In progress" to "p1", "Done" to "d1"),
    )
}
