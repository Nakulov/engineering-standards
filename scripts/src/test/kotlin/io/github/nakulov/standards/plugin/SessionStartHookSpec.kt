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
        runHook(plugin, project, envFile, cache)
        runHook(plugin, project, envFile, cache)
        envFile.readText()
    }

    shouldReturnTest(makeRulesRun(plugin)) {
        runHook(plugin, project, envFile, cache)
    }

    shouldReturnTest(makeNoEnvFileRun(bare)) {
        runHook(bare, project, envFile = null, cache)
    }

    shouldReturnTest(makeRootRecord(plugin)) {
        runHook(plugin, project, envFile, cache)
        readRootRecord(cache, project)
    }

    shouldReturnTest(makeNoCacheRun(cache = blocked, plugin)) {
        runHook(plugin, project, envFile, cache = blocked)
    }

    shouldReturnTest(makeRootOnlyRun(bare)) {
        runHook(bare, project, envFile, cache)
    }
})
