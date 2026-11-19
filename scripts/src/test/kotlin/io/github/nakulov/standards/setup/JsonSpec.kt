package io.github.nakulov.standards.setup

import io.github.nakulov.kotestkit.shouldReturnTest
import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
import io.github.nakulov.standards.task.cli.TaskError
import io.kotest.core.spec.style.BehaviorSpec

class JsonSpec :
    BehaviorSpec({
        shouldReturnTest(makeFormattedJson()) {
            JsonParser(text = makeFormattedJson(), file = "a.json").parse().toText()
        }

        shouldReturnTest(makeFormattedJson()) {
            JsonParser(text = makeCompactJson(), file = "a.json").parse().toText()
        }

        shouldReturnTest(makeParsedValue()) {
            JsonParser(text = "{\"b\": 1, \"a\": [true, null, -2.5e3]}", file = "a.json").parse()
        }

        shouldReturnTest(JsonString(value = "\"\\\n\tЖ/")) {
            JsonParser(text = "\"\\\"\\\\\\n\\t\\u0416\\/\"", file = "a.json").parse()
        }

        shouldReturnTest("\"\\u0001 \\\" \\\\ \\n\"") {
            JsonString(value = "\u0001 \" \\ \n").toText()
        }

        shouldReturnTest("{\n  \"a\": {},\n  \"b\": []\n}") {
            JsonParser(text = "{\"a\":{},\"b\":[]}", file = "a.json").parse().toText()
        }

        shouldThrowWithMessageTest<TaskError.Failure>("a.json:3: ожидается ключ в кавычках") {
            JsonParser(text = "{\n  \"a\": 1,\n  b\n}", file = "a.json").parse()
        }

        shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: комментарии JSON не поддерживает — уберите их") {
            JsonParser(text = "// настройки\n{}", file = "a.json").parse()
        }

        shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: строка без закрывающей кавычки") {
            JsonParser(text = "{\"a", file = "a.json").parse()
        }

        shouldThrowWithMessageTest<TaskError.Failure>("a.json:2: ожидается конец файла") {
            JsonParser(text = "{}\nx", file = "a.json").parse()
        }

        shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: ожидается «:» после ключа a") {
            JsonParser(text = "{\"a\" 1}", file = "a.json").parse()
        }

        shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: ожидается «,» или «]»") {
            JsonParser(text = "[1 2]", file = "a.json").parse()
        }
    })
