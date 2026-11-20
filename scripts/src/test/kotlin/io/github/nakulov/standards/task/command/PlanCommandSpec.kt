package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.IssueParts
import io.github.nakulov.standards.task.github.Issues
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.verifySequence
import java.io.File

class PlanCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val console = mockk<Console>()
        val board = mockk<Board>()
        val root = makePlanRoot()
        val temp = makePlanTemp(root = root)
        val command = PlanCommand(git = git, issues = issues, board = board, directory = root)
        val merged = IssueParts.merge(
            body = makePlanIssueBody(),
            parts = File(root, "parts.md").readText(),
            planLine = PLAN_LINE,
        )
        justRun { console.line(text = any()) }
        justRun { console.error(text = any()) }
        justRun { git.fetch() }
        justRun { git.addWorktree(path = any(), ref = any()) }
        justRun { git.removeWorktree(path = any()) }
        justRun { git.commitAll(path = any(), message = any()) }
        justRun { git.pushHead(path = any(), branch = any()) }
        justRun { git.moveBranch(branch = any(), commit = any()) }
        justRun { issues.editBody(number = any(), body = any()) }
        justRun { board.move(item = any(), status = any()) }
        every { issues.nodeId(number = 53) } returns "I_issue53"
        every { board.item(issue = 53, nodeId = "I_issue53") } returns makeGroomedItem()
        every { git.root() } returns root.path
        every { git.commonDir() } returns File(root, ".git").path
        every { git.openedAt(branch = PLAN_BRANCH) } returns null
        every { git.hasChanges(path = temp) } returns true
        every { git.head(path = temp) } returns "abc123"
        every { issues.info(number = 53) } returns makeTaskInfo().copy(number = 53)
        every { issues.body(number = 53) } returns makePlanIssueBody()

        given("ветки нет") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            `when` {
                command.run(args = makePlanArgs(), console = console)
                then {
                    File(root, "plans/commit-commands.md").exists() shouldBe false
                    verifySequence {
                        git.root()
                        issues.info(number = 53)
                        issues.body(number = 53)
                        git.fetch()
                        git.openedAt(branch = PLAN_BRANCH)
                        git.hasRemoteBranch(name = PLAN_BRANCH)
                        git.hasLocalBranch(name = PLAN_BRANCH)
                        git.commonDir()
                        git.addWorktree(path = temp, ref = "origin/dev")
                        git.hasChanges(path = temp)
                        git.commitAll(path = temp, message = "Добавить план: Команды коммита")
                        git.pushHead(path = temp, branch = PLAN_BRANCH)
                        console.line(text = "Добавить план: Команды коммита → $PLAN_BRANCH")
                        git.head(path = temp)
                        git.removeWorktree(path = temp)
                        issues.editBody(number = 53, body = merged)
                        console.line(text = "#53: тело задачи обновлено, локальный plans/commit-commands.md удалён")
                        issues.nodeId(number = 53)
                        board.item(issue = 53, nodeId = "I_issue53")
                        console.line(text = "#53 уже в «Groomed»")
                    }
                }
            }
        }

        given("ветка есть, локальная отстала") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
            every {
                git.isAncestor(ancestor = "refs/heads/$PLAN_BRANCH", descendant = "origin/$PLAN_BRANCH")
            } returns true
            `when` {
                command.run(args = makePlanArgs(), console = console)
                then {
                    verify { git.addWorktree(path = temp, ref = "origin/$PLAN_BRANCH") }
                    verify { git.commitAll(path = temp, message = "Обновить план: Команды коммита") }
                    verify { git.moveBranch(branch = PLAN_BRANCH, commit = "abc123") }
                }
            }
        }

        given("план не изменился") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            every { git.hasChanges(path = temp) } returns false
            `when` {
                command.run(args = makePlanArgs(), console = console)
                then {
                    verify(exactly = 0) { git.commitAll(path = any(), message = any()) }
                    verify { console.line(text = "план не изменился — коммита нет") }
                    verify { issues.editBody(number = 53, body = merged) }
                }
            }
        }

        given("план не изменился, локальная отстала") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
            every {
                git.isAncestor(ancestor = "refs/heads/$PLAN_BRANCH", descendant = "origin/$PLAN_BRANCH")
            } returns true
            every { git.hasChanges(path = temp) } returns false
            `when` {
                command.run(args = makePlanArgs(), console = console)
                then {
                    verify { git.moveBranch(branch = PLAN_BRANCH, commit = "origin/$PLAN_BRANCH") }
                }
            }
        }

        given("коммит упал") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            every {
                git.commitAll(path = any(), message = any())
            } throws TaskError.Failure(message = "git commit: сбой")
            `when` {
                shouldThrow<TaskError.Failure> { command.run(args = makePlanArgs(), console = console) }
                then {
                    verify { git.removeWorktree(path = temp) }
                    verify(exactly = 0) { issues.editBody(number = any(), body = any()) }
                }
            }
        }

        given("worktree не удалился") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            every { git.removeWorktree(path = temp) } throws TaskError.Failure(message = "git worktree: занят")
            `when` {
                command.run(args = makePlanArgs(), console = console)
                then {
                    verify { console.error(text = "удалите вручную: git worktree remove --force $temp") }
                    verify { issues.editBody(number = 53, body = merged) }
                }
            }
        }

        given("задача в Backlog") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            every { board.item(issue = 53, nodeId = "I_issue53") } returns makeBacklogItem()
            `when` {
                command.run(args = makePlanArgs(), console = console)
                then {
                    verifyOrder {
                        issues.editBody(number = 53, body = merged)
                        board.move(item = makeBacklogItem(), status = Board.GROOMED)
                        console.line(text = "#53 → Groomed")
                    }
                }
            }
        }

        given("статус не сменился") {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            every {
                board.item(issue = 53, nodeId = "I_issue53")
            } throws TaskError.Refusal(message = "#53 не на доске — добавьте её на доску проекта")
            `when` {
                val error = shouldThrow<TaskError.Failure> { command.run(args = makePlanArgs(), console = console) }
                then {
                    error.message shouldBe "план сохранён, статус — нет: task groomed 53 " +
                        "(#53 не на доске — добавьте её на доску проекта)"
                    File(root, "plans/commit-commands.md").exists() shouldBe false
                    verify { issues.editBody(number = 53, body = merged) }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Failure>(
            "план сохранён, тело задачи — нет: повторите task plan (gh issue: сбой)",
        ) {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
            every { issues.editBody(number = 53, body = any()) } throws TaskError.Failure(message = "gh issue: сбой")
            command.run(args = makePlanArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#53 закрыта") {
            every { issues.info(number = 53) } returns makeClosedInfo().copy(number = 53)
            command.run(args = makePlanArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#53 — эпик: план — на подзадачу") {
            every { issues.info(number = 53) } returns makeEpicInfo().copy(number = 53)
            command.run(args = makePlanArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "ветка $PLAN_BRANCH открыта в /work/wt — выполнение начато, план правит исполнитель",
        ) {
            every { git.openedAt(branch = PLAN_BRANCH) } returns "/work/wt"
            command.run(args = makePlanArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "локальная $PLAN_BRANCH расходится с origin/$PLAN_BRANCH — сведите вручную",
        ) {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
            every {
                git.isAncestor(ancestor = "refs/heads/$PLAN_BRANCH", descendant = "origin/$PLAN_BRANCH")
            } returns false
            command.run(args = makePlanArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "локальная $PLAN_BRANCH не отправлена в origin — отправьте её или удалите",
        ) {
            every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
            every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
            command.run(args = makePlanArgs(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>("нет plans/other.md — сначала напишите план") {
            command.run(args = listOf("53", "other", "--issue-parts", "parts.md"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>("использование: task plan ${command.usage}") {
            command.run(args = listOf("53", "commit-commands"), console = console)
        }
    })
