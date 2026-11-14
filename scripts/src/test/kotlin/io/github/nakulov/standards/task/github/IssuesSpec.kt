package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class IssuesSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val issues = Issues(gitHub = gitHub)

        shouldReturnTest(makeOpenTaskInfo()) {
            every { gitHub.read(args = makeStateArgs()) } returns "OPEN\tprocess,type:task"
            every { gitHub.listRest(path = makeBlockersPath(), jq = makeBlockersJq()) } returns listOf("3", "8")
            issues.info(number = 13)
        }

        shouldReturnTest(makeClosedUntypedInfo()) {
            every { gitHub.read(args = makeStateArgs()) } returns "CLOSED\t"
            every { gitHub.listRest(path = makeBlockersPath(), jq = makeBlockersJq()) } returns emptyList()
            issues.info(number = 13)
        }

        shouldReturnTest("I_kwDOissue13") {
            every { gitHub.read(args = makeNodeIdArgs()) } returns "I_kwDOissue13"
            issues.nodeId(number = 13)
        }

        shouldReturnTest(PullRequest(number = 30, merged = true)) {
            every { gitHub.read(args = makePullRequestArgs()) } returns "30\tMERGED"
            issues.pullRequest(branch = "task/13-x")
        }

        shouldReturnTest(PullRequest(number = 31, merged = false)) {
            every { gitHub.read(args = makePullRequestArgs()) } returns "31\tOPEN"
            issues.pullRequest(branch = "task/13-x")
        }

        shouldReturnTest(null) {
            every { gitHub.read(args = makePullRequestArgs()) } throws makeNoPullRequestFailure()
            issues.pullRequest(branch = "task/13-x")
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeBadGatewayMessage()) {
            every { gitHub.read(args = makePullRequestArgs()) } throws makeBadGatewayFailure()
            issues.pullRequest(branch = "task/13-x")
        }
    })
