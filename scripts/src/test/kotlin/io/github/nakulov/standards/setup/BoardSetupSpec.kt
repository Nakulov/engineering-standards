package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File

class BoardSetupSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val step = BoardSetup(gitHub = gitHub, template = makeTemplate(), scratch = makeBoardScratch())
        every { gitHub.read(args = match { args -> args.last() == makeRepositoryJq() }) } returns "R_repo\tU_owner\tapp"
        every { gitHub.read(args = match { args -> args.last() == makeTemplateJq() }) } returns makeTemplateRow()
        every {
            gitHub.listGraphql(query = any(), jq = any(), variables = makeRepositoryVariables())
        } returns emptyList()

        shouldReturnTest(makeTemplate()) {
            BoardSetup.parse(text = "# доска-шаблон\nowner: owner\nnumber: 12\n", file = "board.yml")
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeTemplateError()) {
            BoardSetup.parse(text = "owner: owner\n", file = "board.yml")
        }

        shouldReturnTest(listOf(makeNoBoardFinding())) {
            step.run(mode = makeCheckMode())
        }

        given("нет доски: копия шаблона и связь с репозиторием") {
            every { gitHub.write(args = match { args -> args.last() == makeCopyJq() }) } returns makeCopyRow()
            every { gitHub.write(args = match { args -> "repository=R_repo" in args }) } returns ""
            `when` {
                val findings = step.run(mode = makeApplyMode())
                then {
                    findings shouldBe listOf(makeCreatedFinding())
                    verify(exactly = 1) {
                        gitHub.write(args = match { args -> args.containsAll(elements = makeCopyFields()) })
                    }
                    verify(exactly = 1) {
                        gitHub.write(args = match { args -> args.containsAll(elements = makeLinkFields()) })
                    }
                }
            }
        }

        shouldReturnTest(listOf(makeBoardFinding(status = Finding.Status.OK, text = "доска ${makeBoardUrl()}"))) {
            every {
                gitHub.listGraphql(query = any(), jq = any(), variables = makeRepositoryVariables())
            } returns listOf(makeStandardBoardRow())
            step.run(mode = makeCheckMode())
        }

        given("доска без статуса, с лишним статусом и выключенным workflow") {
            val bodies = mutableListOf<String>()
            every {
                gitHub.listGraphql(query = any(), jq = any(), variables = makeRepositoryVariables())
            } returns listOf(makeDriftBoardRow())
            every { gitHub.read(args = match { args -> args.last() == makeOptionsJq() }) } returns makeOptionRows()
            every { gitHub.write(args = match { args -> "--input" in args }) } answers {
                bodies += File(firstArg<List<String>>().last()).readText()
                ""
            }
            `when` {
                val findings = step.run(mode = makeApplyMode())
                then {
                    findings shouldBe makeDriftFindings()
                    bodies shouldBe listOf(makeStatusesBody())
                }
            }
        }

        shouldReturnTest(listOf(makeTwoBoardsFinding())) {
            every {
                gitHub.listGraphql(query = any(), jq = any(), variables = makeRepositoryVariables())
            } returns listOf(makeStandardBoardRow(), makeStandardBoardRow())
            step.run(mode = makeApplyMode())
        }
    })
