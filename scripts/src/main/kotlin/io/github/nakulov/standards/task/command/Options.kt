package io.github.nakulov.standards.task.command

import io.github.nakulov.standards.task.cli.TaskError
import java.io.File

/**
 * Разбор аргументов вида `--ключ значение` и чтение файла с текстом от ассистента.
 */
object Options {
    /** Ключи из [allowed], каждый не больше раза; [required] обязательны. Иначе — ошибка с [usage]. */
    fun parse(
        args: List<String>,
        allowed: Set<String>,
        required: Set<String>,
        usage: String,
    ): Map<String, String> {
        val pairs = args.chunked(size = 2)
        val keys = pairs.map { pair -> pair.first() }
        val valid = pairs.all { pair -> pair.size == 2 && pair.first() in allowed } &&
            keys.size == keys.toSet().size && keys.containsAll(elements = required)
        if (!valid) {
            throw TaskError.Failure(message = "использование: $usage")
        }
        return pairs.associate { pair -> pair.first() to pair.last() }
    }

    fun readFile(
        path: String,
        what: String,
    ): String {
        val file = File(path)
        if (!file.isFile) {
            throw TaskError.Failure(message = "нет файла $path — сначала запишите $what")
        }
        return file.readText()
    }
}
