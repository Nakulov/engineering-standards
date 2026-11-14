package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.standards.task.git.Git
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class StandardFilesSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val root = makeProjectRoot()
        val plugin = makePluginRoot()
        every { git.root() } returns root.path
        val step = StandardFiles(git = git, pluginRoot = plugin)

        shouldReturnTest(makeMissingFindings()) {
            writeProjectFile(root = root, path = ".github/ISSUE_TEMPLATE/task.md", text = "старый шаблон\n")
            writeProjectFile(root = root, path = ".github/ISSUE_TEMPLATE/story.md", text = "свой шаблон\n")
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(makeCopiedFiles()) {
            writeProjectFile(root = root, path = ".github/ISSUE_TEMPLATE/task.md", text = "старый шаблон\n")
            step.run(mode = makeApplyMode())
            readProjectFiles(root = root)
        }

        shouldReturnTest(listOf(makeFilesOk())) {
            step.run(mode = makeApplyMode())
            step.run(mode = makeCheckMode())
        }
    })
