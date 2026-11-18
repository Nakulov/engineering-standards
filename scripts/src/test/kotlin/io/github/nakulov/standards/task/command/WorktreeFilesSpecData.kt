package io.github.nakulov.standards.task.command

import java.io.File
import java.nio.file.Files

/** Основная папка с `local.properties` и `.idea/x.xml`; [include] — содержимое `.worktreeinclude`, `null` — файла нет. */
fun makeMainFolder(include: String?): File {
    val main = makeFolder(prefix = "main")
    File(main, "local.properties").writeText("sdk.dir=/sdk\n")
    File(main, ".idea").mkdirs()
    File(main, ".idea/x.xml").writeText("<x/>\n")
    if (include != null) {
        File(main, ".worktreeinclude").writeText(include)
    }
    return main
}

fun makeCopyFolder(): File {
    return makeFolder(prefix = "copy")
}

/** Временная папка в `build/` модуля: её убирает `./gradlew clean`. */
private fun makeFolder(prefix: String): File {
    val base = File(System.getProperty("task.cache"), "worktree-files")
    base.mkdirs()
    return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ prefix).toFile()
}
