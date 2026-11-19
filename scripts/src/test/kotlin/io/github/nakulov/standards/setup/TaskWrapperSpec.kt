package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File

class TaskWrapperSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val home = makeProjectRoot()
        val plugin = makeWrapperPlugin()
        val step = TaskWrapper(home = home, pluginRoot = plugin, path = "${File(home, ".local/bin").path}:/usr/bin")

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.FIXED, text = makeInstalledText()))) {
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeWrapperSource() to true) {
            step.run(mode = makeApplyMode())
            readWrapper(home = home) to File(home, ".local/bin/task").canExecute()
        }

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.NOTE, text = makeMissingText()))) {
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.FIXED, text = makeUpdatedText()))) {
            writeWrapper(home = home, text = makeOldWrapper())
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.DEVIATION, text = makeDifferentText()))) {
            writeWrapper(home = home, text = makeOldWrapper())
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.OK, text = "обёртка по стандарту"))) {
            writeWrapper(home = home, text = makeWrapperSource())
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.NOTE, text = makeForeignText(plugin = plugin)))) {
            writeWrapper(home = home, text = makeForeignTask())
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeForeignTask()) {
            writeWrapper(home = home, text = makeForeignTask())
            step.run(mode = makeApplyMode())
            readWrapper(home = home)
        }

        shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.OK, text = "обёртка по стандарту"), makePathNote())) {
            writeWrapper(home = home, text = makeWrapperSource())
            TaskWrapper(home = home, pluginRoot = plugin, path = "/usr/bin").run(mode = makeCheckMode())
        }

        shouldThrowWithMessageTest<TaskError.Failure>("~/.local/bin/task: не записана — проверьте права на ~/.local/bin") {
            makeReadOnlyBin(home = home)
            step.run(mode = makeApplyMode())
        }
    })
