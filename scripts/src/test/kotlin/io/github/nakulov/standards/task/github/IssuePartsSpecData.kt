package io.github.nakulov.standards.task.github

const val PLAN_LINE = "План: ветка `long/53-commit-commands`, `plans/commit-commands.md`"

fun makeIssueBody(): String {
    return "<!-- подсказка шаблона -->\n\n## Проблема\nТекст.\n\n## Результаты обсуждения\n\n" +
        "**Отклонено:**\n- груминг — причина\n\n## Критерии готовности\n**Автоматически:**\n- [ ] старое\n" +
        "**Вручную:**\n- [ ] руками\n"
}

fun makeParts(): String {
    return "## Отклонено\n- план — причина\n\n## Автоматически\n- [ ] `./gradlew :scripts:test`\n"
}

fun makeMergedBody(): String {
    return "<!-- подсказка шаблона -->\n\n<!-- план -->\n$PLAN_LINE\n<!-- /план -->\n\n## Проблема\nТекст.\n\n" +
        "## Результаты обсуждения\n\n**Отклонено:**\n- груминг — причина\n\n<!-- план -->\n- план — причина\n" +
        "<!-- /план -->\n\n## Критерии готовности\n**Автоматически:**\n- [ ] старое\n\n<!-- план -->\n" +
        "- [ ] `./gradlew :scripts:test`\n<!-- /план -->\n**Вручную:**\n- [ ] руками\n"
}

fun makeNewParts(): String {
    return "## Отклонено\n- другой — причина\n\n## Автоматически\n- [ ] `./gradlew :scripts:test`\n"
}

/** Повтор: в «Отклонено» строка плана заменена, строка груминга на месте. */
fun makeRemergedBody(): String {
    return makeMergedBody().replace(oldValue = "- план — причина", newValue = "- другой — причина")
}

fun makeBrokenBody(): String {
    return makeMergedBody().replaceFirst(oldValue = "<!-- /план -->\n\n## Критерии", newValue = "\n## Критерии")
}
