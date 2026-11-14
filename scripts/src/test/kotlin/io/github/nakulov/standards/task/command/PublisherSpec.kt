package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.github.nakulov.standards.task.github.Issues
import io.github.nakulov.standards.task.github.PullRequest
import io.github.nakulov.standards.task.github.PullRequests
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class PublisherSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val issues = mockk<Issues>()
        val pullRequests = mockk<PullRequests>()
        val checks = mockk<CheckRunner>()
        val console = mockk<Console>()
        val publisher = Publisher(git = git, issues = issues, pullRequests = pullRequests, checks = checks)
        justRun { console.line(text = any()) }
        justRun { checks.run(command = any()) }
        justRun { git.push(name = any()) }
        every { git.isClean() } returns true

        given("PR ещё нет") {
            every { issues.pullRequest(branch = "task/13-x") } returns null
            every {
                pullRequests.create(
                    base = "dev",
                    head = "task/13-x",
                    title = "Заголовок",
                    body = makePublishedBody(),
                    draft = true,
                )
            } returns "https://github.com/owner/repo/pull/32"
            `when` {
                publisher.publish(draft = makeParsedDraft(), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        checks.run(command = "./gradlew test")
                        console.line(text = "проверка «./gradlew test» — прошла")
                        checks.run(command = "scripts/task --help")
                        console.line(text = "проверка «scripts/task --help» — прошла")
                        git.push(name = "task/13-x")
                        issues.pullRequest(branch = "task/13-x")
                        pullRequests.create(
                            base = "dev",
                            head = "task/13-x",
                            title = "Заголовок",
                            body = makePublishedBody(),
                            draft = true,
                        )
                        console.line(text = "PR (черновик): https://github.com/owner/repo/pull/32")
                    }
                }
            }
        }

        given("PR уже открыт") {
            every { issues.pullRequest(branch = "task/13-x") } returns PullRequest(number = 32, merged = false)
            every {
                pullRequests.edit(head = "task/13-x", title = "Заголовок", body = makePublishedBody())
            } returns "https://github.com/owner/repo/pull/32"
            `when` {
                publisher.publish(draft = makeParsedDraft(), console = console)
                then {
                    verifySequence {
                        git.isClean()
                        checks.run(command = "./gradlew test")
                        console.line(text = "проверка «./gradlew test» — прошла")
                        checks.run(command = "scripts/task --help")
                        console.line(text = "проверка «scripts/task --help» — прошла")
                        git.push(name = "task/13-x")
                        issues.pullRequest(branch = "task/13-x")
                        pullRequests.edit(head = "task/13-x", title = "Заголовок", body = makePublishedBody())
                        console.line(text = "PR обновлён: https://github.com/owner/repo/pull/32")
                    }
                }
            }
        }

        given("проверка не прошла") {
            every { checks.run(command = "./gradlew test") } throws makeFailedCheckRefusal()
            `when` {
                val error = shouldThrow<TaskError.Refusal> {
                    publisher.publish(draft = makeParsedDraft(), console = console)
                }
                then {
                    error.message shouldBe makeFailedCheckMessage()
                    verifySequence {
                        git.isClean()
                        checks.run(command = "./gradlew test")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("PR #32 уже слит — task done") {
            every { issues.pullRequest(branch = "task/13-x") } returns PullRequest(number = 32, merged = true)
            publisher.publish(draft = makeParsedDraft(), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeDirtyMessage()) {
            every { git.isClean() } returns false
            publisher.publish(draft = makeParsedDraft(), console = console)
        }
    })
