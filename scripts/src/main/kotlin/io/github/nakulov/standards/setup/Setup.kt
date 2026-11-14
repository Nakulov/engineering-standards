package io.github.nakulov.standards.setup

import io.github.nakulov.standards.task.cli.Console
import io.github.nakulov.standards.task.cli.TaskError

/**
 * `setup`: приводит репозиторий текущей папки к стандарту или только сверяет (`--check`). Одна команда для нового и
 * существующего репозитория: шаг применяет только недостающее, повторный запуск ничего не меняет. Удаляющие шаги
 * выполняются только с `--confirm` (#8, решения 1–4). Код выхода: 0 — соответствует, 1 — отклонения или нужен
 * `--confirm`, 2 — сбой.
 */
class Setup(
    private val environment: Environment,
    private val steps: List<Step>,
    private val console: Console,
    private val debug: Boolean,
) {
    /** Шаг стандарта: сверяет свою часть репозитория и, если [Mode.apply], исправляет. */
    fun interface Step {
        fun run(mode: Mode): List<Finding>
    }

    /** [apply] — исправлять, иначе только сверять; [confirmed] — разрешены удаляющие шаги. */
    data class Mode(
        val apply: Boolean,
        val confirmed: Boolean,
    )

    fun run(args: List<String>): Int {
        val code = try {
            execute(args = args)
        } catch (error: TaskError) {
            report(message = error.message.orEmpty(), error = error)
            error.exitCode
        } catch (@Suppress("TooGenericExceptionCaught") error: RuntimeException) {
            report(message = "сбой скрипта: $error — подробности: TASK_DEBUG=1", error = error)
            2
        }
        return code
    }

    private fun execute(args: List<String>): Int {
        if (args == listOf(HELP)) {
            USAGE.forEach { line -> console.line(text = line) }
            return 0
        }
        val mode = MODES[args] ?: throw TaskError.Failure(message = "использование: setup [--check | --confirm]")
        environment.verify()
        var failing = false
        steps.forEach { step ->
            step.run(mode = mode).forEach { finding ->
                console.line(text = finding.line())
                failing = failing || finding.status.failing
            }
        }
        return if (failing) 1 else 0
    }

    private fun report(
        message: String,
        error: Exception,
    ) {
        console.error(text = message)
        if (debug) {
            console.error(text = error.stackTraceToString())
        }
    }

    private companion object {
        const val HELP = "--help"
        val MODES = mapOf(
            emptyList<String>() to Mode(apply = true, confirmed = false),
            listOf("--check") to Mode(apply = false, confirmed = false),
            listOf("--confirm") to Mode(apply = true, confirmed = true),
        )
        val USAGE = listOf(
            "setup — настройка репозитория текущей папки по стандарту. Запуск:",
            "",
            "  setup — применить недостающее; удаляющие шаги только печатаются",
            "  setup --confirm — применить, включая удаляющие шаги",
            "  setup --check — только сверить: 0 — соответствует, 1 — отклонения",
        )
    }
}
