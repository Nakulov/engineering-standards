package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.github.PullRequest

fun makeNotTaskBranchMessage(): String {
    return "dev — не ветка задачи: task done запускается из task/<N>-… или long/<N>-…"
}

fun makeDetachedMessage(): String {
    return "HEAD отсоединён — не ветка задачи: task done запускается из task/<N>-… или long/<N>-…"
}

fun makeOpenPullRequest(): PullRequest {
    return PullRequest(number = 31, merged = false)
}

fun makeLongRemovedMessage(): String {
    return "long-ветка long/13-big удалена — локально и в origin"
}

fun makeNoLongMessage(): String {
    return "long-ветки задачи #13 нет — удалена только ветка итогового PR"
}

fun makeLongOpenedMessage(): String {
    return "ветка long/13-big открыта в /repo — переключите там ветку и повторите task done"
}
