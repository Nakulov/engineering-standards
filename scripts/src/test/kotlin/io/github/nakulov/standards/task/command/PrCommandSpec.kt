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
import io.github.nakulov.standards.task.github.PullRequests
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class PrCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val pullRequests = mockk<PullRequests>()
        val publisher = mockk<Publisher>()
        val console = mockk<Console>()
        val command = PrCommand(
            git = git,
            issues = issues,
            board = board,
            pullRequests = pullRequests,
            publisher = publisher,
        )
        justRun { console.line(text = any()) }
        justRun { publisher.publish(draft = any(), console = console) }
        justRun { pullRequests.ready(head = any()) }
        justRun { board.move(item = any(), status = any()) }
        every { git.currentBranch() } returns "task/13-x"
        every { issues.nodeId(number = 13) } returns "I_issue13"

        given("PR в dev") {
            every { git.base(branch = "task/13-x") } returns null
            `when` {
                command.run(args = makeDraftArgs(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.base(branch = "task/13-x")
                        publisher.publish(draft = makeParsedDraft(), console = console)
                    }
                }
            }
        }

        given("PR в dev с --part") {
            every { git.base(branch = "task/13-x") } returns null
            `when` {
                command.run(args = makeDraftArgs() + "--part", console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.base(branch = "task/13-x")
                        publisher.publish(draft = makePartDraft(base = "dev"), console = console)
                    }
                }
            }
        }

        given("ветку начали от long-ветки") {
            every { git.base(branch = "task/13-x") } returns "long/13-big"
            `when` {
                command.run(args = makeDraftArgs(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.base(branch = "task/13-x")
                        publisher.publish(draft = makePartDraft(base = "long/13-big"), console = console)
                    }
                }
            }
        }

        given("--ready у последнего PR задачи") {
            every { issues.pullRequest(branch = "task/13-x") } returns PullRequest(number = 32, merged = false)
            every { pullRequests.link(head = "task/13-x") } returns "Closes #13"
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            `when` {
                command.run(args = listOf("--ready"), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        issues.pullRequest(branch = "task/13-x")
                        pullRequests.ready(head = "task/13-x")
                        console.line(text = "PR #32 готов к ревью")
                        pullRequests.link(head = "task/13-x")
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        board.move(item = makeInProgressItem(), status = "In review")
                        console.line(text = "#13 → In review")
                    }
                }
            }
        }

        given("--ready у PR с Part of") {
            every { issues.pullRequest(branch = "task/13-x") } returns PullRequest(number = 32, merged = false)
            every { pullRequests.link(head = "task/13-x") } returns "Part of #13"
            `when` {
                command.run(args = listOf("--ready"), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        issues.pullRequest(branch = "task/13-x")
                        pullRequests.ready(head = "task/13-x")
                        console.line(text = "PR #32 готов к ревью")
                        pullRequests.link(head = "task/13-x")
                        console.line(text = "#13 — PR не последний (Part of), статус остаётся")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNoPrMessage()) {
            every { issues.pullRequest(branch = "task/13-x") } returns null
            command.run(args = listOf("--ready"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("из long-ветки PR в dev открывает task final 13") {
            every { git.currentBranch() } returns "long/13-big"
            command.run(args = makeDraftArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("«dev» — не ветка задачи: task pr запускается из task/<N>-…") {
            every { git.currentBranch() } returns "dev"
            command.run(args = makeDraftArgs(), console = console)
        }
    })
