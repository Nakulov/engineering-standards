package io.github.nakulov.standards.task.cli

import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifySequence

class TaskSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val console = mockk<Console>()
        val command = mockk<Command>()
        justRun { console.line(text = any()) }
        justRun { console.error(text = any()) }
        every { command.usage } returns makeStartUsage()

        given("справка без команд") {
            val task = Task(commands = emptyMap(), console = console, debug = false)
            `when` {
                val code = task.run(args = listOf("--help"))
                then {
                    code shouldBe 0
                    verifySequence {
                        console.line(text = makeHelpTitle())
                        console.line(text = "")
                        console.line(text = makeNoCommandsLine())
                    }
                }
            }
        }

        given("справка с командой") {
            val task = Task(commands = mapOf("start" to command), console = console, debug = false)
            `when` {
                val code = task.run(args = emptyList())
                then {
                    code shouldBe 0
                    verifySequence {
                        console.line(text = makeHelpTitle())
                        console.line(text = "")
                        console.line(text = makeStartHelpLine())
                    }
                }
            }
        }

        given("неизвестная команда") {
            val task = Task(commands = mapOf("start" to command), console = console, debug = false)
            `when` {
                val code = task.run(args = listOf("nope"))
                then {
                    code shouldBe 2
                    verifySequence { console.error(text = makeUnknownCommandError()) }
                }
            }
        }

        given("команда получает аргументы без своего имени") {
            justRun { command.run(args = any(), console = console) }
            val task = Task(commands = mapOf("start" to command), console = console, debug = false)
            `when` {
                val code = task.run(args = listOf("start", "13", "task-scripts"))
                then {
                    code shouldBe 0
                    verifySequence { command.run(args = listOf("13", "task-scripts"), console = console) }
                }
            }
        }

        given("команда отказывает") {
            every { command.run(args = any(), console = console) } throws makeRefusal()
            val task = Task(commands = mapOf("start" to command), console = console, debug = false)
            `when` {
                val code = task.run(args = listOf("start", "13"))
                then {
                    code shouldBe 1
                    verifySequence { console.error(text = makeRefusalMessage()) }
                }
            }
        }

        given("команда падает с непредвиденной ошибкой") {
            every { command.run(args = any(), console = console) } throws IllegalStateException("нет ответа")
            val task = Task(commands = mapOf("start" to command), console = console, debug = false)
            `when` {
                val code = task.run(args = listOf("start", "13"))
                then {
                    code shouldBe 2
                    verifySequence { console.error(text = makeCrashMessage()) }
                }
            }
        }

        given("сбой с TASK_DEBUG=1") {
            every { command.run(args = any(), console = console) } throws makeFailure()
            val task = Task(commands = mapOf("start" to command), console = console, debug = true)
            `when` {
                val code = task.run(args = listOf("start", "13"))
                then {
                    code shouldBe 2
                    verifySequence {
                        console.error(text = makeFailureMessage())
                        console.error(text = match { text -> text.startsWith(prefix = makeFailureTraceStart()) })
                    }
                }
            }
        }
    })
