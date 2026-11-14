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
