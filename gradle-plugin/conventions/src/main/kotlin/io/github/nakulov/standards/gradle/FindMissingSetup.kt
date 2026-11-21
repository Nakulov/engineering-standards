package io.github.nakulov.standards.gradle

private const val PLUGIN_ID = "standards.kotlin"
private const val NO_KOTLIN = "$PLUGIN_ID требует kotlin(\"jvm\"): примените его в plugins рядом со $PLUGIN_ID."
private const val NO_BOUND = "$PLUGIN_ID: задайте порог покрытия — " +
    "standards { coverageMinBound = <замер, %> } (KT-TEST-7)."

internal fun findMissingSetup(hasKotlin: Boolean, coverageMinBound: Int?): String? {
    if (!hasKotlin) return NO_KOTLIN
    if (coverageMinBound == null) return NO_BOUND
    return null
}
