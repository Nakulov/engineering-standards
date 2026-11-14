package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
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

class RulesetsSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val rulesets = Rulesets(gitHub = gitHub, scratch = makeScratch())
        every { gitHub.listRest(path = makeRulesetsPath(), jq = makeRulesetsJq()) } returns emptyList()
        every { gitHub.read(args = makeCheckRunsArgs()) } returns "0"
        every { gitHub.read(args = makeWorkflowsArgs()) } returns "0"
        every { gitHub.read(args = makeClassicArgs()) } throws TaskError.Failure(message = makeNotProtected())

        shouldReturnTest(listOf(makeUnavailable())) {
            every { gitHub.listRest(path = makeRulesetsPath(), jq = makeRulesetsJq()) } throws makeUpgradeFailure()
            rulesets.run(mode = makeCheckMode())
        }

        shouldReturnTest(makeEmptyRepositoryCheck()) {
            rulesets.run(mode = makeCheckMode())
        }

        shouldReturnTest(makeForeignCheck()) {
            every { gitHub.listRest(path = makeRulesetsPath(), jq = makeRulesetsJq()) } returns makeForeignRows()
            every { gitHub.read(args = makeWorkflowsArgs()) } returns "2"
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "1") })
            } returns makeDevLines(threads = "true")
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "2") })
            } returns makeLongLines()
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "3") })
            } returns makeTagsLines()
            rulesets.run(mode = makeCheckMode())
        }

        shouldReturnTest(makeDevDiffers()) {
            every { gitHub.listRest(path = makeRulesetsPath(), jq = makeRulesetsJq()) } returns makeStandardRows()
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "1") })
            } returns makeDevLines(threads = "false")
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "2") })
            } returns makeLongLines()
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "3") })
            } returns makeTagsLines()
            rulesets.run(mode = makeCheckMode()).take(n = 2)
        }

        given("применение: прогон check есть, ruleset'ов нет, классическая защита есть") {
            val bodies = mutableListOf<String>()
            every { gitHub.read(args = makeCheckRunsArgs()) } returns "3"
            every { gitHub.read(args = makeClassicArgs()) } returns "https://api.github.com/…/protection"
            every { gitHub.write(args = any()) } answers {
                val args = firstArg<List<String>>()
                bodies += File(args.last()).readText()
                ""
            }
            `when` {
                val findings = rulesets.run(mode = makeApplyMode())
                then {
                    findings shouldBe makeCreatedFindings()
                    bodies.first() shouldBe makeDevJson()
                    verify(exactly = 3) { gitHub.write(args = match { args -> args[2] == "POST" }) }
                    makeScratch().listFiles().orEmpty().toList() shouldBe emptyList()
                }
            }
        }

        given("применение с --confirm: классическая защита удаляется") {
            every { gitHub.listRest(path = makeRulesetsPath(), jq = makeRulesetsJq()) } returns makeStandardRows()
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "1") })
            } returns makeDevLines(threads = "true")
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "2") })
            } returns makeLongLines()
            every {
                gitHub.read(args = match { args -> args.take(n = 3) == makeRulesetPrefix(id = "3") })
            } returns makeTagsLines()
            every { gitHub.read(args = makeClassicArgs()) } returns "https://api.github.com/…/protection"
            every { gitHub.write(args = makeDeleteClassicArgs()) } returns ""
            `when` {
                val findings = rulesets.run(mode = makeConfirmedMode())
                then {
                    findings.last() shouldBe makeClassicDeleted()
                    verify(exactly = 1) { gitHub.write(args = makeDeleteClassicArgs()) }
                }
            }
        }
    })
