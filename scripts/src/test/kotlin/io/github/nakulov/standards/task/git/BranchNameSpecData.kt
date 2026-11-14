package io.github.nakulov.standards.task.git

fun makeBadSlugMessage(): String {
    return "slug «Скрипты» — латиница в нижнем регистре, цифры и дефисы, до 40 знаков: task-scripts"
}

fun makeLongSlug(): String {
    return "a-very-long-slug-that-does-not-fit-the-limit"
}

fun makeLongSlugMessage(): String {
    return "slug «a-very-long-slug-that-does-not-fit-the-limit» — латиница в нижнем регистре, цифры и дефисы, " +
        "до 40 знаков: task-scripts"
}

fun makeTaskBranch(): BranchName {
    return BranchName(issue = 13, slug = "task-scripts", long = false)
}

fun makeLongBranch(): BranchName {
    return BranchName(issue = 13, slug = "task-scripts", long = true)
}
