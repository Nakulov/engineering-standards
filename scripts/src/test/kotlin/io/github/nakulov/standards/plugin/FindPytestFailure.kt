package io.github.nakulov.standards.plugin

import io.github.nakulov.standards.setup.makeProjectRoot
import io.github.nakulov.standards.setup.writeProjectFile
import io.github.nakulov.standards.task.shell.ProcessShell
import java.io.File

private const val TEMPLATE = "templates/project/pytest.ini"
private const val CONFIG = "pytest.ini"
private const val PYTEST = "pytest==9.2.0"
private const val PYTEST_COV = "pytest-cov==7.1.0"
private const val FAILURE = "FAIL "

// Исключение KT-TEST-6: pytest запускается процессом в образце [files] с копией шаблона в корне, как её кладёт setup:
// pythonpath шаблона считается от папки pytest.ini. Версии — нижние границы шаблона (minversion, required_plugins),
// закреплённые: тест не зависит от новых выпусков; поднял границу в pytest.ini — подними и PYTEST, PYTEST_COV.
// Из вывода остаются код выхода и строка FAIL pytest-cov — остальное (платформа, время) зависит от машины.
fun findPytestFailure(files: Map<String, String>): List<String> {
    val project = makeProjectRoot()
    File(System.getProperty(/* key = */ "task.repoRoot"), TEMPLATE).copyTo(target = File(project, CONFIG))
    files.forEach { (path, text) -> writeProjectFile(project, path, text) }
    val command = listOf("uv", "tool", "run", "--from", PYTEST, "--with", PYTEST_COV) +
        listOf("pytest", "-p", "no:cacheprovider")
    val result = ProcessShell(directory = project).run(command)
    val failures = result.out
        .lines()
        .filter { line -> line.startsWith(prefix = FAILURE) }
    return listOf(result.code.toString()) + failures
}
