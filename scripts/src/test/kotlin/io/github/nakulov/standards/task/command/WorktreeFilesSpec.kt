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
import io.mockk.verify
import io.mockk.verifySequence
import java.io.File

class WorktreeFilesSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val git = mockk<Git>()
        val console = mockk<Console>()
        val files = WorktreeFiles(git = git)
        justRun { console.line(text = any()) }

        given("нет .worktreeinclude") {
            val main = makeMainFolder(include = null)
            val copy = makeCopyFolder()
            every { git.mainRoot() } returns main.path
            `when` {
                files.copyInto(copy = copy.path, console = console)
                then {
                    verifySequence { git.mainRoot() }
                    verify(exactly = 0) { console.line(text = any()) }
                }
            }
        }

        given("список — файл и папка") {
            val main = makeMainFolder(include = "local.properties\n.idea/\n")
            val copy = makeCopyFolder()
            every { git.mainRoot() } returns main.path
            every {
                git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
            } returns listOf(".idea/x.xml", "local.properties")
            `when` {
                files.copyInto(copy = copy.path, console = console)
                then {
                    File(copy, "local.properties").readText() shouldBe "sdk.dir=/sdk\n"
                    File(copy, ".idea/x.xml").readText() shouldBe "<x/>\n"
                    verifySequence {
                        git.mainRoot()
                        git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
                        console.line(text = "из .worktreeinclude скопировано: .idea/x.xml, local.properties")
                    }
                }
            }
        }

        given("строки-пути, которых нет в основной папке; шаблоны, комментарии и исключения не проверяются") {
            val main = makeMainFolder(include = "local.properties\nsecret.env\nkeys/\n*.jks\n# комментарий\n!a.txt\n")
            val copy = makeCopyFolder()
            every { git.mainRoot() } returns main.path
            every {
                git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
            } returns listOf("local.properties")
            `when` {
                files.copyInto(copy = copy.path, console = console)
                then {
                    verifySequence {
                        git.mainRoot()
                        git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
                        console.line(text = "из .worktreeinclude скопировано: local.properties")
                        console.line(text = ".worktreeinclude: secret.env — нет в основной папке, пропущено")
                        console.line(text = ".worktreeinclude: keys/ — нет в основной папке, пропущено")
                    }
                }
            }
        }
    })
