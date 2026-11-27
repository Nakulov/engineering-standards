package io.github.nakulov.standards.plugin

import io.github.nakulov.standards.setup.makeProjectRoot
import io.github.nakulov.standards.setup.writeProjectFile
import io.github.nakulov.standards.task.shell.ProcessShell
import java.io.File

private const val TEMPLATE = "templates/project/ruff-standard.toml"
private const val CONFIG = "ruff-standard.toml"
private const val VERSION_PATTERN = "required-version = \"==(.+)\""

// Исключение KT-TEST-6: ruff проверяет настоящие файлы — образец [files] собирается во временной папке рядом с копией
// шаблона, как её кладёт setup, и ruff запускается процессом из этой папки: пути per-file-ignores считаются от неё.
// Версия ruff — из required-version шаблона: тест проверяет ту версию, которую стандарт требует от проектов.
fun findRuffOutput(files: Map<String, String>, args: List<String>): List<String> {
    val template = File(System.getProperty(/* key = */ "task.repoRoot"), TEMPLATE)
    val match = Regex(pattern = VERSION_PATTERN).find(input = template.readText())
        ?: error(message = "в $TEMPLATE нет required-version")
    val project = makeProjectRoot()
    template.copyTo(target = File(project, CONFIG))
    files.forEach { (path, text) -> writeProjectFile(project, path, text) }
    val command = listOf("uv", "tool", "run", "ruff@${match.groupValues[1]}") + args +
        listOf("--no-cache", "--config", CONFIG)
    val result = ProcessShell(directory = project).run(command)
    val lines = result.out
        .lines()
        .filter { line -> line.isNotBlank() }
    return listOf(result.code.toString()) + lines
}
