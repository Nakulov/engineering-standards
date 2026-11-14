package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class CheckRunnerSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val shell = mockk<Shell>()
        val checks = CheckRunner(shell = shell)

        shouldReturnTest(Unit) {
            every { shell.run(command = makeCheckCommand()) } returns makePassedCheck()
            checks.run(command = "./gradlew test")
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeFailedCheckMessage()) {
            every { shell.run(command = makeCheckCommand()) } returns makeFailedCheck()
            checks.run(command = "./gradlew test")
        }
    })
