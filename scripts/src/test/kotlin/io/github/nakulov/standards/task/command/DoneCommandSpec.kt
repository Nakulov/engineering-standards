package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues
import io.github.nakulov.standards.task.github.PullRequest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class DoneCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val console = mockk<Console>()
        val command = DoneCommand(git = git, issues = issues, board = board)
        justRun { console.line(text = any()) }
        justRun { board.move(item = any(), status = any()) }
        justRun { git.fetch() }
        justRun { git.updateDev() }
        justRun { git.detach(ref = any()) }
        justRun { git.deleteBranch(name = any()) }
        every { git.currentBranch() } returns "task/13-task-scripts"
        every { git.isClean() } returns true
        every { issues.pullRequest(branch = "task/13-task-scripts") } returns PullRequest(number = 30, merged = true)
        every { issues.nodeId(number = 13) } returns "I_issue13"

        given("последний PR слит в основном клоне") {
            every { git.isWorktree() } returns false
            every { issues.info(number = 13) } returns makeClosedInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-task-scripts")
                        git.fetch()
                        git.isWorktree()
                        git.updateDev()
                        console.line(text = "dev обновлён до origin/dev")
                        git.deleteBranch(name = "task/13-task-scripts")
                        console.line(text = "ветка task/13-task-scripts удалена")
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "Done")
                        console.line(text = "#13 → Done")
                    }
                }
            }
        }

        given("PR с Part of слит в worktree") {
            every { git.isWorktree() } returns true
            every { issues.info(number = 13) } returns makeTaskInfo()
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-task-scripts")
                        git.fetch()
                        git.isWorktree()
                        git.detach(ref = "origin/dev")
                        console.line(text = "worktree отсоединён на origin/dev, dev не тронут")
                        git.deleteBranch(name = "task/13-task-scripts")
                        console.line(text = "ветка task/13-task-scripts удалена")
                        issues.info(number = 13)
                        console.line(text = "#13 открыта — не последний PR задачи, статус остаётся")
                    }
                }
            }
        }

        given("задачу уже закрыли в Done") {
            every { git.isWorktree() } returns false
            every { issues.info(number = 13) } returns makeClosedInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeDoneItem()
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-task-scripts")
                        git.fetch()
                        git.isWorktree()
                        git.updateDev()
                        console.line(text = "dev обновлён до origin/dev")
                        git.deleteBranch(name = "task/13-task-scripts")
                        console.line(text = "ветка task/13-task-scripts удалена")
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        console.line(text = "#13 → Done")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNotTaskBranchMessage()) {
            every { git.currentBranch() } returns "dev"
            command.run(args = emptyList(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeDetachedMessage()) {
            every { git.currentBranch() } returns ""
            command.run(args = emptyList(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("PR #31 не слит — сначала слияние") {
            every { issues.pullRequest(branch = "task/13-task-scripts") } returns makeOpenPullRequest()
            command.run(args = emptyList(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("у ветки task/13-task-scripts нет PR — сначала task pr") {
            every { issues.pullRequest(branch = "task/13-task-scripts") } returns null
            command.run(args = emptyList(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeDirtyMessage()) {
            every { git.isClean() } returns false
            command.run(args = emptyList(), console = console)
        }
    })
