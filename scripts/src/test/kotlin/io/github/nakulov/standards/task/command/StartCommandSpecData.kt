package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.board.BoardItem
import io.github.nakulov.standards.task.github.IssueInfo
import java.io.File
import java.nio.file.Files

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
    return "использование: task start <N> <slug> [--long | --from long/<M>-…] [--worktree] — ветка task/<N>-<slug> " +
        "от origin/dev, статус In progress; --worktree — в копии .claude/worktrees/<slug>"
}

fun makeNoLongBranchMessage(): String {
    return "нет ветки origin/long/13-big — сначала task start <N> <slug> --long"
}

fun makeFastForwardLine(): String {
    return "ветка long/13-task-scripts уже есть — переключился и перемотал до origin/long/13-task-scripts"
}

fun makeDivergedMessage(): String {
    return "локальная task/13-task-scripts расходится с origin/task/13-task-scripts — сведите вручную"
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

fun makeCopyPath(): String {
    return "/repo/.claude/worktrees/task-scripts"
}

fun makeOpenedMessage(folder: String): String {
    return "ветка task/13-task-scripts открыта в $folder — работайте там"
}

fun makeOccupiedMessage(folder: String): String {
    return "папка $folder занята — работайте там или удалите её"
}

/** Существующая папка копии: `File.exists` в команде — настоящий, папка во временном каталоге `build/` модуля. */
fun makeExistingCopy(): File {
    val base = File(System.getProperty("task.cache"), "start")
    base.mkdirs()
    return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "copy").toFile()
}
