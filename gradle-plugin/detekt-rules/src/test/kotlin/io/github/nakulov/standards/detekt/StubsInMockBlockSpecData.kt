package io.github.nakulov.standards.detekt

const val TOP_LEVEL_STUB_MESSAGE =
    "KT-KIT-13. Заглушка верхнего уровня спеки — в блоке при создании мока `mockk<T> { … }`."

const val TOP_LEVEL_EVERY_CODE = """
class ExampleSpec : BehaviorSpec({
    val repository = mockk<Repository>()
    every { repository.root() } returns "/first" andThen "/second"
})
"""

const val TOP_LEVEL_JUST_RUN_CODE = """
class ExampleSpec : BehaviorSpec({
    val console = mockk<Console>()
    justRun { console.line(text = any()) }
})
"""

const val TOP_LEVEL_CO_EVERY_CODE = """
class ExampleSpec : BehaviorSpec({
    val repository = mockk<Repository>()
    coEvery { repository.load() } coAnswers { "loaded" }
})
"""

const val FACTORY_MOCK_STUB_CODE = """
class ExampleSpec : BehaviorSpec({
    val repository = makeRepository()
    every { repository.root() } returns "/repo"
})
"""

const val MOCK_BLOCK_STUBS_CODE = """
class ExampleSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<Repository> {
        every { root() } returns "/repo"
        justRun { save(any()) }
    }
})
"""

const val NESTED_STUBS_CODE = """
class ExampleSpec : BehaviorSpec({
    val repository = mockk<Repository>()

    given("репозиторий") {
        every { repository.root() } returns "/repo"
    }

    withGivens(
        nameFn = { case -> case.arg1 },
        case(arg1 = "/repo"),
    ) { root ->
        coEvery { repository.load() } returns root
    }
})
"""

const val TOP_LEVEL_VERIFY_CODE = """
class ExampleSpec : BehaviorSpec({
    val repository = mockk<Repository>()
    verifySequence { repository.root() }
})
"""

const val FUN_SPEC_STUB_CODE = """
class ExampleSpec : FunSpec({
    val repository = mockk<Repository>()
    every { repository.root() } returns "/repo"
})
"""

const val NO_LAMBDA_SPEC_CODE = """
class ExampleSpec : BehaviorSpec()
"""

fun makeTopLevelStubFindings(): List<String> {
    return listOf(
        "4:5 $TOP_LEVEL_STUB_MESSAGE",
    )
}
