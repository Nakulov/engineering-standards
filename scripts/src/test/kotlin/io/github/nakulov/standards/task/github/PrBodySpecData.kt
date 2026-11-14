package io.github.nakulov.standards.task.github

import java.io.File

/** Текст образца из `src/test/resources`. */
fun makeResource(path: String): String {
    return checkNotNull(Thread.currentThread().contextClassLoader.getResource(path)) { path }.readText()
}

fun makeNoChecksBody(): String {
    return "<!-- Заголовок — что стало. -->\nCloses #13\n<!-- Задача в одном PR — Closes #N. -->\n\n" +
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

fun makeNoAutomaticMessage(): String {
    return "в тексте PR нет строки «**Автоматически:**» — пишите тело по шаблону PR"
}

fun makeTemplateChecks(): List<String> {
    return listOf("**Автоматически:**", "- `./gradlew test` — прошла")
}
