package io.github.nakulov.standards.task.board

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub

/**
 * Доска задачи. Поле Status и его варианты ищутся по именам из стандарта, id нигде не хранятся (#13, концепция).
 * Доска новой задачи — единственная доска, связанная с репозиторием.
 */
class Board(
    private val gitHub: GitHub,
    private val pause: (Long) -> Unit,
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

    /**
     * Добавить задачу на доску репозитория через REST — GraphQL-добавление оставляло элемент невидимым в списке доски
     * (ui-design-system) — и убедиться, что она видна в списке. Задача уже на доске (автодобавление подзадач) —
     * не ошибка.
     */
    fun add(
        issue: Int,
        databaseId: String,
        nodeId: String,
    ): BoardItem {
        val items = itemsPath()
        val addError = try {
            gitHub.write(args = listOf("api", "--method", "POST", items, "-f", "type=Issue", "-F", "id=$databaseId"))
            null
        } catch (error: TaskError.Failure) {
            error
        }
        val item = try {
            item(issue = issue, nodeId = nodeId)
        } catch (refusal: TaskError.Refusal) {
            throw addError ?: refusal
        }
        if (!isVisible(items = items, itemId = item.id)) {
            throw TaskError.Failure(message = "#$issue добавлена, но не видна в списке доски — проверьте доску вручную")
        }
        return item
    }

    private fun itemsPath(): String {
        val repository = gitHub.read(args = listOf("repo", "view", "--json", "owner,name", "--jq", REPO_JQ))
        val (owner, name) = repository.split("\t")
        val rows = gitHub.listGraphql(
            query = PROJECTS_QUERY,
            jq = PROJECTS_JQ,
            variables = mapOf("owner" to owner, "name" to name),
        )
        if (rows.size != 1) {
            val reason = if (rows.isEmpty()) "нет связанной доски" else "несколько связанных досок"
            throw TaskError.Refusal(message = "у репозитория $reason — свяжите с репозиторием одну доску")
        }
        val (ownerPath, number) = rows.first().split("\t")
        return "$ownerPath/projectsV2/$number/items"
    }

    private fun isVisible(
        items: String,
        itemId: String,
    ): Boolean {
        repeat(times = VISIBILITY_CHECKS) { attempt ->
            if (itemId in gitHub.listRest(path = items, jq = ".[].node_id")) {
                return true
            }
            if (attempt < VISIBILITY_CHECKS - 1) {
                pause(VISIBILITY_PAUSE_MS)
            }
        }
        return false
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

        private const val VISIBILITY_CHECKS = 3
        private const val VISIBILITY_PAUSE_MS = 2000L
        private const val REPO_JQ = ".owner.login + \"\\t\" + .name"
        private const val PROJECTS_QUERY = "query(\$owner:String!,\$name:String!,\$endCursor:String){" +
            "repository(owner:\$owner,name:\$name){projectsV2(first:20,after:\$endCursor){nodes{number " +
            "owner{__typename ... on User{login} ... on Organization{login}}} pageInfo{hasNextPage endCursor}}}}"
        private const val PROJECTS_JQ = ".data.repository.projectsV2.nodes[] | [(if .owner.__typename == " +
            "\"Organization\" then \"orgs\" else \"users\" end) + \"/\" + .owner.login, (.number | tostring)] | @tsv"
        private const val ITEMS_QUERY = "query(\$id:ID!,\$endCursor:String){node(id:\$id){... on Issue{" +
            "projectItems(first:50,after:\$endCursor){nodes{id project{id field(name:\"Status\"){" +
            "... on ProjectV2SingleSelectField{id options{id name}}}} fieldValueByName(name:\"Status\"){" +
            "... on ProjectV2ItemFieldSingleSelectValue{name}}} pageInfo{hasNextPage endCursor}}}}}"
        private const val ITEMS_JQ = ".data.node.projectItems.nodes[] | [.id, .project.id, .project.field.id, " +
            "(.fieldValueByName.name // \"\"), ([.project.field.options[] | .name + \"=\" + .id] | join(\";\"))] | @tsv"
    }
}
