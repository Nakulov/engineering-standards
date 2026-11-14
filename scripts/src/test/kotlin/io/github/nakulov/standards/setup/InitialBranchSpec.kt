package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder

class InitialBranchSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val git = mockk<Git>()
        val step = InitialBranch(gitHub = gitHub, git = git, files = makeStandardPaths())
        every { gitHub.read(args = makeBranchesArgs()) } returns "1"
        every { gitHub.read(args = makeDefaultBranchArgs()) } returns "main"
        every { gitHub.read(args = makeCommitsArgs()) } returns "1"
        every { gitHub.read(args = makeDevBranchArgs()) } throws TaskError.Failure(message = makeBranchNotFound())
        every { gitHub.read(args = makeMainShaArgs()) } returns "abc123"
        every { gitHub.write(args = any()) } returns ""

        shouldReturnTest(listOf(makeBranchFinding(status = Finding.Status.OK, text = "основная ветка dev"))) {
            every { gitHub.read(args = makeDefaultBranchArgs()) } returns "dev"
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(listOf(makeEmptyCheck())) {
            every { gitHub.read(args = makeBranchesArgs()) } returns "0"
            step.run(mode = makeCheckMode())
        }

        given("пустой репозиторий: первый коммит делает setup") {
            every { gitHub.read(args = makeBranchesArgs()) } returns "0"
            every { git.hasCommits() } returns false
            justRun { git.firstCommit(branch = "dev", message = makeFirstCommitMessage(), paths = makeStandardPaths()) }
            justRun { git.push(name = "dev") }
            `when` {
                val findings = step.run(mode = makeApplyMode())
                then {
                    findings shouldBe listOf(makeFirstCommitFinding())
                    verifyOrder {
                        git.firstCommit(branch = "dev", message = makeFirstCommitMessage(), paths = makeStandardPaths())
                        git.push(name = "dev")
                        gitHub.write(args = makeSetDefaultArgs())
                    }
                }
            }
        }

        shouldReturnTest(listOf(makeLocalCommitsFinding())) {
            every { gitHub.read(args = makeBranchesArgs()) } returns "0"
            every { git.hasCommits() } returns true
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(listOf(makeReplaceFinding(status = Finding.Status.NEEDS_CONFIRM))) {
            step.run(mode = makeApplyMode())
        }

        given("main с одним коммитом и --confirm: dev от main, основная, main удалена") {
            `when` {
                val findings = step.run(mode = makeConfirmedMode())
                then {
                    findings shouldBe listOf(makeReplaceFinding(status = Finding.Status.FIXED))
                    verifyOrder {
                        gitHub.write(args = makeCreateDevArgs())
                        gitHub.write(args = makeSetDefaultArgs())
                        gitHub.write(args = makeDeleteMainArgs())
                    }
                }
            }
        }

        given("main с историей") {
            every { gitHub.read(args = makeCommitsArgs()) } returns "2"
            `when` {
                val findings = step.run(mode = makeConfirmedMode())
                then {
                    findings shouldBe listOf(makeHistoryFinding())
                    verify(exactly = 0) { gitHub.write(args = any()) }
                }
            }
        }
    })
