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
        justRun { git.fetch() }
        justRun { git.createBranch(name = any(), base = any()) }
        justRun { git.switchTo(name = any()) }
        every { git.isClean() } returns true
        every { git.currentBranch() } returns "long/13-x"
        every { git.hasLocalBranch(name = "task/13-final") } returns false
        every { git.hasRemoteBranch(name = "task/13-final") } returns false
        every { git.merge(ref = "origin/long/13-x") } returns true
        every { git.longBranch(issue = 13) } returns "long/13-x"
        every { issues.nodeId(number = 13) } returns "I_issue13"
        every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()

        given("первый итоговый PR из long-ветки") {
            `when` {
                command.run(args = listOf("13") + makeDraftArgs(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        git.fetch()
                        git.hasLocalBranch(name = "task/13-final")
                        git.hasRemoteBranch(name = "task/13-final")
                        git.createBranch(name = "task/13-final", base = "origin/long/13-x")
                        publisher.publish(draft = makeFinalDraft(), console = console)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "In review")
                        console.line(text = "#13 → In review")
                    }
                }
            }
        }

        given("повтор из ветки final") {
            every { git.currentBranch() } returns "task/13-final"
            every { git.hasLocalBranch(name = "task/13-final") } returns true
            `when` {
                command.run(args = listOf("13") + makeDraftArgs(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.longBranch(issue = 13)
                        git.isClean()
                        git.fetch()
                        git.hasLocalBranch(name = "task/13-final")
                        git.merge(ref = "origin/long/13-x")
                        publisher.publish(draft = makeFinalDraft(), console = console)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "In review")
                        console.line(text = "#13 → In review")
                    }
                }
            }
        }

        given("повтор из long-ветки, final только в origin") {
            every { git.hasRemoteBranch(name = "task/13-final") } returns true
            `when` {
                command.run(args = listOf("13") + makeDraftArgs(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        git.fetch()
                        git.hasLocalBranch(name = "task/13-final")
                        git.hasRemoteBranch(name = "task/13-final")
                        git.switchTo(name = "task/13-final")
                        git.merge(ref = "origin/long/13-x")
                        publisher.publish(draft = makeFinalDraft(), console = console)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "In review")
                        console.line(text = "#13 → In review")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeFinalMergeMessage()) {
            every { git.hasLocalBranch(name = "task/13-final") } returns true
            every { git.merge(ref = "origin/long/13-x") } returns false
            command.run(args = listOf("13") + makeDraftArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("у задачи #13 нет long-ветки — итоговый PR не из чего собрать") {
            every { git.currentBranch() } returns "task/13-final"
            every { git.longBranch(issue = 13) } returns null
            command.run(args = listOf("13") + makeDraftArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeFinalDirtyMessage()) {
            every { git.isClean() } returns false
            command.run(args = listOf("13") + makeDraftArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeFinalWrongBranchMessage()) {
            every { git.currentBranch() } returns "task/13-x"
            command.run(args = listOf("13") + makeDraftArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeFinalUsageMessage()) {
            command.run(args = makeDraftArgs(), console = console)
        }
    })
