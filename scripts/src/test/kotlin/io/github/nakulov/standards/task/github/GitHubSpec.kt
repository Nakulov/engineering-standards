package io.github.nakulov.standards.task.github

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class GitHubSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val shell = mockk<Shell>()
        val pause = mockk<(Long) -> Unit>()
        justRun { pause.invoke(any()) }
        val gitHub = GitHub(shell = shell, pause = pause)

        given("чтение без сбоя") {
            every { shell.run(command = makeViewCommand()) } returns makeTitleResult()
            `when` {
                val out = gitHub.read(args = makeViewArgs())
                then {
                    out shouldBe makeTitle()
                    verifySequence { shell.run(command = makeViewCommand()) }
                }
            }
        }

        given("чтение: 502, повтор успешен") {
            every { shell.run(command = makeViewCommand()) } returnsMany listOf(makeBadGateway(), makeTitleResult())
            `when` {
                val out = gitHub.read(args = makeViewArgs())
                then {
                    out shouldBe makeTitle()
                    verifySequence {
                        shell.run(command = makeViewCommand())
                        pause.invoke(1000L)
                        shell.run(command = makeViewCommand())
                    }
                }
            }
        }

        given("чтение: 502 дважды") {
            every { shell.run(command = makeViewCommand()) } returns makeBadGateway()
            `when` {
                val error = shouldThrow<TaskError.Failure> { gitHub.read(args = makeViewArgs()) }
                then {
                    error.message shouldBe makeBadGatewayMessage()
                    verifySequence {
                        shell.run(command = makeViewCommand())
                        pause.invoke(1000L)
                        shell.run(command = makeViewCommand())
                    }
                }
            }
        }

        given("чтение: 404 не повторяется") {
            every { shell.run(command = makeViewCommand()) } returns makeNotFound()
            `when` {
                val error = shouldThrow<TaskError.Failure> { gitHub.read(args = makeViewArgs()) }
                then {
                    error.message shouldBe makeNotFoundMessage()
                    verifySequence { shell.run(command = makeViewCommand()) }
                }
            }
        }

        given("запись: 502 не повторяется") {
            every { shell.run(command = makeEditCommand()) } returns makeBadGateway()
            `when` {
                val error = shouldThrow<TaskError.Failure> { gitHub.write(args = makeEditArgs()) }
                then {
                    error.message shouldBe makeBadGatewayMessage()
                    verifySequence { shell.run(command = makeEditCommand()) }
                }
            }
        }

        given("REST-список") {
            every { shell.run(command = makeRestListCommand()) } returns makeIdsResult()
            `when` {
                val ids = gitHub.listRest(path = "repos/owner/repo/issues?state=open", jq = ".[].number")
                then {
                    ids shouldBe makeIds()
                    verifySequence { shell.run(command = makeRestListCommand()) }
                }
            }
        }

        given("GraphQL-список") {
            every { shell.run(command = makeGraphqlListCommand()) } returns makeIdsResult()
            `when` {
                val ids = gitHub.listGraphql(
                    query = makePagedQuery(),
                    jq = ".data.user.projectV2.items.nodes[].id",
                    variables = mapOf("n" to "10"),
                )
                then {
                    ids shouldBe makeIds()
                    verifySequence { shell.run(command = makeGraphqlListCommand()) }
                }
            }
        }

        shouldThrowWithMessageTest<IllegalArgumentException>(makeUnpagedQueryMessage()) {
            gitHub.listGraphql(query = makeUnpagedQuery(), jq = ".", variables = emptyMap())
        }

        shouldReturnTest(emptyList<String>()) {
            every { shell.run(command = makeRestListCommand()) } returns makeEmptyResult()
            gitHub.listRest(path = "repos/owner/repo/issues?state=open", jq = ".[].number")
        }
    })
