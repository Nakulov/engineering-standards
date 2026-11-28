package io.github.nakulov.standards.task.cli

import io.github.nakulov.standards.plugin.makeTempDir
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

const val UNKNOWN_COMMAND = "nope"
const val DOWNLOAD_MISS = "task: готовый jar не скачан — собираю локально; нет доступа к GitHub Packages — " +
    "gh auth refresh -s read:packages\n"
const val UNKNOWN_COMMAND_ERROR = "неизвестная команда nope — см. task --help\n"
const val CACHE_PREFIX = "cache"

data class LauncherRun(
    val code: Int,
    val out: String,
    val err: String,
)

fun makeUnknownCommandRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = UNKNOWN_COMMAND_ERROR)
}

fun makeLocalBuildRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = DOWNLOAD_MISS + UNKNOWN_COMMAND_ERROR)
}

fun makeFailedBuildRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = DOWNLOAD_MISS + "сбой сборки task — подробности выше\n")
}

fun makeSetupHelpRun(): LauncherRun {
    return LauncherRun(
        code = 0,
        out = "setup — настройка репозитория текущей папки по стандарту. Запуск:\n\n" +
            "  setup — применить недостающее; удаляющие шаги только печатаются\n" +
            "  setup --confirm — применить, включая удаляющие шаги\n" +
            "  setup --check — только сверить: 0 — соответствует, 1 — отклонения\n",
        err = "",
    )
}

fun makeNoJavaRun(): LauncherRun {
    return LauncherRun(code = 2, out = "", err = "Нет Java — установите JDK 21: brew install openjdk@21\n")
}

fun makeOldJavaRun(): LauncherRun {
    return LauncherRun(
        code = 2,
        out = "",
        err = "Java 17, нужна ≥ 21 — установите JDK 21: brew install openjdk@21\n",
    )
}

fun makeVersionRun(): LauncherRun {
    return LauncherRun(code = 0, out = makeTaskVersion() + "\n", err = "")
}

fun makeVersionScript(): String {
    return System.getProperty(/* key = */ "task.versionScript")
}

fun makeSetupLauncher(): String {
    return System.getProperty("task.setupLauncher")
}

fun makeRepoRoot(): String {
    return System.getProperty(/* key = */ "task.repoRoot")
}

fun makeDownloadEnvironment(): Map<String, String> {
    return makeEnvironment(makeSystemPath(), makePublishedPackages(), makeGradle(code = 1))
}

fun makeCachedEnvironment(): Map<String, String> {
    val cache = makeTempDir(CACHE_PREFIX)
    val jar = File(/* parent = */ cache, /* child = */ "engineering-standards/task-${makeTaskVersion()}.jar")
    jar.parentFile.mkdirs()
    File(System.getProperty(/* key = */ "task.jar")).copyTo(jar)
    return makeEnvironment(
        path = makeSystemPath(),
        packages = makeEmptyPackages(),
        gradle = makeGradle(code = 1),
        cache = cache,
    )
}

fun makeLocalBuildEnvironment(): Map<String, String> {
    return makeEnvironment(makeSystemPath(), makeEmptyPackages(), makeGradle(code = 0))
}

fun makeFailedBuildEnvironment(): Map<String, String> {
    return makeEnvironment(makeSystemPath(), makeEmptyPackages(), makeGradle(code = 1))
}

// Первая `java` в PATH не запускается — как заглушка macOS `/usr/bin/java` без установленной JDK.
fun makeNoJavaEnvironment(): Map<String, String> {
    return makeEnvironment(makeJavaPath(script = "exit 1"), makeEmptyPackages(), makeGradle(code = 1))
}

fun makeOldJavaEnvironment(): Map<String, String> {
    return makeEnvironment(
        makeJavaPath(script = "echo 'openjdk version \"17.0.2\" 2022-01-18' >&2"),
        makeEmptyPackages(),
        makeGradle(code = 1),
    )
}

fun makeBareEnvironment(): Map<String, String> {
    return makeEnvironment(makeBarePath(), makeEmptyPackages(), makeGradle(code = 1))
}

fun makeTaskLauncher(): String {
    return System.getProperty(/* key = */ "task.launcher")
}

fun makeBarePath(): String {
    return "/usr/bin:/bin"
}

// Вывод — в файлы: иначе сломанное перенаправление `-C` (копия запускает себя бесконечно) повесит чтение вывода,
// и тест зависнет, а не упадёт.
fun runLauncher(
    environment: Map<String, String>,
    args: List<String>,
    launcher: String = makeTaskLauncher(),
    directory: File? = null,
    timeout: Duration = 3.minutes,
): LauncherRun {
    val logs = makeTempDir(prefix = "launcher")
    val out = File(/* parent = */ logs, /* child = */ "out")
    val err = File(/* parent = */ logs, /* child = */ "err")
    val builder = ProcessBuilder(listOf(launcher) + args).directory(directory)
        .redirectOutput(out)
        .redirectError(err)
    builder.environment().putAll(environment)
    val process = builder.start()
    val isFinished = process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)
    if (!isFinished) {
        process.destroyForcibly()
    }
    check(isFinished) { "task ${args.joinToString(separator = " ")} не завершился за $timeout" }
    return LauncherRun(code = process.exitValue(), out = out.readText(), err = err.readText())
}

// Свой пустой кеш на каждый случай: jar прошлого случая не подменяет скачивание или сборку.
private fun makeEnvironment(
    path: String,
    packages: String,
    gradle: String,
    cache: File = makeTempDir(CACHE_PREFIX),
): Map<String, String> {
    return mapOf(
        "PATH" to path,
        "XDG_CACHE_HOME" to cache.absolutePath,
        "TASK_PACKAGES_URL" to "file://$packages",
        "TASK_GRADLE" to gradle,
    )
}

// java тестовой JVM — первой: запускатель запускает jar той же Java, на которой его собрал Gradle.
private fun makeSystemPath(): String {
    return "${System.getProperty(/* key = */ "java.home")}/bin:${System.getenv(/* name = */ "PATH")}"
}

private fun makeJavaPath(script: String): String {
    val java = makeStub(File(makeTempDir(prefix = "stub"), /* child = */ "java"), script)
    return "${java.parentFile.absolutePath}:${makeSystemPath()}"
}

// Код 0 — запускатель забирает `build/libs/task.jar`, собранный до тестов.
private fun makeGradle(code: Int): String {
    val gradle = makeStub(File(makeTempDir(prefix = "gradle"), /* child = */ "gradlew"), script = "exit $code")
    return gradle.absolutePath
}

private fun makeStub(file: File, script: String): File {
    file.writeText(text = "#!/bin/sh\n$script\n")
    file.setExecutable(/* executable = */ true)
    return file
}

private fun makePublishedPackages(): String {
    return System.getProperty(/* key = */ "task.packages")
}

private fun makeEmptyPackages(): String {
    return makeTempDir(prefix = "packages").absolutePath
}

private fun makeTaskVersion(): String {
    return System.getProperty(/* key = */ "task.version")
}
