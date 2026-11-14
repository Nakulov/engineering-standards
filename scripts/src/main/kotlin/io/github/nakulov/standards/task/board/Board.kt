package io.github.nakulov.standards.task.board

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub

/**
 * Доска задачи. Поле Status и его варианты ищутся по именам из стандарта, id нигде не хранятся (#13, концепция).
 */
class Board(
    private val gitHub: GitHub,
) {
    /** Элемент задачи [issue] с глобальным id [nodeId]; задача должна быть ровно на одной доске. */
    fun item(
        issue: Int,
        nodeId: String,
    ): BoardItem {
        val rows = gitHub.listGraphql(query = ITEMS_QUERY, jq = ITEMS_JQ, variables = mapOf("id" to nodeId))
        if (rows.size != 1) {
            val reason = if (rows.isEmpty()) "не на доске — добавьте её на доску проекта" else "на нескольких досках"
            throw TaskError.Refusal(message = "#$issue $reason")
        }
        val (id, projectId, fieldId, status, options) = rows.first().split("\t")
        return BoardItem(
            id = id,
            projectId = projectId,
            fieldId = fieldId,
            status = status,
            options = parseOptions(text = options),
        )
    }

    private fun parseOptions(text: String): Map<String, String> {
        return text.split(";").associate { option -> option.substringBefore("=") to option.substringAfter("=") }
    }

    fun move(
        item: BoardItem,
        status: String,
    ) {
        val standard = STATUSES.joinToString(separator = ", ")
        val option = item.options[status]
            ?: throw TaskError.Failure(message = "на доске нет статуса «$status» — статусы по стандарту: $standard")
        gitHub.write(
            args = listOf(
                "project",
                "item-edit",
                "--id",
                item.id,
                "--project-id",
                item.projectId,
                "--field-id",
                item.fieldId,
                "--single-select-option-id",
                option,
            ),
        )
    }

    companion object {
        const val BACKLOG = "Backlog"
        const val GROOMED = "Groomed"
        const val IN_PROGRESS = "In progress"
        const val IN_REVIEW = "In review"
        const val DONE = "Done"
        val STATUSES = listOf(BACKLOG, GROOMED, IN_PROGRESS, IN_REVIEW, DONE)

        private const val ITEMS_QUERY = "query(\$id:ID!,\$endCursor:String){node(id:\$id){... on Issue{" +
            "projectItems(first:50,after:\$endCursor){nodes{id project{id field(name:\"Status\"){" +
            "... on ProjectV2SingleSelectField{id options{id name}}}} fieldValueByName(name:\"Status\"){" +
            "... on ProjectV2ItemFieldSingleSelectValue{name}}} pageInfo{hasNextPage endCursor}}}}}"
        private const val ITEMS_JQ = ".data.node.projectItems.nodes[] | [.id, .project.id, .project.field.id, " +
            "(.fieldValueByName.name // \"\"), ([.project.field.options[] | .name + \"=\" + .id] | join(\";\"))] | @tsv"
    }
}
