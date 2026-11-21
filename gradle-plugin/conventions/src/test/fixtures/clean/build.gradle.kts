plugins {
    id("org.jetbrains.kotlin.jvm")
    id("standards.kotlin")
}

// Чистый код без находок, порог 0: check проходит — плагин не роняет проект без нарушений.
standards {
    coverageMinBound = 0
}
