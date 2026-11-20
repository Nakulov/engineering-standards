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
        justRun { git.removeWorktree(path = any()) }
        justRun { git.deleteRemoteBranch(name = any()) }
        every { git.root() } returns "/repo/.claude/worktrees/task-scripts"
        every { git.copyPath(slug = "task-scripts") } returns "/repo/.claude/worktrees/task-scripts"

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
                        git.root()
                        git.copyPath(slug = "task-scripts")
                        git.removeWorktree(path = "/repo/.claude/worktrees/task-scripts")
                        console.line(
                            text = "копия /repo/.claude/worktrees/task-scripts удалена — текущей папки больше нет",
                        )
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

        given("ветка блока task/13-big-pr1 слита в копии long-ветки .claude/worktrees/big") {
            every { git.currentBranch() } returns "task/13-big-pr1"
            every { issues.pullRequest(branch = "task/13-big-pr1") } returns PullRequest(number = 40, merged = true)
            every { git.isWorktree() } returns true
            every { git.root() } returns "/repo/.claude/worktrees/big"
            every { git.copyPath(slug = "big-pr1") } returns "/repo/.claude/worktrees/big-pr1"
            every { issues.info(number = 13) } returns makeTaskInfo()
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-big-pr1")
                        git.fetch()
                        git.isWorktree()
                        git.detach(ref = "origin/dev")
                        console.line(text = "worktree отсоединён на origin/dev, dev не тронут")
                        git.deleteBranch(name = "task/13-big-pr1")
                        console.line(text = "ветка task/13-big-pr1 удалена")
                        issues.info(number = 13)
                        console.line(text = "#13 открыта — не последний PR задачи, статус остаётся")
                        git.root()
                        git.copyPath(slug = "big-pr1")
                    }
                }
            }
        }

        given("итоговый PR из task/13-final слит в копии long-ветки .claude/worktrees/big") {
            every { git.currentBranch() } returns "task/13-final"
            every { issues.pullRequest(branch = "task/13-final") } returns PullRequest(number = 50, merged = true)
            every { git.isWorktree() } returns true
            every { git.longBranch(issue = 13) } returns "long/13-big"
            every { git.openedAt(branch = "long/13-big") } returns null
            every { git.hasLocalBranch(name = "long/13-big") } returns true
            every { git.hasRemoteBranch(name = "long/13-big") } returns true
            every { issues.info(number = 13) } returns makeClosedInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.root() } returns "/repo/.claude/worktrees/big"
            every { git.copyPath(slug = "big") } returns "/repo/.claude/worktrees/big"
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-final")
                        git.longBranch(issue = 13)
                        git.openedAt(branch = "long/13-big")
                        git.fetch()
                        git.isWorktree()
                        git.detach(ref = "origin/dev")
                        console.line(text = "worktree отсоединён на origin/dev, dev не тронут")
                        git.deleteBranch(name = "task/13-final")
                        console.line(text = "ветка task/13-final удалена")
                        git.hasLocalBranch(name = "long/13-big")
                        git.deleteBranch(name = "long/13-big")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.deleteRemoteBranch(name = "long/13-big")
                        console.line(text = makeLongRemovedMessage())
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "Done")
                        console.line(text = "#13 → Done")
                        git.root()
                        git.copyPath(slug = "big")
                        git.removeWorktree(path = "/repo/.claude/worktrees/big")
                        console.line(text = "копия /repo/.claude/worktrees/big удалена — текущей папки больше нет")
                    }
                }
            }
        }

        given("итоговый PR из task/13-final слит, long-ветки уже нет") {
            every { git.currentBranch() } returns "task/13-final"
            every { issues.pullRequest(branch = "task/13-final") } returns PullRequest(number = 50, merged = true)
            every { git.isWorktree() } returns false
            every { git.longBranch(issue = 13) } returns null
            every { issues.info(number = 13) } returns makeClosedInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeDoneItem()
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-final")
                        git.longBranch(issue = 13)
                        git.fetch()
                        git.isWorktree()
                        git.updateDev()
                        console.line(text = "dev обновлён до origin/dev")
                        git.deleteBranch(name = "task/13-final")
                        console.line(text = "ветка task/13-final удалена")
                        console.line(text = makeNoLongMessage())
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        console.line(text = "#13 → Done")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeLongOpenedMessage()) {
            every { git.currentBranch() } returns "task/13-final"
            every { issues.pullRequest(branch = "task/13-final") } returns PullRequest(number = 50, merged = true)
            every { git.longBranch(issue = 13) } returns "long/13-big"
            every { git.openedAt(branch = "long/13-big") } returns "/repo"
            command.run(args = emptyList(), console = console)
        }
    })
