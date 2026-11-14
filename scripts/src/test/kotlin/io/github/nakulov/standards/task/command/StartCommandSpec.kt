package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.kotestkit.withGivens
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

class StartCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val console = mockk<Console>()
        val command = StartCommand(git = git, issues = issues, board = board)
        justRun { console.line(text = any()) }
        justRun { board.move(item = any(), status = any()) }
        justRun { git.fetch() }
        justRun { git.createBranch(name = any(), base = any()) }
        justRun { git.switchTo(name = any()) }
        every { git.isClean() } returns true
        every { issues.nodeId(number = 13) } returns "I_issue13"

        given("задача Groomed, ветки нет") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            `when` {
                command.run(args = listOf("13", "task-scripts"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.createBranch(name = "task/13-task-scripts", base = "origin/dev")
                        console.line(text = "ветка task/13-task-scripts от origin/dev")
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                    }
                }
            }
        }

        given("повторный запуск: long-ветка есть, задача уже In progress") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "long/13-task-scripts") } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--long"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.hasLocalBranch(name = "long/13-task-scripts")
                        git.switchTo(name = "long/13-task-scripts")
                        console.line(text = "ветка long/13-task-scripts уже есть — переключился на неё")
                        console.line(text = "#13 уже In progress")
                    }
                }
            }
        }

        given("исследование") {
            every { issues.info(number = 13) } returns makeResearchInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            `when` {
                command.run(args = listOf("13", "wiki-spike"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        console.line(text = "#13 — исследование: ветка не нужна")
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                    }
                }
            }
        }

        withGivens(
            nameFn = { given -> "отказ: ${given.arg1}" },
            case(arg1 = "#13 закрыта", arg2 = makeClosedInfo(), arg3 = makeGroomedItem()),
            case(arg1 = "#13 — эпик: в работу берутся его подзадачи", arg2 = makeEpicInfo(), arg3 = makeGroomedItem()),
            case(arg1 = "#13 в статусе «Backlog» — сначала груминг", arg2 = makeTaskInfo(), arg3 = makeBacklogItem()),
            case(arg1 = "#13 заблокирована: #3, #8 открыты", arg2 = makeBlockedInfo(), arg3 = makeGroomedItem()),
        ) { message, info, item ->
            every { issues.info(number = 13) } returns info
            every { board.item(issue = 13, nodeId = "I_issue13") } returns item
            shouldThrowWithMessageTest<TaskError.Refusal>(message) {
                command.run(args = listOf("13", "task-scripts"), console = console)
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeDirtyMessage()) {
            every { git.isClean() } returns false
            command.run(args = listOf("13", "task-scripts"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeUsageMessage()) {
            command.run(args = listOf("13"), console = console)
        }
    })
