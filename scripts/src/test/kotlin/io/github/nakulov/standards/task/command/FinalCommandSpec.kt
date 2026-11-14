package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class FinalCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val publisher = mockk<Publisher>()
        val console = mockk<Console>()
        val command = FinalCommand(git = git, issues = issues, board = board, publisher = publisher)
        justRun { console.line(text = any()) }
        justRun { publisher.publish(draft = any(), console = console) }
        justRun { board.move(item = any(), status = any()) }
        every { issues.nodeId(number = 13) } returns "I_issue13"

        given("итоговый PR из long-ветки") {
            every { git.currentBranch() } returns "long/13-x"
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            `when` {
                command.run(args = listOf("13") + makeDraftArgs(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        publisher.publish(draft = makeFinalDraft(), console = console)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "In review")
                        console.line(text = "#13 → In review")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("task final 13 запускается из long/13-…, сейчас «task/13-x»") {
            every { git.currentBranch() } returns "task/13-x"
            command.run(args = listOf("13") + makeDraftArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeFinalUsageMessage()) {
            command.run(args = makeDraftArgs(), console = console)
        }
    })
