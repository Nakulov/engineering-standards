package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.standards.task.cli.LauncherRun
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а bash-хук `hooks/session-start` — запускает его копию процессом во
// временной папке плагина (#43, решение 9). Подготовка папок и запуск — в SessionStartHookSpecData.
class SessionStartHookSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val plugin = makeHookPlugin(rules = mapOf("b.md" to "Правило Б\n", "a.md" to "Правило А\n"))
        val bare = makeHookPlugin(rules = emptyMap())
        val project = makeGitProject()
        val envFile = makeEnvFile()
        val cache = makeTempDir(prefix = "cache")

        shouldReturnTest(makePathLine(plugin = plugin)) {
            runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
            runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
            envFile.readText()
        }

        shouldReturnTest(makeRulesRun(plugin = plugin)) {
            runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
        }

        shouldReturnTest(makeNoEnvFileRun(plugin = bare)) {
            runHook(plugin = bare, project = project, envFile = null, cache = cache)
        }

        shouldReturnTest(plugin.path + "\n") {
            runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
            readRootRecord(cache = cache, project = project)
        }

        val blocked = makeBlockedCache()
        shouldReturnTest(makeNoCacheRun(cache = blocked, plugin = plugin)) {
            runHook(plugin = plugin, project = project, envFile = envFile, cache = blocked)
        }

        shouldReturnTest(LauncherRun(code = 0, out = makeRootLine(plugin = bare), err = "")) {
            runHook(plugin = bare, project = project, envFile = envFile, cache = cache)
        }
    })
