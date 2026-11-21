// Плагин Kotlin — один на сборку: модули применяют его без своей версии.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}
