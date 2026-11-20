package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.Issues
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence

class GroomedCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val console = mockk<Console>()
        val command = GroomedCommand(issues = issues, board = board)
        justRun { console.line(text = any()) }
        justRun { board.move(item = any(), status = any()) }
        every { issues.nodeId(number = 13) } returns "I_issue13"
        every { board.item(issue = 13, nodeId = "I_issue13") } returns makeBacklogItem()

        given("открытая задача в Backlog") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            `when` {
                command.run(args = listOf("13"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeBacklogItem(), status = Board.GROOMED)
                        console.line(text = "#13 → Groomed")
                    }
                }
            }
        }

        withGivens(
            nameFn = { given -> "отказ: ${given.arg1}" },
            case(arg1 = "#13 закрыта", arg2 = makeClosedInfo()),
            case(arg1 = "#13 — эпик: в Groomed переводятся его подзадачи", arg2 = makeEpicInfo()),
        ) { message, info ->
            every { issues.info(number = 13) } returns info
            `when` {
                shouldThrowWithMessage<TaskError.Refusal>(message) {
                    command.run(args = listOf("13"), console = console)
                }
                then {
                    verify(exactly = 0) { board.item(issue = any(), nodeId = any()) }
                }
            }
        }

        withGivens(
            nameFn = { given -> "неверные аргументы ${given.arg1}" },
            case(arg1 = emptyList<String>()),
            case(arg1 = listOf("x")),
            case(arg1 = listOf("13", "14")),
        ) { args ->
            shouldThrowWithMessageTest<TaskError.Failure>("использование: task groomed ${command.usage}") {
                command.run(args = args, console = console)
            }
        }
    })
