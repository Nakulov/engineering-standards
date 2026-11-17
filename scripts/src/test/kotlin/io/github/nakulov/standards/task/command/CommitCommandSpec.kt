package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.File

class CommitCommandSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val console = mockk<Console>()
        val directory = makeCommitDirectory()
        val command = CommitCommand(git = git, directory = directory)
        val messageFile = File(directory, "message.txt").path
        justRun { console.line(text = any()) }
        justRun { git.commitPaths(paths = any(), messageFile = any()) }

        given("файлы изменены, план отмечен") {
            every { git.changedPaths() } returns
                listOf("scripts/a.kt", "scripts/b.kt", "plans/commit-commands.md", "other.txt")
            `when` {
                command.run(args = listOf("--message-file", "message.txt"), console = console)
                then {
                    verifySequence {
                        git.changedPaths()
                        git.commitPaths(
                            paths = listOf("scripts/a.kt", "scripts/b.kt", "plans/commit-commands.md"),
                            messageFile = messageFile,
                        )
                        console.line(text = "коммит: Добавить task commit")
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(
            "`scripts/b.kt` не изменён или не существует — уберите из «Файлов»",
        ) {
            every { git.changedPaths() } returns listOf("scripts/a.kt")
            command.run(args = listOf("--message-file", "message.txt"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>("нет файла сообщения nope.txt") {
            command.run(args = listOf("--message-file", "nope.txt"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Failure>("использование: task commit ${command.usage}") {
            command.run(args = emptyList(), console = console)
        }
    })
