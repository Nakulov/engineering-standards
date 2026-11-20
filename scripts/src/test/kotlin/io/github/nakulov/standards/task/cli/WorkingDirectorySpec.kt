package io.github.nakulov.standards.task.cli

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import java.io.File

class WorkingDirectorySpec :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerRoot

        val current = makeCurrentDirectory()
        val directory = WorkingDirectory(current = current)

        shouldReturnTest(WorkingDirectory.Invocation(directory = current, args = makeCommitArgs())) {
            directory.split(args = makeCommitArgs())
        }

        shouldReturnTest(
            WorkingDirectory.Invocation(directory = makeCopyDirectory(current = current), args = makeCommitArgs()),
        ) {
            directory.split(args = listOf("-C", makeCopyDirectory(current = current).path) + makeCommitArgs())
        }

        shouldReturnTest(WorkingDirectory.Invocation(directory = File(current, "copy"), args = makeCommitArgs())) {
            makeCopyDirectory(current = current)
            directory.split(args = listOf("-C", "copy") + makeCommitArgs())
        }

        shouldReturnTest(WorkingDirectory.Invocation(directory = current, args = listOf("commit", "-C", "copy"))) {
            directory.split(args = listOf("commit", "-C", "copy"))
        }

        shouldThrowWithMessageTest<TaskError.Failure>("-C без папки — task -C <папка> <команда>") {
            directory.split(args = listOf("-C"))
        }

        shouldThrowWithMessageTest<TaskError.Failure>("-C missing: нет такой папки — проверьте путь") {
            directory.split(args = listOf("-C", "missing") + makeCommitArgs())
        }
    })
