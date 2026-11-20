package io.github.nakulov.standards.task.command

import io.github.nakulov.kotestkit.case
import io.github.nakulov.kotestkit.then
import io.github.nakulov.kotestkit.`when`
import io.github.nakulov.kotestkit.withGivens
import io.github.nakulov.standards.task.board.Board
import io.github.nakulov.standards.task.github.Issues
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify

class GroomingSpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val issues = mockk<Issues>()
        val board = mockk<Board>()
        val grooming = Grooming(issues = issues, board = board)
        justRun { board.move(item = any(), status = any()) }
        every { issues.nodeId(number = 13) } returns "I_issue13"

        given("задача в Backlog") {
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeBacklogItem()
            `when` {
                val line = grooming.groom(number = 13)
                then {
                    line shouldBe "#13 → Groomed"
                    verify { board.move(item = makeBacklogItem(), status = Board.GROOMED) }
                }
            }
        }

        withGivens(
            nameFn = { given -> "задача в «${given.arg1.status}»" },
            case(arg1 = makeGroomedItem()),
            case(arg1 = makeInProgressItem()),
            case(arg1 = makeDoneItem().copy(status = "In review")),
            case(arg1 = makeDoneItem()),
        ) { item ->
            every { board.item(issue = 13, nodeId = "I_issue13") } returns item
            `when` {
                val line = grooming.groom(number = 13)
                then {
                    line shouldBe "#13 уже в «${item.status}»"
                    verify(exactly = 0) { board.move(item = any(), status = any()) }
                }
            }
        }
    })
