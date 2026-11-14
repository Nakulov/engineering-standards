package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import java.io.File

/**
 * Доска репозитория (#8, решение 9). Нет доски — копия доски-шаблона из `templates/github/board.yml` и связь с
 * репозиторием: только копия переносит включённые workflow, API их не включает (опыт 2026-11-14). Есть доска —
 * недостающие статусы добавляются, текущие передаются со своими id (без id задачи теряют статус); лишние статусы и
 * выключенные workflow только называются: удаление сотрёт статус задачам, а workflow включает человек.
 */
class BoardSetup(
    private val gitHub: GitHub,
    private val template: Template,
    private val scratch: File,
) : Setup.Step {
    /** Доска-шаблон: владелец и номер. */
    data class Template(
        val owner: String,
        val number: Int,
    )

    override fun run(mode: Setup.Mode): List<Finding> {
        val repository = graphql(query = REPOSITORY_QUERY) + REPOSITORY_FIELDS + jq(filter = REPOSITORY_JQ)
        val (repositoryId, ownerId, name) = gitHub.read(args = repository).split("\t")
        val boards = gitHub.listGraphql(query = BOARDS_QUERY, jq = BOARDS_JQ, variables = REPOSITORY_VARIABLES)
        val templateFields = listOf("-f", "owner=${template.owner}", "-F", "number=${template.number}")
        val reference = gitHub.read(args = graphql(query = TEMPLATE_QUERY) + templateFields + jq(filter = TEMPLATE_JQ))
            .split("\t")
        return when (boards.size) {
            0 -> listOf(
                create(
                    mode = mode,
                    templateId = reference.first(),
                    ownerId = ownerId,
                    repositoryId = repositoryId,
                    name = name,
                ),
            )
            1 -> check(board = boards.first().split("\t"), reference = reference, mode = mode)
            else -> listOf(
                Finding(
                    status = Finding.Status.DEVIATION,
                    area = AREA,
                    text = "с репозиторием связано досок: ${boards.size} — task работает с одной, лишние отвяжите",
                ),
            )
        }
    }

    private fun create(
        mode: Setup.Mode,
        templateId: String,
        ownerId: String,
        repositoryId: String,
        name: String,
    ): Finding {
        if (!mode.apply) {
            return Finding(status = Finding.Status.DEVIATION, area = AREA, text = "нет доски, связанной с репозиторием")
        }
        val fields = listOf("-f", "template=$templateId", "-f", "owner=$ownerId", "-f", "title=$name")
        val copy = gitHub.write(args = graphql(query = COPY_MUTATION) + fields + jq(filter = COPY_JQ)).split("\t")
        val link = listOf("-f", "project=${copy.first()}", "-f", "repository=$repositoryId")
        gitHub.write(args = graphql(query = LINK_MUTATION) + link)
        val text = "создана доска «$name» (${copy.last()}) — копия шаблона, связана с репозиторием"
        return Finding(status = Finding.Status.FIXED, area = AREA, text = text)
    }

    /** [board]: id, url, id поля Status, статусы `id=имя;…`, включённые workflow `имя;…`. */
    private fun check(
        board: List<String>,
        reference: List<String>,
        mode: Setup.Mode,
    ): List<Finding> {
        val (_, url, fieldId, optionsText, workflowsText) = board
        if (fieldId.isEmpty()) {
            val text = "на доске $url нет поля Status"
            return listOf(Finding(status = Finding.Status.DEVIATION, area = AREA, text = text))
        }
        val names = split(text = optionsText).map { option -> option.substringAfter(delimiter = "=") }
        val missing = Board.STATUSES.filter { status -> status !in names }
        val extra = names.filter { status -> status !in Board.STATUSES }
        val disabled = split(text = reference.last()).filter { workflow -> workflow !in split(text = workflowsText) }
        val findings = mutableListOf<Finding>()
        if (missing.isNotEmpty()) {
            if (mode.apply) {
                addStatuses(fieldId = fieldId, missing = missing)
            }
            val status = if (mode.apply) Finding.Status.FIXED else Finding.Status.DEVIATION
            val text = "нет статусов: ${missing.joinToString(separator = ", ")}"
            findings += Finding(status = status, area = AREA, text = text)
        }
        if (extra.isNotEmpty()) {
            val names = extra.joinToString(separator = ", ")
            val text = "статусы вне стандарта: $names — не удаляются: на них могут стоять задачи"
            findings += Finding(status = Finding.Status.NOTE, area = AREA, text = text)
        }
        if (disabled.isNotEmpty()) {
            val text = "выключены workflow: ${disabled.joinToString(separator = ", ")} — включите: $url/workflows"
            findings += Finding(status = Finding.Status.DEVIATION, area = AREA, text = text)
        }
        if (findings.none { finding -> finding.status != Finding.Status.NOTE }) {
            findings.add(index = 0, element = Finding(status = Finding.Status.OK, area = AREA, text = "доска $url"))
        }
        return findings
    }

    /** Все текущие статусы — со своими id, иначе GitHub выдаст им новые и задачи потеряют статус. */
    private fun addStatuses(
        fieldId: String,
        missing: List<String>,
    ) {
        val options = graphql(query = OPTIONS_QUERY) + listOf("-f", "field=$fieldId") + jq(filter = OPTIONS_JQ)
        val current = gitHub.read(args = options).lines().filter { line -> line.isNotBlank() }.associate { line ->
            val (id, name, color, description) = "$line\t\t\t".split("\t")
            name to "{\"id\":${quote(text = id)},\"name\":${quote(text = name)},\"color\":${quote(text = color)}," +
                "\"description\":${quote(text = unescape(text = description))}}"
        }
        val added = missing.associateWith { name ->
            "{\"name\":${quote(text = name)},\"color\":\"GRAY\",\"description\":\"\"}"
        }
        // Порядок статусов — порядок колонок доски: сначала стандарт по порядку, потом статусы проекта.
        val ordered = Board.STATUSES.mapNotNull { name -> current[name] ?: added[name] } +
            current.filterKeys { name -> name !in Board.STATUSES }.values
        val body = "{\"query\":${quote(text = UPDATE_MUTATION)},\"variables\":{\"field\":${quote(text = fieldId)}," +
            "\"options\":[${ordered.joinToString(separator = ",")}]}}"
        scratch.mkdirs()
        val file = File(scratch, "board-statuses.json")
        file.writeText(body)
        try {
            gitHub.write(args = listOf("api", "graphql", "--input", file.path))
        } finally {
            file.delete()
        }
    }

    private fun graphql(query: String): List<String> {
        return listOf("api", "graphql", "-f", "query=$query")
    }

    private fun jq(filter: String): List<String> {
        return listOf("--jq", filter)
    }

    private fun split(text: String): List<String> {
        return text.split(";").filter { part -> part.isNotEmpty() }
    }

    /** `@tsv` экранирует табуляцию, перевод строки и `\`; описание статуса возвращается как было. */
    private fun unescape(text: String): String {
        return text.replace(oldValue = "\\t", newValue = "\t").replace(oldValue = "\\n", newValue = "\n")
            .replace(oldValue = "\\\\", newValue = "\\")
    }

    private fun quote(text: String): String {
        val escaped = text.replace(oldValue = "\\", newValue = "\\\\").replace(oldValue = "\"", newValue = "\\\"")
            .replace(oldValue = "\n", newValue = "\\n").replace(oldValue = "\t", newValue = "\\t")
        return "\"$escaped\""
    }

    companion object {
        private const val AREA = "доска"
        private val REPOSITORY_FIELDS = listOf("-F", "owner={owner}", "-F", "name={repo}")
        private val REPOSITORY_VARIABLES = mapOf("owner" to "{owner}", "name" to "{repo}")
        private const val REPOSITORY_QUERY = "query(\$owner:String!,\$name:String!){" +
            "repository(owner:\$owner,name:\$name){id name owner{id}}}"
        private const val REPOSITORY_JQ = ".data.repository | [.id, .owner.id, .name] | @tsv"
        private const val BOARDS_QUERY = "query(\$owner:String!,\$name:String!,\$endCursor:String){" +
            "repository(owner:\$owner,name:\$name){projectsV2(first:20,after:\$endCursor){nodes{id url " +
            "field(name:\"Status\"){... on ProjectV2SingleSelectField{id options{id name}}} " +
            "workflows(first:50){nodes{name enabled}}} pageInfo{hasNextPage endCursor}}}}"
        private const val BOARDS_JQ = ".data.repository.projectsV2.nodes[] | [.id, .url, (.field.id // \"\"), " +
            "([.field.options[]? | .id + \"=\" + .name] | join(\";\")), " +
            "([.workflows.nodes[] | select(.enabled) | .name] | join(\";\"))] | @tsv"
        private const val TEMPLATE_QUERY = "query(\$owner:String!,\$number:Int!){repositoryOwner(login:\$owner){" +
            "... on ProjectV2Owner{projectV2(number:\$number){id workflows(first:50){nodes{name enabled}}}}}}"
        private const val TEMPLATE_JQ = ".data.repositoryOwner.projectV2 | " +
            "[.id, ([.workflows.nodes[] | select(.enabled) | .name] | join(\";\"))] | @tsv"
        private const val COPY_MUTATION = "mutation(\$template:ID!,\$owner:ID!,\$title:String!){copyProjectV2(input:{" +
            "projectId:\$template,ownerId:\$owner,title:\$title,includeDraftIssues:false}){projectV2{id url}}}"
        private const val COPY_JQ = ".data.copyProjectV2.projectV2 | [.id, .url] | @tsv"
        private const val LINK_MUTATION = "mutation(\$project:ID!,\$repository:ID!){linkProjectV2ToRepository(input:{" +
            "projectId:\$project,repositoryId:\$repository}){repository{id}}}"
        private const val OPTIONS_QUERY = "query(\$field:ID!){node(id:\$field){... on ProjectV2SingleSelectField{" +
            "options{id name color description}}}}"
        private const val OPTIONS_JQ = ".data.node.options[] | [.id, .name, .color, .description] | @tsv"
        private const val UPDATE_MUTATION = "mutation(\$field:ID!," +
            "\$options:[ProjectV2SingleSelectFieldOptionInput!]){updateProjectV2Field(input:{fieldId:\$field," +
            "singleSelectOptions:\$options}){projectV2Field{" +
            "... on ProjectV2SingleSelectField{id}}}}"

        /** Доска-шаблон из `board.yml`: строки `owner: …` и `number: …`; `#` — комментарий. */
        fun parse(
            text: String,
            file: String,
        ): Template {
            val values = text.lines().filter { line -> line.isNotBlank() && !line.startsWith(prefix = "#") }
                .associate { line ->
                    line.substringBefore(delimiter = ":").trim() to line.substringAfter(delimiter = ":").trim()
                }
            val owner = values["owner"]
            val number = values["number"]?.toIntOrNull()
            if (owner.isNullOrEmpty() || number == null) {
                throw TaskError.Failure(message = "$file: нужны строки «owner: <владелец>» и «number: <номер доски>»")
            }
            return Template(owner = owner, number = number)
        }
    }
}
