package io.github.nakulov.standards.task.shell

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class ProcessShellSpec :
    BehaviorSpec({
        val shell = ProcessShell(directory = makeWorkDirectory())

        shouldReturnTest(ShellResult(code = 0, out = "привет\n", err = "")) {
            shell.run(command = listOf("echo", "привет"))
        }

        shouldReturnTest(ShellResult(code = 3, out = "", err = "ошибка\n")) {
            shell.run(command = listOf("sh", "-c", "echo ошибка >&2; exit 3"))
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeMissingToolMessage()) {
            shell.run(command = listOf("no-such-tool", "--version"))
        }
    })
