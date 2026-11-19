package io.github.nakulov.standards.setup

import java.io.File

fun makeWrapperSource(): String {
    return "#!/bin/bash\n# engineering-standards task-wrapper\nexec true\n"
}

/** Наша обёртка прошлой версии: метка есть, текст другой. */
fun makeOldWrapper(): String {
    return "#!/bin/bash\n# engineering-standards task-wrapper\nexec false\n"
}

/** Чужой `task` (например go-task): метки нет. */
fun makeForeignTask(): String {
    return "#!/bin/sh\necho go-task\n"
}

/** Папка плагина с источником обёртки `scripts/task-wrapper`. */
fun makeWrapperPlugin(): File {
    val plugin = makeProjectRoot()
    val source = File(plugin, "scripts/task-wrapper")
    source.parentFile.mkdirs()
    source.writeText(makeWrapperSource())
    return plugin
}

fun writeWrapper(
    home: File,
    text: String,
) {
    val file = File(home, ".local/bin/task")
    file.parentFile.mkdirs()
    file.writeText(text)
}

fun readWrapper(home: File): String {
    return File(home, ".local/bin/task").readText()
}

fun makeWrapperFinding(
    status: Finding.Status,
    text: String,
): Finding {
    return Finding(status = status, area = "машина", text = "~/.local/bin/task: $text")
}

fun makeInstalledText(): String {
    return "обёртка установлена — запись вне репозитория, одна на машину"
}

fun makeUpdatedText(): String {
    return "обёртка обновлена — запись вне репозитория, одна на машину"
}

fun makeMissingText(): String {
    return "обёртки нет — task из терминала не найдётся, поставит setup"
}

fun makeDifferentText(): String {
    return "обёртка отличается от стандарта — обновит setup"
}

fun makeForeignText(plugin: File): String {
    return "чужая программа, не трогаю — task из терминала: ${plugin.path}/scripts/task"
}

fun makePathNote(): Finding {
    val text = "~/.local/bin нет в PATH — добавьте в ~/.zshrc: export PATH=\"\$HOME/.local/bin:\$PATH\""
    return Finding(status = Finding.Status.NOTE, area = "машина", text = text)
}

/** `~/.local/bin` только для чтения: обёртку в ней не записать. */
fun makeReadOnlyBin(home: File) {
    val bin = File(home, ".local/bin")
    bin.mkdirs()
    bin.setWritable(false)
}
