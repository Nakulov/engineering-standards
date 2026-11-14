package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class EnvironmentSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val environment = Environment(gitHub = gitHub)
        every { gitHub.read(args = makeAdminArgs()) } returns "true"

        shouldReturnTest(Unit) {
            every { gitHub.read(args = makeUserArgs()) } returns makeHeaders(scopes = "gist, project, read:org, repo")
            environment.verify()
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNoProjectMessage()) {
            every { gitHub.read(args = makeUserArgs()) } returns makeHeaders(scopes = "gist, read:org, repo")
            environment.verify()
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNoScopesMessage()) {
            every { gitHub.read(args = makeUserArgs()) } returns "HTTP/2.0 200 OK\n\n{}"
            environment.verify()
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNotAdminMessage()) {
            every { gitHub.read(args = makeUserArgs()) } returns makeHeaders(scopes = "project, repo")
            every { gitHub.read(args = makeAdminArgs()) } returns "false"
            environment.verify()
        }
    })
