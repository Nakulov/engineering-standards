package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class StageCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val console = mockk<Console>()
        val command = StageCommand(gitHub = gitHub, template = makeStageTemplateFile())
        justRun { console.line(text = any()) }

        given("все разделы заполнены, со сроком") {
            every { gitHub.write(args = makeMilestoneArgs()) } returns "https://github.com/owner/repo/milestone/1"
            `when` {
                val args = makeStageArgs(file = "stage/full.md") + listOf("--due", "2026-12-01")
                command.run(args = args, console = console)
                then {
                    verifySequence {
                        gitHub.write(args = makeMilestoneArgs())
                        console.line(text = "этап «Этап 1»: https://github.com/owner/repo/milestone/1")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeEmptySectionsMessage()) {
            command.run(args = makeStageArgs(file = "stage/partial.md"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>("срок «1 декабря» — в формате ГГГГ-ММ-ДД") {
            command.run(args = makeStageArgs(file = "stage/full.md") + listOf("--due", "1 декабря"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeStageUsageMessage()) {
            command.run(args = listOf("list"), console = console)
        }
    })
