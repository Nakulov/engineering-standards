package sample

internal fun trimmed(names: List<String>): List<String> {
    return names.map { it.trim() }
}
