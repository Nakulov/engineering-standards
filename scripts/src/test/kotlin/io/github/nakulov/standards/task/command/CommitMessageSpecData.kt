package io.github.nakulov.standards.task.command

fun makeCommitText(): String {
    return "Добавить task commit\n\nЗадача плана: Задача 3: команда task commit\nФайлы:\n" +
        "- `scripts/a.kt` — команда\n- `scripts/b.kt` — тест\n"
}

fun makeCommitMessage(): CommitMessage {
    return CommitMessage(
        title = "Добавить task commit",
        planTask = "Задача 3: команда task commit",
        files = listOf("scripts/a.kt", "scripts/b.kt"),
    )
}

fun makeNoTitleText(): String {
    return "\nЗадача плана: Задача 3\nФайлы:\n- `a.kt` — код\n"
}

fun makeNoPlanTaskText(): String {
    return "Добавить\n\nФайлы:\n- `a.kt` — код\n"
}

fun makeNoFilesText(): String {
    return "Добавить\n\nЗадача плана: Задача 3\nФайлы:\n"
}

fun makePlanPath(): String {
    return "plans/commit-commands.md"
}

fun makeNoPlanTaskCommitText(): String {
    return "Поправить опечатку\n\nФайлы:\n- `scripts/a.kt` — опечатка\n"
}

fun makeNoPlanTaskCommitMessage(): CommitMessage {
    return CommitMessage(title = "Поправить опечатку", planTask = null, files = listOf("scripts/a.kt"))
}
