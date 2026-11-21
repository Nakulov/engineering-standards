package io.github.nakulov.standards.gradle

import org.gradle.api.provider.Property

/** Настройки проекта для `standards.kotlin`: `standards { coverageMinBound = 85 }`. */
abstract class StandardsExtension {

    /** Порог покрытия строк тестами, %: замер проекта с округлением вниз, не снижается (KT-TEST-7). */
    abstract val coverageMinBound: Property<Int>
}
