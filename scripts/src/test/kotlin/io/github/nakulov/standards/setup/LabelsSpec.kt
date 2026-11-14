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

class LabelsSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val labels = Labels(gitHub = gitHub, standard = makeStandardLabels())
        every { gitHub.read(args = makeUsageArgs(encoded = "bug")) } returns "0"
        every { gitHub.read(args = makeUsageArgs(encoded = "good%20first%20issue")) } returns "1"
        every { gitHub.write(args = any()) } returns ""

        shouldReturnTest(makeStandardLabels()) {
            Labels.parse(text = makeLabelsText(), file = "labels.yml")
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeBadLineMessage()) {
            Labels.parse(text = "color: 0E8A16", file = "labels.yml")
        }

        shouldThrowWithMessageTest<TaskError.Failure>("labels.yml: у метки type:task нужны name, color и description") {
            Labels.parse(text = "- name: type:task\n  color: 0E8A16", file = "labels.yml")
        }

        shouldThrowWithMessageTest<TaskError.Failure>(makeUnquotedMessage()) {
            Labels.parse(text = makeUnquotedText(), file = "labels.yml")
        }

        shouldReturnTest(makeCheckFindings()) {
            every { gitHub.listRest(path = makeLabelsPath(), jq = makeLabelsJq()) } returns makeExistingRows()
            labels.run(mode = makeCheckMode())
        }

        given("применение без --confirm") {
            every { gitHub.listRest(path = makeLabelsPath(), jq = makeLabelsJq()) } returns makeDriftRows()
            `when` {
                val findings = labels.run(mode = makeApplyMode())
                then {
                    findings shouldBe makeApplyFindings()
                    verify(exactly = 1) { gitHub.write(args = makeCreateBugArgs()) }
                    verify(exactly = 1) { gitHub.write(args = makePatchTaskArgs()) }
                    verify(exactly = 1) { gitHub.write(args = makeDeleteArgs(encoded = "bug")) }
                    verify(exactly = 0) { gitHub.write(args = makeDeleteArgs(encoded = "good%20first%20issue")) }
                }
            }
        }

        given("применение с --confirm: метка с задачами удаляется") {
            every { gitHub.listRest(path = makeLabelsPath(), jq = makeLabelsJq()) } returns makeExistingRows()
            `when` {
                val findings = labels.run(mode = makeConfirmedMode())
                then {
                    findings shouldBe makeConfirmedFindings()
                    verify(exactly = 1) { gitHub.write(args = makeDeleteArgs(encoded = "good%20first%20issue")) }
                }
            }
        }
    })
