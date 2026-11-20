# Агенты работают без запросов доступа

**Цель:** исполнитель и ревьюеры плана правят файлы через Edit/Write и шлют в Bash по одной команде — в режиме «accept edits» прогон `/execute` идёт без запросов доступа.
**Архитектура:** у `task` появляется опция `-C <папка>` (как у `git -C`) — агенту не нужен `cd`; `setup` дописывает в `allow` правило `Edit(/plans/journal/**)` — журналы и файл сообщения коммита пишутся без вопросов; правило «файлы — Edit/Write, Bash — одна команда» лежит в `rules/bash-commands.md` и копией 1 в 1 — в 4 агентах, совпадение проверяет `MarkersSpec`.
**Стек:** Kotlin (скрипты `task`/`setup`), Kotest + kotestkit + MockK, Markdown (правила, агенты).

---

## Ограничения
- Чужие правила `allow` проекта не трогаем — только дописываем недостающие правила стандарта (как сейчас делает `PluginSettings`).
- `task` без `-C` работает как раньше: текущая папка — `user.dir`.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: опция `task -C <папка>`; второе правило `allow` в `setup`; правило `rules/bash-commands.md` и его копии в `plan-executor` и трёх ревьюерах; команды агентов — через `git -C` / `task -C`, сообщение коммита — Write в `<рабочая папка>/plans/journal/<slug>-commit.txt`.
- Какую проблему решает, что даёт: исполнитель шлёт один скрипт (`cd …; sed -i …; cat > … <<EOF; task commit …; git status`), он не подходит ни под одно правило `allow` — запрос доступа на каждый шаг (#98). После правки: файлы — Edit/Write (их принимает «accept edits», журнал — правило `Edit(/plans/journal/**)`), Bash — одиночные `task …` (правило `Bash(task:*)`) и `git -C …` (только чтение у ревьюеров).
- Как встраивается: `-C` разбирается в `Main.kt` до создания `ProcessShell` — все команды `task` получают папку из опции; правило основной сессии грузит хук `SessionStart` (`hooks/session-start:28`), агентам — копия в их тексте.
- Критерии цели:
  - `task -C <папка> <команда>` работает в указанной папке; без пути или с несуществующей папкой — понятная ошибка, код 2 (`WorkingDirectorySpec`);
  - `setup` дописывает `Edit(/plans/journal/**)`, повторно не дублирует, чужие правила сохраняет (`PluginSettingsSpec`);
  - правило в `rules/bash-commands.md` и в 4 агентах — копии 1 в 1 (`MarkersSpec`);
  - `/execute` в «Ворктри» и «accept edits»: исполнитель отмечает `[x]`, пишет сообщение, коммитит без запросов; в журнале прогона нет команд с `cd`/`;`/`&&`;
  - ревьюеры в том же прогоне — без запросов;
  - `/groom` в обычном режиме: запись журнала без запроса;
  - в копии: запись в `.worktrees/<slug>/plans/journal/` без запроса.

## Контекст
- Задействованные файлы:
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:30` — папка из `user.dir`, дальше в `ProcessShell`, `CommitCommand`, `PlanCommand`
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/TaskError.kt` — `Failure` — код 2
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/SystemConsole.kt` — `error(text)` в stderr
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PluginSettings.kt` — `permission()`, константа `RULE = "Bash(task:*)"`
  - `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PluginSettingsSpec.kt`, `PluginSettingsSpecData.kt` — случаи и данные `allow`
  - `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpec.kt`, `MarkersSpecData.kt` — копии блоков 1 в 1 (`COPIES`)
  - `agents/plan-executor.md:22` (вход «Рабочая папка»), `:72` (коммит)
  - `agents/{implementation,test,standards}-reviewer.md` — строка «`Bash` — только чтение: `git diff`, `git log`; …»
  - `.claude/settings.json` — `allow`: `Bash(task:*)`
  - `README.md:46` — что пишет `setup` в `.claude/settings.json`
- Паттерны: спека + данные (`*Spec.kt` + `*SpecData.kt`, фабрики `make…`), `shouldReturnTest`/`shouldThrowWithMessageTest` из kotestkit, временная папка — от `System.getProperty("task.cache")` (`GitignoreBlockSpecData.kt:7`); блоки-копии между маркерами `<!-- >>> префикс:имя -->` (`MarkersSpecData.kt`).
- Зависимости: PR 3 ссылается на `task -C` из PR 1 и на правило `allow` из PR 2.

## Структура файлов
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectory.kt` — разбор опции `-C`; вход: `WorkingDirectory(current: File).split(args: List<String>): Invocation`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:29` — папка и аргументы из `WorkingDirectory`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectorySpec.kt`, `WorkingDirectorySpecData.kt` — случаи `-C`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PluginSettings.kt` — список правил `allow` вместо одного
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PluginSettingsSpec.kt`, `PluginSettingsSpecData.kt` — второе правило, случай «недостающее дописывается»
- Изменить: `.claude/settings.json` — правило `Edit(/plans/journal/**)`
- Изменить: `README.md:46` — два разрешения
- Создать: `rules/bash-commands.md` — правило: файлы — Edit/Write, Bash — одна команда
- Изменить: `agents/plan-executor.md` — копия правила; команды и коммит через `-C`, файл сообщения
- Изменить: `agents/implementation-reviewer.md`, `agents/test-reviewer.md`, `agents/standards-reviewer.md` — копия правила; `git -C`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpec.kt`, `MarkersSpecData.kt` — пары «правило ↔ агент»

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: подходы — из груминга #98 (решения 1–5).
- Решения:
  - `-C` — только первым аргументом, до имени команды (как `git -C`): `task commit -C x` — аргументы команды, не опция; разбор в отдельном классе — `Main` не тестируется, разбор — да.
  - Ошибка `-C` печатается в `main` до `Task.run` — `ProcessShell` нужна папка до создания команд; код выхода — `TaskError.Failure.exitCode` (2).
  - `PluginSettings`: список `RULES`, по находке на правило — видно, какое правило добавлено.
  - Файл сообщения коммита — `<рабочая папка>/plans/journal/<slug>-commit.txt`: внутри рабочей папки, `plans/journal/` в `.gitignore` — `git status` чист; покрыт правилом `Edit(/plans/journal/**)`.
  - Правило агентам — копией, а не ссылкой (изменено при плане, решение груминга 3): агент работает в проекте пользователя, `rules/` лежит в папке плагина, путь к ней агенту неизвестен; совпадение копий проверяет `MarkersSpec`.
  - Ревьюерам — блок правила целиком, со строкой про Edit/Write, хотя этих инструментов у них нет: копия 1 в 1 — условие `MarkersSpec`, лишняя строка им не мешает.
  - Сборка в другой папке — `<папка>/gradlew -p <папка> …` (ревью плана, В1): без `cd`; под правило проекта вида `Bash(./gradlew:*)` не подходит — запрос доступа на сборку остаётся, см. «Открыто».
- Как вписывается в систему: `rules/*.md` грузит хук основной сессии; агенты получают правило текстом; `setup` владеет правилами стандарта в `allow`.
- Обратная совместимость: нужна — `task` без `-C` и чужие `allow` не меняются.
- Отклонено:
  - `cd <папка> && task …` — пропуск `cd` и разбор `&&` в Claude Code не проверены, баг может остаться;
  - «Bash уже в рабочей папке» — ломается, если #96 уберёт `EnterWorktree`;
  - файл сообщения в основной папке рядом с журналом — из копии запись запрещена (#96); в `/tmp` — вне рабочих папок, тот же запрос;
  - правило только в агентах — основная сессия продолжит слать скрипты;
  - правило `allow` руками только в этом репозитории — проекты на стандарте не получат;
  - строка-ссылка на `rules/bash-commands.md` в агентах — путь не разрешится в проекте пользователя.

## Технические детали
- `WorkingDirectory.Invocation(directory: File, args: List<String>)` — data class; `-C` первым → папка = абсолютный путь как есть, относительный — `File(current, path)`; аргументы без первых двух.
- Ошибки: `-C` без пути — `-C без папки — task -C <папка> <команда>`; не папка — `-C <путь>: нет такой папки — проверьте путь`.
- `PluginSettings.RULES = listOf("Bash(task:*)", "Edit(/plans/journal/**)")`; недостающие дописываются в конец `allow` в порядке `RULES`.
- Блок правила — `<!-- >>> rules:bash-commands -->` … `<!-- <<< rules:bash-commands -->`, текст 1 в 1 в `rules/bash-commands.md` и 4 агентах.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: `task` работает в папке из опции `-C`
  - [x] Задача 1: опция `-C` у `task`
  - [x] Приёмка
- PR 2: `setup` разрешает запись журналов без запроса
  - [x] Задача 2: список правил `allow` в `PluginSettings`
  - [x] Задача 3: правило в настройках репозитория и в README
  - [ ] Приёмка
- PR 3: агенты правят файлы через Edit/Write и шлют одиночные команды
  - [ ] Задача 4: правило `rules/bash-commands.md` и его копии в агентах
  - [ ] Задача 5: команды агентов через `-C`
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- ручные проверки `/execute`, `/groom` и записи в копии — раздел «После завершения»

## PR 1: `task` работает в папке из опции `-C`
**Объём:** `cli/WorkingDirectory.kt`, `cli/Main.kt`, `cli/WorkingDirectorySpec.kt`, `cli/WorkingDirectorySpecData.kt` — опция `-C`; ≈ 4 файла

### Задача 1: опция `-C` у `task`
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectory.kt` — разбор `-C`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:25-30`, `:68` — папка и аргументы из `WorkingDirectory`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectorySpec.kt`, `WorkingDirectorySpecData.kt` — без `-C`, абсолютный и относительный путь, `-C` без пути, нет папки, `-C` не первым

- [x] 1. Падающий тест: случаи `-C`
  `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectorySpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.cli

  import java.io.File
  import java.nio.file.Files

  /** Временная текущая папка `task` — своя на каждый тест. */
  fun makeCurrentDirectory(): File {
      val base = File(System.getProperty("task.cache"), "cli")
      base.mkdirs()
      return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "current").toFile()
  }

  /** Папка копии внутри [current] — создаётся. */
  fun makeCopyDirectory(current: File): File {
      val copy = File(current, "copy")
      copy.mkdirs()
      return copy
  }

  fun makeCommitArgs(): List<String> {
      return listOf("commit", "--message-file", "plans/journal/slug-commit.txt")
  }
  ```
  `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectorySpec.kt`:
  ```kotlin
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
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*WorkingDirectorySpec*'` — падает: `Unresolved reference: WorkingDirectory`
- [x] 3. Минимальный код: разбор `-C` и подключение в `main`
  `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/WorkingDirectory.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.cli

  import java.io.File

  /**
   * Опция `-C <папка>` перед командой, как у `git -C` (#98): `task` работает в указанной папке — агенту не нужен `cd`.
   * Без опции — текущая папка [current]; относительный путь — от неё. `-C` после имени команды — аргумент команды.
   */
  class WorkingDirectory(
      private val current: File,
  ) {
      fun split(args: List<String>): Invocation {
          if (args.firstOrNull() != OPTION) {
              return Invocation(directory = current, args = args)
          }
          val path = args.getOrNull(index = 1)
              ?: throw TaskError.Failure(message = "$OPTION без папки — task $OPTION <папка> <команда>")
          val directory = File(path).let { file -> if (file.isAbsolute) file else File(current, path) }
          if (!directory.isDirectory) {
              throw TaskError.Failure(message = "$OPTION $path: нет такой папки — проверьте путь")
          }
          return Invocation(directory = directory, args = args.drop(n = 2))
      }

      /** Папка, в которой работает `task`, и аргументы без опции `-C`. */
      data class Invocation(
          val directory: File,
          val args: List<String>,
      )

      private companion object {
          const val OPTION = "-C"
      }
  }
  ```
  `Main.kt` — KDoc и начало `main` (строки 25–30):
  было:
  ```kotlin
  /**
   * Точка входа `task`: её запускает `scripts/task` из jar в кеше и передаёт корень плагина в `TASK_PLUGIN_ROOT`.
   * Команды работают в текущей папке.
   */
  fun main(args: Array<String>) {
      val directory = File(System.getProperty("user.dir"))
  ```
  стало:
  ```kotlin
  /**
   * Точка входа `task`: её запускает `scripts/task` из jar в кеше и передаёт корень плагина в `TASK_PLUGIN_ROOT`.
   * Команды работают в текущей папке или в папке из `-C <папка>` ([WorkingDirectory]).
   */
  fun main(args: Array<String>) {
      val invocation = try {
          WorkingDirectory(current = File(System.getProperty("user.dir"))).split(args = args.toList())
      } catch (error: TaskError) {
          SystemConsole.error(text = error.message.orEmpty())
          exitProcess(error.exitCode)
      }
      val directory = invocation.directory
  ```
  последняя строка `main` (строка 68):
  было: `    exitProcess(task.run(args = args.toList()))`
  стало: `    exitProcess(task.run(args = invocation.args))`
- [x] 4. Запустить `./gradlew :scripts:test --tests '*WorkingDirectorySpec*'` — `BUILD SUCCESSFUL`; затем `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [x] 5. Коммит — тест и код вместе

### Приёмка
- [x] Что даёт этот PR — проверено: `task -C <путь к репозиторию> --help` печатает список команд; `task -C missing --help` — `-C missing: нет такой папки — проверьте путь`, код 2
- [x] Крайние случаи обработаны: без `-C`, `-C` без пути, относительный путь, `-C` после команды
- [x] Полный набор тестов зелёный: `./gradlew :scripts:check` — `BUILD SUCCESSFUL`

## PR 2: `setup` разрешает запись журналов без запроса
**Объём:** `setup/PluginSettings.kt`, `setup/PluginSettingsSpec.kt`, `setup/PluginSettingsSpecData.kt`, `.claude/settings.json`, `README.md` — правило `Edit(/plans/journal/**)`; ≈ 5 файлов

### Задача 2: список правил `allow` в `PluginSettings`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PluginSettings.kt` — `RULES` вместо `RULE`, `permissions()` — находка на правило
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PluginSettingsSpecData.kt` — второе правило в данных и находках; данные «только Bash»
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PluginSettingsSpec.kt` — случай «недостающее правило дописывается, имеющееся не дублируется»

- [x] 1. Падающий тест: данные со вторым правилом
  `PluginSettingsSpecData.kt`:
  - `makeStandardKeys` — массив `allow` (строки 62–64), было:
    ```kotlin
            "    \"allow\": [",
            "      \"Bash(task:*)\"",
            "    ]",
    ```
    стало:
    ```kotlin
            "    \"allow\": [",
            "      \"Bash(task:*)\",",
            "      \"Edit(/plans/journal/**)\"",
            "    ]",
    ```
  - `makeProjectSettingsWithStandard` — массив `allow` (строки 91–94), стало:
    ```kotlin
                "allow": [
                  "Bash(./gradlew:*)",
                  "Bash(task:*)",
                  "Edit(/plans/journal/**)"
                ]
    ```
  - `makePermissionOnlySettings` — массив `allow`, стало:
    ```kotlin
                "allow": [
                  "Bash(task:*)",
                  "Edit(/plans/journal/**)"
                ]
    ```
  - новая фабрика после `makePermissionOnlySettings` — настройки до #98:
    ```kotlin
    /** Настройки до #98: только правило `Bash(task:*)`. */
    fun makeBashOnlySettings(): String {
        return """
            {
              "permissions": {
                "allow": [
                  "Bash(task:*)"
                ]
              }
            }
        """.trimIndent() + "\n"
    }
    ```
  - `makeSettingsAddedFindings` — в конец списка:
    ```kotlin
            makeSettingsFinding(
                status = Finding.Status.FIXED,
                text = "разрешение Edit(/plans/journal/**) добавлено — закоммитьте через задачу",
            ),
    ```
  - `makeSettingsMissingFindings` — в конец списка:
    ```kotlin
            makeSettingsFinding(status = Finding.Status.DEVIATION, text = "нет разрешения Edit(/plans/journal/**)"),
    ```
  - `makeSettingsUnchangedFindings` — в конец списка:
    ```kotlin
            makeSettingsFinding(status = Finding.Status.OK, text = "разрешение Edit(/plans/journal/**)"),
    ```
  - `makeDisabledFindings`: `return listOf(unchanged[0], disabled, unchanged[2], unchanged[3])`
  - `makeOtherSourceFindings`: `return listOf(other, unchanged[1], unchanged[2], unchanged[3])`
  - `makeStandardRepositoryFindings` — в конец списка:
    ```kotlin
            makeSettingsFinding(
                status = Finding.Status.FIXED,
                text = "разрешение Edit(/plans/journal/**) добавлено — закоммитьте через задачу",
            ),
    ```
  - новая фабрика в конце файла:
    ```kotlin
    /** Репозиторий стандарта с `Bash(task:*)`: имеющееся правило — OK, недостающее — дописано. */
    fun makeJournalAddedFindings(): List<Finding> {
        val standard = makeStandardRepositoryFindings()
        return listOf(
            standard[0],
            makeSettingsFinding(status = Finding.Status.OK, text = "разрешение Bash(task:*)"),
            standard[2],
        )
    }
    ```
  `PluginSettingsSpec.kt` — после случая `makePermissionOnlySettings()` (строка 81):
  ```kotlin
          shouldReturnTest(makeJournalAddedFindings()) {
              writeSettings(root = root, text = makeBashOnlySettings())
              PluginSettings(git = git, pluginRoot = root).run(mode = makeApplyMode())
          }

          shouldReturnTest(makePermissionOnlySettings()) {
              writeSettings(root = root, text = makeBashOnlySettings())
              PluginSettings(git = git, pluginRoot = root).run(mode = makeApplyMode())
              readSettings(root = root)
          }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*PluginSettingsSpec*'` — падает: несовпадение ожидаемого и фактического — в фактическом нет `Edit(/plans/journal/**)` (в находках и в тексте настроек)
- [x] 3. Минимальный код: `PluginSettings.kt`
  - KDoc класса, было: «стандарт владеет тремя ключами — маркетплейс, `enabledPlugins` и разрешение `Bash(task:*)`; …»; стало: «стандарт владеет тремя ключами — маркетплейс, `enabledPlugins` и разрешения `Bash(task:*)` и `Edit` на журналы плана в `plans/journal/` (#98: журналы и сообщение коммита — без запроса доступа); недостающие разрешения дописываются, остальные ключи и правила проекта не трогаются. …» (остаток абзаца без изменений). В KDoc нельзя писать `/**` внутри текста: Kotlin откроет вложенный комментарий; точное правило — в `RULES`.
  - в `run`: `val findings = plugin + permissions(fields = fields, mode = mode)`
  - метод `permission(...)` целиком заменить на:
    ```kotlin
    private fun permissions(
        fields: MutableMap<String, JsonValue>,
        mode: Setup.Mode,
    ): List<Finding> {
        val permissions = objectAt(fields = fields, key = PERMISSIONS)
        val allow = permissions.fields[ALLOW] ?: JsonArray(items = emptyList())
        val items = (allow as? JsonArray)?.items
            ?: throw TaskError.Failure(message = "$SETTINGS: $PERMISSIONS.$ALLOW — ожидается массив […]")
        val missing = RULES.filter { rule -> JsonString(value = rule) !in items }
        if (mode.apply && missing.isNotEmpty()) {
            val updated = JsonArray(items = items + missing.map { rule -> JsonString(value = rule) })
            fields[PERMISSIONS] = JsonObject(fields = permissions.fields + (ALLOW to updated))
        }
        return RULES.map { rule ->
            when {
                rule !in missing -> finding(status = Finding.Status.OK, text = "разрешение $rule")
                !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "нет разрешения $rule")
                else -> finding(status = Finding.Status.FIXED, text = "разрешение $rule добавлено$COMMIT")
            }
        }
    }
    ```
  - в `companion object`: `const val RULE = "Bash(task:*)"` заменить на
    ```kotlin
        val RULES = listOf("Bash(task:*)", "Edit(/plans/journal/**)")
    ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*PluginSettingsSpec*'` — `BUILD SUCCESSFUL`; затем `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [x] 5. Коммит — тест и код вместе

### Задача 3: правило в настройках репозитория и в README
- Изменить: `.claude/settings.json` — правило `Edit(/plans/journal/**)`
- Изменить: `README.md:46` — два разрешения

Тест: не применим — настройки и текст; формат `.claude/settings.json` проверяет `PluginSettings` (шаг 2).
- [x] 1. `.claude/settings.json` — целиком:
  ```json
  {
    "permissions": {
      "allow": [
        "Bash(task:*)",
        "Edit(/plans/journal/**)"
      ]
    }
  }
  ```
- [x] 2. Проверить, что `PluginSettings` считает файл полным: `./gradlew :scripts:test --tests '*PluginSettingsSpec*'` — `BUILD SUCCESSFUL`, и сравнить текст файла с `makePermissionOnlySettings()` в `PluginSettingsSpecData.kt` — совпадает (формат печати `setup`, повторный `setup` файл не перепишет)
- [x] 3. `README.md:46`, было:
  `- \`.claude/settings.json\` — маркетплейс \`engineering-standards\`, включённый плагин и разрешение \`Bash(task:*)\`.`
  стало:
  `- \`.claude/settings.json\` — маркетплейс \`engineering-standards\`, включённый плагин и разрешения \`Bash(task:*)\` и \`Edit(/plans/journal/**)\` (журналы и сообщение коммита — без запроса доступа).`
- [x] 4. Коммит

### Приёмка
- [ ] Что даёт этот PR — проверено: `PluginSettingsSpec` — дописывает `Edit(/plans/journal/**)`, повторно не дублирует (`makeSettingsUnchangedFindings`), чужое `Bash(./gradlew:*)` сохраняет (`makeProjectSettingsWithStandard`)
- [ ] Крайние случаи обработаны: только `Bash(task:*)` → дописано одно правило; режим проверки — две находки DEVIATION
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] Перед PR 3 — вручную (ревью плана, В2): в режиме «accept edits» команда `git -C <путь к копии> diff` проходит без запроса доступа. Спросила — ⚠️ блокер: PR 3 не начинать, новый круг обсуждения с пользователем (правило `Bash(git -C:*)` в `RULES` или `git diff` без `-C`)

## PR 3: агенты правят файлы через Edit/Write и шлют одиночные команды
**Объём:** `rules/bash-commands.md`, `agents/plan-executor.md`, `agents/{implementation,test,standards}-reviewer.md`, `task/MarkersSpec.kt`, `task/MarkersSpecData.kt` — правило и его копии; ≈ 7 файлов

### Задача 4: правило `rules/bash-commands.md` и его копии в агентах
- Создать: `rules/bash-commands.md` — правило
- Изменить: `agents/plan-executor.md`, `agents/implementation-reviewer.md`, `agents/test-reviewer.md`, `agents/standards-reviewer.md` — блок `rules:bash-commands`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpecData.kt` — пары «правило ↔ агент»
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpec.kt` — ожидаемые блоки, названия

- [ ] 1. Падающий тест: пары «правило ↔ агент»
  `MarkersSpecData.kt` — от `/** Скилл и агент…` до конца `COPIES` (строки 5–11), стало:
  ```kotlin
  /** Источник и копия, у которых блоки `<префикс>:*` — копии 1 в 1. */
  private class Copies(val source: String, val copy: String, val prefix: String)

  private val COPIES = listOf(
      Copies(source = "skills/decompose/SKILL.md", copy = "agents/plan-reviewer.md", prefix = "plan"),
      Copies(source = "skills/execute/SKILL.md", copy = "agents/plan-executor.md", prefix = "execute"),
  ) + listOf("plan-executor", "implementation-reviewer", "test-reviewer", "standards-reviewer").map { agent ->
      Copies(source = "rules/bash-commands.md", copy = "agents/$agent.md", prefix = "rules")
  }
  ```
  `findMarkerMismatches` и `findSkillMarkers` целиком заменить на:
  ```kotlin
  /** Имена блоков, чей текст в источнике и копии расходится или есть только в одном файле. */
  fun findMarkerMismatches(): List<String> = COPIES.flatMap { copies ->
      val source = blocks(path = copies.source, prefix = copies.prefix)
      val copy = blocks(path = copies.copy, prefix = copies.prefix)
      (source.keys + copy.keys).sorted().filter { name -> source[name] != copy[name] }
  }

  /** Имена блоков всех источников по алфавиту, без повторов — пустой источник не даёт ложно-зелёного сравнения. */
  fun findSourceMarkers(): List<String> =
      COPIES.flatMap { copies -> blocks(path = copies.source, prefix = copies.prefix).keys }.distinct().sorted()
  ```
  `MarkersSpec.kt` — комментарий и тесты (строки 7–29), стало:
  ```kotlin
  // Вне стандарта: спека проверяет не класс, а пары файлов — блоки между маркерами в них копии 1 в 1:
  // `plan:*` — `skills/decompose/SKILL.md` и `agents/plan-reviewer.md` (#46),
  // `execute:*` — `skills/execute/SKILL.md` и `agents/plan-executor.md` (#48): правило в скилле и в агенте не расходится,
  // `rules:*` — `rules/bash-commands.md` и 4 агента с Bash (#98): правила плагина грузятся только основной сессии.
  // Разбор — в MarkersSpecData.
  class MarkersSpec :
      BehaviorSpec({
          shouldReturnTest(
              emptyList<String>(),
              options = TestOptions(name = "блоки plan:*, execute:* и rules:* — копии 1 в 1"),
          ) {
              findMarkerMismatches()
          }

          shouldReturnTest(
              listOf(
                  "execute:unit",
                  "plan:coverage",
                  "plan:decomposition",
                  "plan:granularity",
                  "plan:no-stubs",
                  "plan:types",
                  "rules:bash-commands",
              ),
              options = TestOptions(name = "в источниках все блоки plan:*, execute:* и rules:*"),
          ) {
              findSourceMarkers()
          }
      })
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — падает: `FileNotFoundException: …/rules/bash-commands.md`
- [ ] 3. Минимальный код: правило и копии
  `rules/bash-commands.md` — целиком:
  ```markdown
  <!-- >>> rules:bash-commands -->
  Файлы — только Edit и Write: код, отметки `[x]` в плане, журнал, сообщение коммита. В Bash файлы не пишутся: ни `sed -i`, ни heredoc, ни `>`.
  Bash — одна команда за вызов: без `cd`, `;` и `&&`. Другая папка — опцией команды: `git -C <папка> …`, `task -C <папка> …`, `<папка>/gradlew -p <папка> …`.
  Причина: скрипт из нескольких команд не подходит ни под одно правило `allow` — запрос доступа на каждый шаг (#98).
  <!-- <<< rules:bash-commands -->
  ```
  `agents/plan-executor.md` — новый раздел между «## Ключевые принципы» (после п. 6) и «## Вход»:
  ```markdown
  ## Файлы и команды

  <!-- >>> rules:bash-commands -->
  Файлы — только Edit и Write: код, отметки `[x]` в плане, журнал, сообщение коммита. В Bash файлы не пишутся: ни `sed -i`, ни heredoc, ни `>`.
  Bash — одна команда за вызов: без `cd`, `;` и `&&`. Другая папка — опцией команды: `git -C <папка> …`, `task -C <папка> …`, `<папка>/gradlew -p <папка> …`.
  Причина: скрипт из нескольких команд не подходит ни под одно правило `allow` — запрос доступа на каждый шаг (#98).
  <!-- <<< rules:bash-commands -->
  ```
  `agents/implementation-reviewer.md`, `agents/test-reviewer.md`, `agents/standards-reviewer.md` — после строки «`Bash` — только чтение: `git diff`, `git log`; тесты не запускаешь — их прогнали исполнитель и CI. Ничего не исправляешь.», через пустую строку, — тот же блок от `<!-- >>> rules:bash-commands -->` до `<!-- <<< rules:bash-commands -->`, 1 в 1.
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — `BUILD SUCCESSFUL`; затем `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и правило вместе

### Задача 5: команды агентов через `-C`
- Изменить: `agents/plan-executor.md:22` — команды в рабочей папке через `-C`
- Изменить: `agents/plan-executor.md:72` — коммит через `task -C`, файл сообщения
- Изменить: `agents/implementation-reviewer.md`, `agents/test-reviewer.md`, `agents/standards-reviewer.md` — `git -C` в строке «`Bash` — только чтение»

Тест: не применим — текст агентов; блоки правила не затрагиваются, `MarkersSpec` остаётся зелёным.
- [ ] 1. `agents/plan-executor.md:22`, было:
  `- **Рабочая папка** — абсолютный путь к корню рабочей копии. Все команды — в ней.`
  стало:
  `- **Рабочая папка** — абсолютный путь к корню рабочей копии. Все команды — в ней, без \`cd\`: \`git -C <рабочая папка> …\`, \`task -C <рабочая папка> …\`, \`<рабочая папка>/gradlew -p <рабочая папка> …\`.`
- [ ] 2. `agents/plan-executor.md:72`, было:
  `   Коммит — \`task commit --message-file <файл сообщения по шаблону>\`: пути — из «Файлы:», план команда добавит сама. Только файлы своей задачи.`
  стало:
  `   Коммит — \`task -C <рабочая папка> commit --message-file <рабочая папка>/plans/journal/<slug>-commit.txt\`: сообщение по шаблону пишешь в этот файл через Write, \`<slug>\` — из имени журнала; пути — из «Файлы:», план команда добавит сама. Только файлы своей задачи.`
- [ ] 3. В трёх ревьюерах, было:
  `` `Bash` — только чтение: `git diff`, `git log`; тесты не запускаешь — их прогнали исполнитель и CI. Ничего не исправляешь. ``
  стало:
  `` `Bash` — только чтение: `git -C <рабочая папка> diff`, `git -C <рабочая папка> log`; тесты не запускаешь — их прогнали исполнитель и CI. Ничего не исправляешь. ``
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — `BUILD SUCCESSFUL`; `grep -n "task commit\|Все команды" agents/plan-executor.md` — обе строки с `-C`
- [ ] 5. Коммит

### Приёмка
- [ ] Что даёт этот PR — проверено: в `agents/plan-executor.md` и трёх ревьюерах — блок правила; команды — через `-C`; `grep -n "cd \|sed -i" agents/*.md` — не находит указаний на `cd` и `sed -i` как способ работы
- [ ] Крайние случаи обработаны: `MarkersSpec` ловит расхождение — поменять слово в блоке одного агента → тест красный, вернуть → зелёный
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам (автоматические — тестами PR 1–3; ручные — «После завершения»)
- [ ] Документация: `README.md` — обновлён в задаче 3; правило основной сессии — `rules/bash-commands.md` (грузит хук)

## Открыто
- Правило `allow` для сборки в другой папке (`<папка>/gradlew -p <папка> …`) — без него исполнитель спрашивает доступ на запуск тестов — пользователь — после PR 3: отдельная задача в Backlog, в #98 не входит
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка:
  - `/execute <N> <slug>` в «Ворктри», режим «accept edits», правила `allow` из `setup`: исполнитель отмечает `[x]`, пишет сообщение, коммитит без запросов доступа; в журнале прогона нет команд с `cd`, `;`, `&&`; запрос на `<папка>/gradlew -p …` ожидаем, пока нет правила `allow` (см. «Открыто»)
  - ревьюеры в том же прогоне — без запросов доступа
  - `/groom` в обычном режиме: запись `plans/journal/groom-<N>.md` без запроса (заодно — `Edit(...)` покрывает Write)
  - в копии: запись в `.worktrees/<slug>/plans/journal/` без запроса (правило `/…` — от папки файла настроек копии)
