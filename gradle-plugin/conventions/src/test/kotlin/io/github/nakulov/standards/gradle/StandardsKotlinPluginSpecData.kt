package io.github.nakulov.standards.gradle

fun makeLowCoverageOutput(): List<String> {
    return listOf(
        "> Rule violated: lines covered percentage is 0.000000, but expected minimum is 100",
    )
}

fun makeNoBoundOutput(): List<String> {
    return listOf(
        "> standards.kotlin: задайте порог покрытия — standards { coverageMinBound = <замер, %> } (KT-TEST-7).",
    )
}

fun makeViolationFindings(): List<String> {
    return listOf(
        "e: /src/main/kotlin/sample/Names.kt:4:22 KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`). [NoImplicitIt]",
    )
}
