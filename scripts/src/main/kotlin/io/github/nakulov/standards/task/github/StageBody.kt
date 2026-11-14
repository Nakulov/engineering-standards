package io.github.nakulov.standards.task.github

/**
 * Описание этапа по шаблону этапа (`templates/github/stage.md`): веха создаётся только с заполненными разделами
 * (#2, «`ROADMAP.md`», пункт 7). Пустой раздел пишется «Нет»; комментарии `<!-- … -->` за содержание не считаются.
 */
object StageBody {
    private const val HEADING = "## "
    private val COMMENT = Regex(pattern = "<!--.*?-->", option = RegexOption.DOT_MATCHES_ALL)

    /** Разделы шаблона, которых в тексте нет или они пусты, — в порядке шаблона. */
    fun emptySections(
        template: String,
        text: String,
    ): List<String> {
        val filled = sections(text = text).filterValues { content -> content.isNotBlank() }.keys
        return sections(text = template).keys.filter { section -> section !in filled }
    }

    private fun sections(text: String): Map<String, String> {
        val parts = COMMENT.replace(input = text, replacement = "").split(Regex(pattern = "(?m)^(?=$HEADING)"))
        return parts.filter { part -> part.startsWith(prefix = HEADING) }.associate { part ->
            val name = part.lineSequence().first().removePrefix(prefix = HEADING).trim()
            name to part.substringAfter(delimiter = "\n", missingDelimiterValue = "")
        }
    }
}
