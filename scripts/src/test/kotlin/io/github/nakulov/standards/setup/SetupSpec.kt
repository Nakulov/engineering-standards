package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.Console
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence

class SetupSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val environment = mockk<Environment>()
        val first = mockk<Setup.Step>()
        val second = mockk<Setup.Step>()
        val console = mockk<Console>()
        justRun { console.line(text = any()) }
        justRun { console.error(text = any()) }
        justRun { environment.verify() }
        val setup = Setup(environment = environment, steps = listOf(first, second), console = console, debug = false)

        given("справка") {
            `when` {
                val code = setup.run(args = listOf("--help"))
                then {
                    code shouldBe 0
                    verify(exactly = 0) { environment.verify() }
                    verify(exactly = 5) { console.line(text = any()) }
                }
            }
        }

        given("неизвестный аргумент") {
            `when` {
                val code = setup.run(args = listOf("--force"))
                then {
                    code shouldBe 2
                    verifySequence { console.error(text = makeUsageError()) }
                }
            }
        }

        given("проверка: всё соответствует или не исправляется запуском") {
            every { first.run(mode = makeCheckMode()) } returns listOf(makeOkFinding())
            every { second.run(mode = makeCheckMode()) } returns listOf(makeWaitingFinding(), makeUnavailableFinding())
            `when` {
                val code = setup.run(args = listOf("--check"))
                then {
                    code shouldBe 0
                    verifySequence {
                        environment.verify()
                        first.run(mode = makeCheckMode())
                        console.line(text = "[ok] настройки: соответствуют стандарту")
                        second.run(mode = makeCheckMode())
                        console.line(text = "[ждёт CI] защита: обязательной проверки нет: CI не настроен")
                        console.line(text = "[недоступно] защита: приватный репозиторий")
                    }
                }
            }
        }

        given("применение: остался шаг, которому нужен --confirm") {
            every { first.run(mode = makeApplyMode()) } returns listOf(makeFixedFinding())
            every { second.run(mode = makeApplyMode()) } returns listOf(makeNeedsConfirmFinding())
            `when` {
                val code = setup.run(args = emptyList())
                then {
                    code shouldBe 1
                }
            }
        }

        given("применение с --confirm") {
            every { first.run(mode = makeConfirmedMode()) } returns listOf(makeFixedFinding())
            every { second.run(mode = makeConfirmedMode()) } returns listOf(makeFixedFinding())
            `when` {
                val code = setup.run(args = listOf("--confirm"))
                then {
                    code shouldBe 0
                }
            }
        }

        given("окружение не готово") {
            every { environment.verify() } throws makeScopeRefusal()
            `when` {
                val code = setup.run(args = listOf("--check"))
                then {
                    code shouldBe 1
                    verify(exactly = 0) { first.run(mode = any()) }
                    verifySequence { console.error(text = makeScopeRefusal().message.orEmpty()) }
                }
            }
        }

        given("непредвиденная ошибка шага") {
            every { first.run(mode = makeCheckMode()) } throws IllegalStateException("нет ответа")
            `when` {
                val code = setup.run(args = listOf("--check"))
                then {
                    code shouldBe 2
                    verifySequence { console.error(text = makeCrashMessage()) }
                }
            }
        }
    })
