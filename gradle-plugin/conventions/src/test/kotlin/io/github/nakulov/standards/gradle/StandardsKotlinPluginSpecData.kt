package io.github.nakulov.standards.gradle

fun makeViolationFindings(): List<String> {
    return listOf(
        "e: /src/main/kotlin/sample/Names.kt:4:22 KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`). [NoImplicitIt]",
    )
}
