plugins {
    id("org.jetbrains.kotlin.jvm")
    id("standards.kotlin")
}

// Тестов в образце нет: покрытие 0, порог 100 — koverVerify падает.
standards {
    coverageMinBound = 100
}
