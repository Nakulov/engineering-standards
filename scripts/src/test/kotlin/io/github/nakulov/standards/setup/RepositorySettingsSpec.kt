package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.standards.task.github.GitHub
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RepositorySettingsSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val gitHub = mockk<GitHub>()
        val settings = RepositorySettings(gitHub = gitHub)

        shouldReturnTest(listOf(makeSettingsOk())) {
            every { gitHub.read(args = makeSettingsArgs()) } returns makeStandardValues()
            settings.run(mode = makeCheckMode())
        }

        shouldReturnTest(makeSettingsDeviations()) {
            every { gitHub.read(args = makeSettingsArgs()) } returns makeCommitMessagesValues()
            settings.run(mode = makeCheckMode())
        }

        given("применение: тело squash-коммита отличается — заголовок уходит парой") {
            every { gitHub.read(args = makeSettingsArgs()) } returns makeCommitMessagesValues()
            every { gitHub.write(args = makeSettingsPatchArgs()) } returns ""
            `when` {
                val findings = settings.run(mode = makeApplyMode())
                then {
                    findings shouldBe makeSettingsFixes()
                    verify(exactly = 1) { gitHub.write(args = makeSettingsPatchArgs()) }
                }
            }
        }
    })
