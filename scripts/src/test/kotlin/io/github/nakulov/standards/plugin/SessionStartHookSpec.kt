package io.github.nakulov.standards.plugin

import io.github.nakulov.kotestkit.shouldReturnTest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec

// Вне стандарта: спека проверяет не класс, а bash-хук `hooks/session-start` — запускает его копию процессом во
// временной папке плагина (#43, решение 9). Подготовка папок и запуск — в SessionStartHookSpecData.
class SessionStartHookSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val plugin = makeHookPlugin(rules = mapOf("b.md" to "Правило Б\n", "a.md" to "Правило А\n"))
    val bare = makeHookPlugin(rules = emptyMap())
    val project = makeGitProject()
    val envFile = makeEnvFile()
    val cache = makeTempDir(prefix = "cache")
    val blocked = makeBlockedCache()

    shouldReturnTest(makePathLine(plugin)) {
        runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
        runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
        envFile.readText()
    }

    shouldReturnTest(makeRulesRun(plugin)) {
        runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
    }

    shouldReturnTest(makeNoEnvFileRun(bare)) {
        runHook(plugin = bare, project = project, envFile = null, cache = cache)
    }

    shouldReturnTest(makeRootRecord(plugin)) {
        runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
        readRootRecord(cache, project)
    }

    shouldReturnTest(makeNoCacheRun(cache = blocked, plugin)) {
        runHook(plugin = plugin, project = project, envFile = envFile, cache = blocked)
    }

    shouldReturnTest(makeRootOnlyRun(bare)) {
        runHook(plugin = bare, project = project, envFile = envFile, cache = cache)
    }
})
