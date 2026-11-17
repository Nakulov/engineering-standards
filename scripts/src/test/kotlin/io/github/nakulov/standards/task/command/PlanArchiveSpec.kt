package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.git.Git
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence
import java.io.File

class PlanArchiveSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val console = mockk<Console>()
        val (branch, main) = makeArchiveFolders()
        val archive = PlanArchive(git = git)
        justRun { console.line(text = any()) }
        justRun { git.removeAndCommit(paths = any(), message = any()) }
        every { git.root() } returns branch.path
        every { git.mainRoot() } returns main.path

        given("в ветке добавлен план") {
            every { git.addedPaths(base = "origin/dev") } returns
                listOf("scripts/a.kt", "plans/commit-commands.md", "plans/done/old.md")
            `when` {
                archive.run(console = console)
                then {
                    File(main, "plans/done/commit-commands.md").readText() shouldBe
                        "# Команды коммита\n\n- [x] Задача 1\n"
                    verifySequence {
                        git.addedPaths(base = "origin/dev")
                        git.root()
                        git.mainRoot()
                        git.removeAndCommit(
                            paths = listOf("plans/commit-commands.md"),
                            message = "Убрать план: Команды коммита",
                        )
                        console.line(text = "план убран из ветки, копия — ${File(main, "plans/done").path}")
                    }
                }
            }
        }

        given("плана в ветке нет") {
            every { git.addedPaths(base = "origin/dev") } returns listOf("scripts/a.kt")
            `when` {
                archive.run(console = console)
                then {
                    verifySequence { git.addedPaths(base = "origin/dev") }
                }
            }
        }
    })
