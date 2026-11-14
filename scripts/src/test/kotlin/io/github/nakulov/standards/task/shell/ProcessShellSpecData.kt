package io.github.nakulov.standards.task.shell

import java.io.File

fun makeWorkDirectory(): File {
    return File(System.getProperty("java.io.tmpdir"))
}

fun makeMissingToolMessage(): String {
    return "не найдена команда no-such-tool — установите: brew install no-such-tool"
}
