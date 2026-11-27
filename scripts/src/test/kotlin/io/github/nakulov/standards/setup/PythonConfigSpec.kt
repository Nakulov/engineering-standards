package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.TestOptions
import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.git.Git
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk

class PythonConfigSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val git = mockk<Git>()
    val root = makeProjectRoot()
    every { git.root() } returns root.path
    every { git.trackedPaths(PYTHON_PATTERN) } returns makePythonPaths()
    val step = PythonConfig(git, makePythonPluginRoot())

    withGivens(
        nameFn = { case -> case.arg1 },
        case(
            arg1 = "нет файлов Python — находка",
            arg2 = emptyList<String>(),
            arg3 = makeCheckMode(),
            arg4 = listOf(makeNoPythonFinding()),
        ),
        case(
            arg1 = "проект без конфигов Python",
            arg2 = makePythonPaths(),
            arg3 = makeCheckMode(),
            arg4 = makeMissingPythonFindings(),
        ),
        case(
            arg1 = "setup пишет копии и заготовки",
            arg2 = makePythonPaths(),
            arg3 = makeApplyMode(),
            arg4 = makeWrittenPythonFindings(),
        ),
    ) { _, paths, mode, expected ->
        shouldReturnTest(expected) {
            every { git.trackedPaths(PYTHON_PATTERN) } returns paths
            step.run(mode)
        }
    }

    withGivens(
        nameFn = { case -> case.arg1 },
        case(
            arg1 = "нет файлов Python — файлы не пишутся",
            arg2 = emptyList<String>(),
            arg3 = emptyMap<String, String>(),
        ),
        case(
            arg1 = "содержимое копий и заготовок",
            arg2 = makePythonPaths(),
            arg3 = makePythonFiles(),
        ),
    ) { _, paths, expected ->
        shouldReturnTest(expected) {
            every { git.trackedPaths(PYTHON_PATTERN) } returns paths
            step.run(makeApplyMode())
            readProjectFiles(root)
        }
    }

    shouldReturnTest(makePythonFilesWithProjectRuff(), options = TestOptions(name = "ruff.toml проекта не трогается")) {
        writeProjectFile(root, path = "ruff.toml", text = PROJECT_RUFF)
        step.run(makeApplyMode())
        readProjectFiles(root)
    }

    shouldReturnTest(listOf(makePythonOk()), options = TestOptions(name = "повторный запуск — по стандарту")) {
        step.run(makeApplyMode())
        step.run(makeCheckMode())
    }

    withGivens(
        nameFn = { case -> "${case.arg1}: ${case.arg4}" },
        case(
            arg1 = "ruff-standard.toml",
            arg2 = "старый стандарт\n",
            arg3 = "ruff-standard.toml: отличается от стандарта",
            arg4 = "старый стандарт",
        ),
        case(
            arg1 = "ruff.toml",
            arg2 = "line-length = 100\n",
            arg3 = "ruff.toml: нет строки extend = \"ruff-standard.toml\" — стандарт не действует",
            arg4 = "без extend",
        ),
        case(
            arg1 = ".ruff.toml",
            arg2 = "line-length = 100\n",
            arg3 = ".ruff.toml: ruff читает его вместо ruff.toml — перенесите настройки в ruff.toml",
            arg4 = "рядом с ruff.toml",
        ),
        case(
            arg1 = "pyproject.toml",
            arg2 = "[tool.ruff]\nline-length = 100\n",
            arg3 = "pyproject.toml: [tool.ruff] не действует рядом с ruff.toml — перенесите настройки в ruff.toml",
            arg4 = "раздел [tool.ruff]",
        ),
        case(
            arg1 = "pyproject.toml",
            arg2 = "[tool.pytest.ini_options]\naddopts = \"-x\"\n",
            arg3 = "pyproject.toml: [tool.pytest] не действует рядом с pytest.ini — " +
                "настройки pytest задаёт стандарт, уберите раздел",
            arg4 = "раздел [tool.pytest]",
        ),
        case(
            arg1 = "pyproject.toml",
            arg2 = "[tool.coverage.report]\nfail_under = 90\n",
            arg3 = "pyproject.toml: [tool.coverage] не действует рядом с .coveragerc — " +
                "перенесите настройки в .coveragerc",
            arg4 = "раздел [tool.coverage]",
        ),
    ) { path, text, expected, _ ->
        shouldReturnTest(listOf(makePythonDeviation(expected))) {
            step.run(makeApplyMode())
            writeProjectFile(root, path, text)
            step.run(makeCheckMode())
        }
    }
})
