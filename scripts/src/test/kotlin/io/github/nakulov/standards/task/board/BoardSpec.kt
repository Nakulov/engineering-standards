package io.github.nakulov.standards.task.board

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class BoardSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val pause = mockk<(Long) -> Unit>()
        justRun { pause.invoke(any()) }
        val board = Board(gitHub = gitHub, pause = pause)
        every { gitHub.read(args = makeRepoArgs()) } returns "owner\trepo"
        every {
            gitHub.listGraphql(query = any(), jq = any(), variables = makeRepoVariables())
        } returns listOf("users/owner\t10")

        shouldReturnTest(makeItem()) {
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns makeItemRows()
            board.item(issue = 13, nodeId = "I_issue13")
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#13 не на доске — добавьте её на доску проекта") {
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns emptyList()
            board.item(issue = 13, nodeId = "I_issue13")
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#13 на нескольких досках") {
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns makeTwoRows()
            board.item(issue = 13, nodeId = "I_issue13")
        }

        given("смена статуса по имени") {
            every { gitHub.write(args = makeMoveArgs()) } returns ""
            `when` {
                board.move(item = makeItem(), status = Board.IN_PROGRESS)
                then {
                    verifySequence { gitHub.write(args = makeMoveArgs()) }
                }
            }
        }

        given("добавление: видна с первой проверки") {
            every { gitHub.write(args = makeAddArgs()) } returns ""
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns makeItemRows()
            every { gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id") } returns listOf("PVTI_1", "PVTI_13")
            `when` {
                val item = board.add(issue = 13, databaseId = "9013", nodeId = "I_issue13")
                then {
                    item shouldBe makeItem()
                    verifySequence {
                        gitHub.read(args = makeRepoArgs())
                        gitHub.listGraphql(query = any(), jq = any(), variables = makeRepoVariables())
                        gitHub.write(args = makeAddArgs())
                        gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables())
                        gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id")
                    }
                }
            }
        }

        given("добавление: задача уже на доске, видна с третьей проверки") {
            every { gitHub.write(args = makeAddArgs()) } throws TaskError.Failure(message = "gh: HTTP 422")
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns makeItemRows()
            every {
                gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id")
            } returnsMany listOf(listOf("PVTI_1"), listOf("PVTI_1"), listOf("PVTI_1", "PVTI_13"))
            `when` {
                val item = board.add(issue = 13, databaseId = "9013", nodeId = "I_issue13")
                then {
                    item shouldBe makeItem()
                    verifySequence {
                        gitHub.read(args = makeRepoArgs())
                        gitHub.listGraphql(query = any(), jq = any(), variables = makeRepoVariables())
                        gitHub.write(args = makeAddArgs())
                        gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables())
                        gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id")
                        pause.invoke(2000L)
                        gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id")
                        pause.invoke(2000L)
                        gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeInvisibleMessage()) {
            every { gitHub.write(args = makeAddArgs()) } returns ""
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns makeItemRows()
            every { gitHub.listRest(path = makeItemsPath(), jq = ".[].node_id") } returns listOf("PVTI_1")
            board.add(issue = 13, databaseId = "9013", nodeId = "I_issue13")
        }

        shouldThrowWithMessageTest<TaskError.Failure>("gh: HTTP 403") {
            every { gitHub.write(args = makeAddArgs()) } throws TaskError.Failure(message = "gh: HTTP 403")
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeVariables()) } returns emptyList()
            board.add(issue = 13, databaseId = "9013", nodeId = "I_issue13")
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNoBoardMessage()) {
            every { gitHub.listGraphql(query = any(), jq = any(), variables = makeRepoVariables()) } returns emptyList()
            board.add(issue = 13, databaseId = "9013", nodeId = "I_issue13")
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeMissingStatusMessage()) {
            board.move(item = makeItem(), status = "Testing")
        }
    })
