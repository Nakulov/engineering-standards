package io.github.nakulov.standards.task.github

import io.github.nakulov.standards.task.cli.TaskError
import io.github.nakulov.standards.task.shell.Shell

/**
 * Вызовы `gh` (#13, решение 5). Чтение при временном сбое (`5xx`, сеть) повторяется один раз, запись — нет: она могла
 * пройти. Списки читаются только [listRest] и [listGraphql] — целиком, постранично: доска бывает больше 100 элементов
 * (#165 в ui-design-system), а у `gh … list` по умолчанию 30.
 */
class GitHub(
    private val shell: Shell,
    private val pause: (Long) -> Unit,
) {
    fun read(args: List<String>): String {
        val first = shell.run(command = listOf("gh") + args)
        val result = if (first.code != 0 && isTransient(err = first.err)) {
            pause(RETRY_PAUSE_MS)
            shell.run(command = listOf("gh") + args)
        } else {
            first
        }
        return output(code = result.code, out = result.out, err = result.err)
    }

    fun write(args: List<String>): String {
        val result = shell.run(command = listOf("gh") + args)
        return output(code = result.code, out = result.out, err = result.err)
    }

    /** Строки `--jq` по всем страницам REST-списка [path]. */
    fun listRest(
        path: String,
        jq: String,
    ): List<String> {
        val separator = if ("?" in path) "&" else "?"
        return lines(text = read(args = listOf("api", "--paginate", "$path${separator}per_page=100", "--jq", jq)))
    }

    /**
     * Строки `--jq` по всем страницам GraphQL-списка. [query] обязан принимать `$endCursor` и запрашивать `pageInfo`:
     * иначе `--paginate` прочитает только первую страницу.
     */
    fun listGraphql(
        query: String,
        jq: String,
        variables: Map<String, String>,
    ): List<String> {
        require("\$endCursor" in query && "pageInfo" in query) {
            "GraphQL-список без \$endCursor и pageInfo читает только первую страницу"
        }
        val fields = variables.flatMap { (name, value) -> listOf("-F", "$name=$value") }
        val args = listOf("api", "graphql", "--paginate", "-f", "query=$query") + fields + listOf("--jq", jq)
        return lines(text = read(args = args))
    }

    private fun output(
        code: Int,
        out: String,
        err: String,
    ): String {
        if (code != 0) {
            val hint = if (isTransient(err = err)) " — повторите позже" else ""
            throw TaskError.Failure(message = "gh: ${err.trim().lineSequence().first()}$hint")
        }
        return out.trim()
    }

    private fun isTransient(err: String): Boolean {
        return TRANSIENT.containsMatchIn(input = err)
    }

    private fun lines(text: String): List<String> {
        return text.lines().filter { line -> line.isNotBlank() }
    }

    private companion object {
        const val RETRY_PAUSE_MS = 1000L
        val TRANSIENT = Regex(
            pattern = "HTTP 5\\d\\d|timeout|timed out|connection reset|unexpected EOF|TLS handshake",
            option = RegexOption.IGNORE_CASE,
        )
    }
}
