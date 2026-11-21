plugins {
    id("org.jetbrains.kotlin.jvm")
    id("standards.kotlin")
}

// Тестов в образце нет: порог 0, чтобы check падал только на находке detekt.
standards {
    coverageMinBound = 0
}
