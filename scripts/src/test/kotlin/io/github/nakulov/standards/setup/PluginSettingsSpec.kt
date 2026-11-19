package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.git.Git
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import java.io.File

class PluginSettingsSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val root = makeProjectRoot()
        every { git.root() } returns root.path
        val step = PluginSettings(git = git, pluginRoot = makeProjectRoot())

        shouldReturnTest(makeSettingsAddedFindings()) {
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeFullSettings()) {
            step.run(mode = makeApplyMode())
            readSettings(root = root)
        }

        shouldReturnTest(makeProjectSettingsWithStandard()) {
            writeSettings(root = root, text = makeProjectSettings())
            step.run(mode = makeApplyMode())
            readSettings(root = root)
        }

        shouldReturnTest(makeSettingsMissingFindings()) {
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(false) {
            step.run(mode = makeCheckMode())
            File(root, ".claude/settings.json").exists()
        }

        shouldReturnTest(makeDisabledFindings()) {
            writeSettings(root = root, text = makeDisabledSettings())
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeDisabledSettings()) {
            writeSettings(root = root, text = makeDisabledSettings())
            step.run(mode = makeApplyMode())
            readSettings(root = root)
        }

        shouldReturnTest(makeOtherSourceFindings()) {
            writeSettings(root = root, text = makeOtherSourceSettings())
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeOtherSourceSettings()) {
            writeSettings(root = root, text = makeOtherSourceSettings())
            step.run(mode = makeApplyMode())
            readSettings(root = root)
        }

        shouldReturnTest(makeFullSettings()) {
            writeSettings(root = root, text = makeOtherSourceSettings())
            step.run(mode = makeConfirmedMode())
            readSettings(root = root)
        }

        shouldReturnTest(makeStandardRepositoryFindings()) {
            PluginSettings(git = git, pluginRoot = root).run(mode = makeApplyMode())
        }

        shouldReturnTest(makePermissionOnlySettings()) {
            PluginSettings(git = git, pluginRoot = root).run(mode = makeApplyMode())
            readSettings(root = root)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json:3: ожидается ключ в кавычках") {
            writeSettings(root = root, text = makeBrokenSettings())
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeBrokenSettings()) {
            writeSettings(root = root, text = makeBrokenSettings())
            runCatching { step.run(mode = makeApplyMode()) }
            readSettings(root = root)
        }

        shouldReturnTest(makeSettingsUnchangedFindings()) {
            step.run(mode = makeApplyMode())
            step.run(mode = makeApplyMode())
        }

        shouldReturnTest(makeFullSettings()) {
            writeSettings(root = root, text = "")
            step.run(mode = makeApplyMode())
            readSettings(root = root)
        }

        shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json: ожидается объект {…} на верхнем уровне") {
            writeSettings(root = root, text = "[]\n")
            step.run(mode = makeApplyMode())
        }

        shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json: permissions.allow — ожидается массив […]") {
            writeSettings(root = root, text = "{\"permissions\": {\"allow\": \"Bash(task:*)\"}}\n")
            step.run(mode = makeApplyMode())
        }

        shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json: enabledPlugins — ожидается объект {…}") {
            writeSettings(root = root, text = "{\"enabledPlugins\": []}\n")
            step.run(mode = makeApplyMode())
        }
    })
