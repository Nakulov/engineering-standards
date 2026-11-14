package io.github.nakulov.standards.task.board

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifySequence

class BoardSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val board = Board(gitHub = gitHub)

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

        shouldThrowWithMessageTest<TaskError.Failure>(makeMissingStatusMessage()) {
            board.move(item = makeItem(), status = "Testing")
        }
    })
