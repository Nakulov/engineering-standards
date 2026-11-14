package io.github.nakulov.standards.task.github

import java.io.File

/** Текст образца из `src/test/resources`. */
fun makeResource(path: String): String {
    return checkNotNull(Thread.currentThread().contextClassLoader.getResource(path)) { path }.readText()
}

fun makeNoChecksBody(): String {
    return "Closes #13\n\n" +
        "## Что меняет PR\n\nКоманда `task pr`.\n\n## Проверки\n**Автоматически:** нет\n" +
        "**Вручную:**\n- [ ] Черновик открыт.\n"
}

fun makeNoLinkBody(): String {
    return "Closes #13\n## Что меняет PR\n\nБез заготовки связи.\n\n## Проверки\n**Автоматически:** нет\n" +
        "**Вручную:**\n- [ ] Нет.\n"
}

/** Строки связи с задачей в теле, собранном из настоящего шаблона PR (`templates/github/`). */
fun readTemplateLinks(): List<String> {
    val template = File(System.getProperty("task.prTemplate")).readText()
    val body = PrBody.compose(text = template, link = "Closes #13", checks = listOf("./gradlew test"))
    return body.lines().filter { line -> line.startsWith(prefix = "Closes #") || line.startsWith(prefix = "Part of #") }
}

/** Строки проверок в теле, собранном из настоящего шаблона PR. */
fun readTemplateChecks(): List<String> {
    val template = File(System.getProperty("task.prTemplate")).readText()
    val body = PrBody.compose(text = template, link = "Closes #13", checks = listOf("./gradlew test"))
    return body.lines().dropWhile { line -> !line.startsWith(prefix = "**Автоматически:**") }.take(n = 2)
}

/** Первая строка тела из настоящего шаблона PR и строки с `<!--`. */
fun readTemplateHead(): List<String> {
    val template = File(System.getProperty("task.prTemplate")).readText()
    val body = PrBody.compose(text = template, link = "Closes #13", checks = listOf("./gradlew test")).lines()
    return listOf(body.first()) + body.filter { line -> "<!--" in line }
}

fun makeTemplateHead(): List<String> {
    return listOf("Closes #13")
}

/**
 * Комментарии, которые не вырезаются: в строке текста, в `коде` и в блоке ```; многострочный комментарий на своих
 * строках — вырезается.
 */
fun makeQuotedCommentsText(): String {
    return "Closes #\n<!-- подсказка\nна две строки -->\n\n## Что меняет PR\n\nЗаготовка `<!-- - что — почему -->` " +
        "в шаблоне. <!-- не вся строка -->\n\n```\n<!-- пример из шаблона -->\n```\n\n## Проверки\n" +
        "**Автоматически:** <!-- заполняет task pr -->\n**Вручную:**\n- [ ] Нет.\n"
}

fun makeQuotedCommentsBody(): String {
    return "Closes #41\n\n## Что меняет PR\n\nЗаготовка `<!-- - что — почему -->` в шаблоне. " +
        "<!-- не вся строка -->\n\n```\n<!-- пример из шаблона -->\n```\n\n" +
        "## Проверки\n**Автоматически:** нет\n**Вручную:**\n- [ ] Нет.\n"
}

fun makeNoAutomaticMessage(): String {
    return "в тексте PR нет строки «**Автоматически:**» — пишите тело по шаблону PR"
}

fun makeTemplateChecks(): List<String> {
    return listOf("**Автоматически:**", "- `./gradlew test` — прошла")
}
