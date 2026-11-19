package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.standards.task.cli.LauncherRun
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File

// Вне стандарта: спека проверяет не класс, а bash-скрипт `scripts/task-wrapper` — запускает его копию процессом в
// разных папках (#43, решение 6). Подготовка репозиториев и запуск — в TaskWrapperScriptSpecData.
class TaskWrapperScriptSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val wrapper = makeWrapperCopy()
        val cache = makeTempDir(prefix = "cache")
        val plugin = makeStubPlugin()
        val project = makeGitProject()

        shouldReturnTest(LauncherRun(code = 0, out = "стандарт --help\n", err = "")) {
            runWrapper(wrapper = wrapper, directory = makeStandardRepo(), cache = cache)
        }

        shouldReturnTest(makePluginRun()) {
            writeRootRecord(cache = cache, project = project, root = plugin)
            runWrapper(wrapper = wrapper, directory = project, cache = cache)
        }

        shouldReturnTest(makeNoRecordRun()) {
            runWrapper(wrapper = wrapper, directory = project, cache = cache)
        }

        shouldReturnTest(makeNoRecordRun()) {
            writeRootRecord(cache = cache, project = project, root = File(plugin, "removed"))
            runWrapper(wrapper = wrapper, directory = project, cache = cache)
        }

        shouldReturnTest(makePluginRun()) {
            writeRootRecord(cache = cache, project = project, root = plugin)
            runWrapper(wrapper = wrapper, directory = makeWorktree(project = project), cache = cache)
        }

        shouldReturnTest(makeNotGitRun()) {
            runWrapper(wrapper = wrapper, directory = makeTempDir(prefix = "plain"), cache = cache)
        }
    })
