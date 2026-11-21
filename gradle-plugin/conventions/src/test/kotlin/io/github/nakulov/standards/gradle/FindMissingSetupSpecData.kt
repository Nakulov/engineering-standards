package io.github.nakulov.standards.gradle

const val COVERAGE_MIN_BOUND = 80
const val NO_KOTLIN_ERROR = "standards.kotlin требует kotlin(\"jvm\"): " +
    "примените его в plugins рядом со standards.kotlin."
const val NO_BOUND_ERROR = "standards.kotlin: задайте порог покрытия — " +
    "standards { coverageMinBound = <замер, %> } (KT-TEST-7)."
