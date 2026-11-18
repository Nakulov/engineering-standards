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
import io.kotest.assertions.throwables.shouldThrowWithMessage
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
        val files = mockk<WorktreeFiles>()
        val command = StartCommand(git = git, issues = issues, board = board, files = files)
        justRun { console.line(text = any()) }
        justRun { files.copyInto(copy = any(), console = any()) }
        justRun { board.move(item = any(), status = any()) }
        justRun { git.fetch() }
        justRun { git.createBranch(name = any(), base = any()) }
        justRun { git.switchTo(name = any()) }
        justRun { git.push(name = any()) }
        justRun { git.setBase(branch = any(), base = any()) }
        every { git.isClean() } returns true
        every { issues.nodeId(number = 13) } returns "I_issue13"
        justRun { git.addBranchWorktree(path = any(), name = any(), base = any()) }
        justRun { git.addWorktreeOn(path = any(), name = any()) }
        justRun { git.fastForwardIn(path = any(), name = any()) }
        justRun { git.switchIn(path = any(), name = any()) }
        val existing = makeExistingCopy().path
        every { git.openedAt(branch = any()) } returns null
        every { git.root() } returns "/repo"
        every { git.copyPath(slug = "task-scripts") } returns makeCopyPath()

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
                        git.openedAt(branch = "task/13-task-scripts")
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

        given("повторный запуск: long-ветка есть и не отстала от origin, задача уже In progress") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "long/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "long/13-task-scripts") } returns true
            every {
                git.isAncestor(ancestor = "origin/long/13-task-scripts", descendant = "refs/heads/long/13-task-scripts")
            } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--long"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.openedAt(branch = "long/13-task-scripts")
                        git.hasLocalBranch(name = "long/13-task-scripts")
                        git.hasRemoteBranch(name = "long/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/long/13-task-scripts",
                            descendant = "refs/heads/long/13-task-scripts",
                        )
                        git.switchTo(name = "long/13-task-scripts")
                        console.line(text = "ветка long/13-task-scripts уже есть — переключился на неё")
                        console.line(text = "#13 уже In progress")
                    }
                }
            }
        }

        given("ветка есть только в origin") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.switchTo(name = "task/13-task-scripts")
                        console.line(text = "ветка task/13-task-scripts уже есть — переключился на неё")
                        console.line(text = "#13 уже In progress")
                    }
                }
            }
        }

        given("локальная long-ветка отстала от origin: в неё слили PR блока") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "long/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "long/13-task-scripts") } returns true
            every {
                git.isAncestor(ancestor = "origin/long/13-task-scripts", descendant = "refs/heads/long/13-task-scripts")
            } returns false
            every {
                git.isAncestor(ancestor = "refs/heads/long/13-task-scripts", descendant = "origin/long/13-task-scripts")
            } returns true
            justRun { git.fastForward(name = "long/13-task-scripts") }
            `when` {
                command.run(args = listOf("13", "task-scripts", "--long"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.openedAt(branch = "long/13-task-scripts")
                        git.hasLocalBranch(name = "long/13-task-scripts")
                        git.hasRemoteBranch(name = "long/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/long/13-task-scripts",
                            descendant = "refs/heads/long/13-task-scripts",
                        )
                        git.isAncestor(
                            ancestor = "refs/heads/long/13-task-scripts",
                            descendant = "origin/long/13-task-scripts",
                        )
                        git.switchTo(name = "long/13-task-scripts")
                        git.fastForward(name = "long/13-task-scripts")
                        console.line(text = makeFastForwardLine())
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                    }
                }
            }
        }

        given("локальная ветка разошлась с origin") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            every { git.isAncestor(ancestor = any(), descendant = any()) } returns false
            `when` {
                shouldThrowWithMessage<TaskError.Refusal>(makeDivergedMessage()) {
                    command.run(args = listOf("13", "task-scripts"), console = console)
                }
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/task/13-task-scripts",
                            descendant = "refs/heads/task/13-task-scripts",
                        )
                        git.isAncestor(
                            ancestor = "refs/heads/task/13-task-scripts",
                            descendant = "origin/task/13-task-scripts",
                        )
                    }
                }
            }
        }

        given("новая long-ветка") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "long/13-big") } returns false
            every { git.hasRemoteBranch(name = "long/13-big") } returns false
            `when` {
                command.run(args = listOf("13", "big", "--long"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.openedAt(branch = "long/13-big")
                        git.hasLocalBranch(name = "long/13-big")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.createBranch(name = "long/13-big", base = "origin/dev")
                        console.line(text = "ветка long/13-big от origin/dev")
                        git.push(name = "long/13-big")
                        console.line(text = "ветка long/13-big отправлена в origin")
                        console.line(text = "#13 уже In progress")
                    }
                }
            }
        }

        given("часть long-ветки: --from") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = "long/13-big") } returns true
            `when` {
                command.run(args = listOf("13", "part-a", "--from", "long/13-big"), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.fetch()
                        git.openedAt(branch = "task/13-part-a")
                        git.hasLocalBranch(name = "task/13-part-a")
                        git.hasRemoteBranch(name = "task/13-part-a")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.createBranch(name = "task/13-part-a", base = "origin/long/13-big")
                        git.setBase(branch = "task/13-part-a", base = "long/13-big")
                        console.line(text = "ветка task/13-part-a от origin/long/13-big")
                        console.line(text = "#13 уже In progress")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeNoLongBranchMessage()) {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = any()) } returns false
            command.run(args = listOf("13", "part-a", "--from", "long/13-big"), console = console)
        }

        withGivens(
            nameFn = { given -> "неверные аргументы ${given.arg1}" },
            case(arg1 = listOf("13")),
            case(arg1 = listOf("13", "part-a", "--long", "--from", "long/13-big")),
            case(arg1 = listOf("13", "part-a", "--from", "task/13-x")),
            case(arg1 = listOf("13", "part-a", "--from")),
        ) { args ->
            shouldThrowWithMessageTest<TaskError.Failure>(makeUsageMessage()) {
                command.run(args = args, console = console)
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

        given("--worktree, ветки нет, основная папка грязная") {
            every { git.isClean() } returns false
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.addBranchWorktree(path = makeCopyPath(), name = "task/13-task-scripts", base = "origin/dev")
                        console.line(text = "ветка task/13-task-scripts от origin/dev — копия ${makeCopyPath()}")
                        files.copyInto(copy = makeCopyPath(), console = console)
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--worktree, ветка есть только в origin") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.addWorktreeOn(path = makeCopyPath(), name = "task/13-task-scripts")
                        console.line(text = "ветка task/13-task-scripts уже есть — копия ${makeCopyPath()}")
                        files.copyInto(copy = makeCopyPath(), console = console)
                        console.line(text = "#13 уже In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--worktree повторно: копия уже есть, ветка отстала от origin") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.openedAt(branch = "task/13-task-scripts") } returns makeCopyPath()
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            every {
                git.isAncestor(ancestor = "origin/task/13-task-scripts", descendant = "refs/heads/task/13-task-scripts")
            } returns false
            every {
                git.isAncestor(ancestor = "refs/heads/task/13-task-scripts", descendant = "origin/task/13-task-scripts")
            } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/task/13-task-scripts",
                            descendant = "refs/heads/task/13-task-scripts",
                        )
                        git.isAncestor(
                            ancestor = "refs/heads/task/13-task-scripts",
                            descendant = "origin/task/13-task-scripts",
                        )
                        git.fastForwardIn(path = makeCopyPath(), name = "task/13-task-scripts")
                        console.line(
                            text = "копия уже есть: ${makeCopyPath()} — перемотал до origin/task/13-task-scripts",
                        )
                        console.line(text = "#13 уже In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--long --worktree: новая long-ветка в копии") {
            every { git.copyPath(slug = "big") } returns "/repo/.claude/worktrees/big"
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "long/13-big") } returns false
            every { git.hasRemoteBranch(name = "long/13-big") } returns false
            `when` {
                command.run(args = listOf("13", "big", "--long", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "big")
                        git.fetch()
                        git.openedAt(branch = "long/13-big")
                        git.hasLocalBranch(name = "long/13-big")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.addBranchWorktree(path = "/repo/.claude/worktrees/big", name = "long/13-big", base = "origin/dev")
                        console.line(text = "ветка long/13-big от origin/dev — копия /repo/.claude/worktrees/big")
                        files.copyInto(copy = "/repo/.claude/worktrees/big", console = console)
                        git.push(name = "long/13-big")
                        console.line(text = "ветка long/13-big отправлена в origin")
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                        console.line(text = "/repo/.claude/worktrees/big")
                    }
                }
            }
        }

        given("--worktree, локальная ветка отстала от origin, копии нет") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            every {
                git.isAncestor(ancestor = "origin/task/13-task-scripts", descendant = "refs/heads/task/13-task-scripts")
            } returns false
            every {
                git.isAncestor(ancestor = "refs/heads/task/13-task-scripts", descendant = "origin/task/13-task-scripts")
            } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/task/13-task-scripts",
                            descendant = "refs/heads/task/13-task-scripts",
                        )
                        git.isAncestor(
                            ancestor = "refs/heads/task/13-task-scripts",
                            descendant = "origin/task/13-task-scripts",
                        )
                        git.addWorktreeOn(path = makeCopyPath(), name = "task/13-task-scripts")
                        git.fastForwardIn(path = makeCopyPath(), name = "task/13-task-scripts")
                        console.line(
                            text = "ветка task/13-task-scripts уже есть — копия ${makeCopyPath()} перемотана до " +
                                "origin/task/13-task-scripts",
                        )
                        files.copyInto(copy = makeCopyPath(), console = console)
                        console.line(text = "#13 уже In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--from long/… --worktree: ветка блока в своей копии") {
            every { git.copyPath(slug = "part-a") } returns "/repo/.claude/worktrees/part-a"
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = "long/13-big") } returns true
            `when` {
                command.run(args = listOf("13", "part-a", "--from", "long/13-big", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "part-a")
                        git.fetch()
                        git.openedAt(branch = "task/13-part-a")
                        git.hasLocalBranch(name = "task/13-part-a")
                        git.hasRemoteBranch(name = "task/13-part-a")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.addBranchWorktree(
                            path = "/repo/.claude/worktrees/part-a",
                            name = "task/13-part-a",
                            base = "origin/long/13-big",
                        )
                        git.setBase(branch = "task/13-part-a", base = "long/13-big")
                        console.line(
                            text = "ветка task/13-part-a от origin/long/13-big — копия /repo/.claude/worktrees/part-a",
                        )
                        files.copyInto(copy = "/repo/.claude/worktrees/part-a", console = console)
                        console.line(text = "#13 уже In progress")
                        console.line(text = "/repo/.claude/worktrees/part-a")
                    }
                }
            }
        }

        given("--long --worktree: своя копия long-ветки отсоединена после task done ветки блока") {
            every { git.copyPath(slug = "big") } returns existing
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "long/13-big") } returns true
            every { git.hasRemoteBranch(name = "long/13-big") } returns true
            every { git.isAncestor(ancestor = "origin/long/13-big", descendant = "refs/heads/long/13-big") } returns true
            every { git.worktrees() } returns listOf("/repo", existing)
            every { git.hasChanges(path = existing) } returns false
            `when` {
                command.run(args = listOf("13", "big", "--long", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "big")
                        git.fetch()
                        git.openedAt(branch = "long/13-big")
                        git.hasLocalBranch(name = "long/13-big")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.isAncestor(ancestor = "origin/long/13-big", descendant = "refs/heads/long/13-big")
                        git.worktrees()
                        git.hasChanges(path = existing)
                        git.switchIn(path = existing, name = "long/13-big")
                        console.line(text = "ветка long/13-big уже есть — копия $existing")
                        console.line(text = "#13 уже In progress")
                        console.line(text = existing)
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOccupiedMessage(folder = existing)) {
            every { git.copyPath(slug = "task-scripts") } returns existing
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            every { git.worktrees() } returns listOf("/repo")
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOccupiedMessage(folder = existing)) {
            every { git.copyPath(slug = "task-scripts") } returns existing
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOpenedMessage(folder = "/repo")) {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.openedAt(branch = "task/13-task-scripts") } returns "/repo"
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOpenedMessage(folder = makeCopyPath())) {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.openedAt(branch = "task/13-task-scripts") } returns makeCopyPath()
            command.run(args = listOf("13", "task-scripts"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#13 — исследование: ветка и копия не нужны") {
            every { issues.info(number = 13) } returns makeResearchInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            command.run(args = listOf("13", "wiki-spike", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#13 закрыта") {
            every { issues.info(number = 13) } returns makeClosedInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

    })
