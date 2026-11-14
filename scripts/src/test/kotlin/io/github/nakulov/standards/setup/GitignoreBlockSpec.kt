package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.standards.task.git.Git
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class GitignoreBlockSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val root = makeProjectRoot()
        every { git.root() } returns root.path
        val step = GitignoreBlock(git = git, block = makeBlock())

        shouldReturnTest(listOf(makeGitignoreFinding(status = Finding.Status.DEVIATION, text = "нет общего блока"))) {
            writeGitignore(root = root, text = makeProjectLines())
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(makeBlockWithProjectLines()) {
            writeGitignore(root = root, text = makeProjectLines())
            step.run(mode = makeApplyMode())
            readGitignore(root = root)
        }

        shouldReturnTest(makeBlockOnly()) {
            step.run(mode = makeApplyMode())
            readGitignore(root = root)
        }

        shouldReturnTest(makeBlockWithProjectLines()) {
            writeGitignore(root = root, text = makeOutdatedBlockWithProjectLines())
            step.run(mode = makeApplyMode())
            readGitignore(root = root)
        }

        shouldReturnTest(makeBlockWithProjectLines()) {
            writeGitignore(root = root, text = makeProjectLines() + "\n" + makeBlockOnly())
            step.run(mode = makeApplyMode())
            readGitignore(root = root)
        }

        shouldReturnTest(listOf(makeGitignoreFinding(status = Finding.Status.OK, text = null))) {
            writeGitignore(root = root, text = makeBlockWithProjectLines())
            step.run(mode = makeCheckMode())
        }

        shouldReturnTest(listOf(makeBrokenMarkersFinding())) {
            writeGitignore(root = root, text = "# >>> engineering-standards\nbuild/\n")
            step.run(mode = makeApplyMode())
        }
    })
