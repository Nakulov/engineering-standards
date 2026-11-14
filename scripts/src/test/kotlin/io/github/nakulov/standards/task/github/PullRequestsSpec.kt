package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence

class PullRequestsSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val pullRequests = PullRequests(gitHub = gitHub)

        shouldReturnTest(makePullUrl()) {
            every { gitHub.write(args = makeCreateDraftArgs()) } returns makeCreateOutput()
            pullRequests.create(base = "dev", head = "task/13-x", title = "Заголовок", body = "Тело", draft = true)
        }

        shouldReturnTest(makePullUrl()) {
            every { gitHub.write(args = makeCreateArgs()) } returns makePullUrl()
            pullRequests.create(base = "dev", head = "long/13-x", title = "Заголовок", body = "Тело", draft = false)
        }

        shouldReturnTest(makePullUrl()) {
            every { gitHub.write(args = makePrEditArgs()) } returns makePullUrl()
            pullRequests.edit(head = "task/13-x", title = "Заголовок", body = "Тело")
        }

        shouldReturnTest("Part of #13") {
            every { gitHub.read(args = makeLinkArgs()) } returns "Part of #13"
            pullRequests.link(head = "task/13-x")
        }

        given("из черновика") {
            every { gitHub.write(args = listOf("pr", "ready", "task/13-x")) } returns ""
            `when` {
                pullRequests.ready(head = "task/13-x")
                then {
                    verifySequence { gitHub.write(args = listOf("pr", "ready", "task/13-x")) }
                }
            }
        }
    })
