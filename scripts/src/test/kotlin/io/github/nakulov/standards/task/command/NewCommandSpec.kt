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

class NewCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val console = mockk<Console>()
        val command = NewCommand(git = git, issues = issues, board = board)
        justRun { console.line(text = any()) }
        justRun { board.move(item = any(), status = any()) }
        every { git.root() } returns System.getProperty("task.repoRoot")
        every { issues.databaseId(number = 40) } returns "9040"
        every { issues.nodeId(number = 40) } returns "I_issue40"

        given("подзадача эпика с блокерами") {
            every {
                issues.create(
                    title = "Новая задача",
                    body = makeBodyText(),
                    label = "type:task",
                    parent = 14,
                    blockedBy = listOf(13, 8),
                )
            } returns 40
            every { board.add(issue = 40, databaseId = "9040", nodeId = "I_issue40") } returns makeGroomedItem()
            `when` {
                command.run(args = makeNewArgs() + listOf("--parent", "14", "--blocked-by", "#13,8"), console = console)
                then {
                    verifySequence {
                        git.root()
                        issues.create(
                            title = "Новая задача",
                            body = makeBodyText(),
                            label = "type:task",
                            parent = 14,
                            blockedBy = listOf(13, 8),
                        )
                        console.line(text = "#40 создана: type:task, подзадача #14")
                        issues.databaseId(number = 40)
                        issues.nodeId(number = 40)
                        board.add(issue = 40, databaseId = "9040", nodeId = "I_issue40")
                        board.move(item = makeGroomedItem(), status = "Backlog")
                        console.line(text = "#40 → Backlog")
                    }
                }
            }
        }

        given("задача сразу в Backlog") {
            every {
                issues.create(
                    title = "Новая задача",
                    body = makeBodyText(),
                    label = "type:task",
                    parent = null,
                    blockedBy = emptyList(),
                )
            } returns 40
            every { board.add(issue = 40, databaseId = "9040", nodeId = "I_issue40") } returns makeBacklogItem()
            `when` {
                command.run(args = makeNewArgs(), console = console)
                then {
                    verifySequence {
                        git.root()
                        issues.create(
                            title = "Новая задача",
                            body = makeBodyText(),
                            label = "type:task",
                            parent = null,
                            blockedBy = emptyList(),
                        )
                        console.line(text = "#40 создана: type:task")
                        issues.databaseId(number = 40)
                        issues.nodeId(number = 40)
                        board.add(issue = 40, databaseId = "9040", nodeId = "I_issue40")
                        console.line(text = "#40 → Backlog")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeUnknownTypeMessage()) {
            command.run(args = listOf("story") + makeNewArgs().drop(n = 1), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>("«x» — не номер задачи") {
            command.run(args = makeNewArgs() + listOf("--parent", "x"), console = console)
        }
    })
