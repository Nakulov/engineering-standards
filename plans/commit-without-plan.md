# task commit работает для задачи без плана

**Цель:** `task commit` требует строку «Задача плана: …», только если в ветке есть план `plans/*.md`.
**Архитектура:** `CommitCommand` находит план ветки через `git.addedPaths(base = "origin/dev")` и `isPlan`, как `PlanArchive.kt:15`, и передаёт путь плана (или `null`) в `CommitMessage.parse`. `parse` требует строку, только когда план есть, и называет его в отказе. Скилл груминга у «сразу в работу» говорит, как коммитить.
**Стек:** Kotlin, kotest `BehaviorSpec` + `io.github.nakulov.kotestkit`, mockk.

---

## Ограничения
- Не трогать: дубль `isPlan` в `CommitCommand.kt:41` и `PlanArchive.kt:31` — пользователь решил не выносить (груминг #82).
- Тесты `CommitCommand` — на `mockk<Git>`, как сейчас; реальный репо для `addedPaths` уже проверяет `GitSpec.kt:183`.

## Обзор
- Что реализуется: условная проверка строки «Задача плана: …» в `task commit`; строка в шаге 4 груминга про команды для «сразу в работу».
- Какую проблему решает, что даёт: задача «сразу в работу» коммитит через `task commit`, а не мимо команд (#77, #92).
- Как встраивается: проверка — до разбора сообщения; дальше проверка файлов и коммит — как сейчас (`CommitCommand.kt:31-37`).
- Критерии цели:
  - ветка без `plans/*.md`, сообщение без «Задача плана» → коммит, `planTask = null`;
  - ветка без плана, строка есть → принимается молча;
  - ветка с `plans/<slug>.md`, строки нет → отказ «в ветке план `plans/<slug>.md` — нет строки «Задача плана: …», добавьте заголовок задачи плана»;
  - `skills/groom/SKILL.md` у «сразу в работу» называет `task start`, `task commit --message-file`, `task pr`.

## Контекст
- Задействованные файлы:
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/CommitMessage.kt:18-30` — `parse(text)`, отказ без строки; `planTask` дальше нигде не читается.
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/CommitCommand.kt:30` — вызов `parse`; `isPlan` — `:41-45`.
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt:190` — `addedPaths(base)`: `git diff --name-only --diff-filter=A <base>...HEAD`.
  - `skills/groom/SKILL.md:30` — «сразу в работу — в этой сессии, без плана и `execute`.»
- Паттерны: поиск плана — `PlanArchive.kt:15` (`addedPaths(base = "origin/dev").filter { isPlan }`); тесты — `shouldReturnTest` / `shouldThrowWithMessageTest`, данные в `*SpecData.kt`.
- Зависимости: нет новых.

## Структура файлов
Отвечает за правило строки «Задача плана»:
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/CommitMessage.kt:8-40` — строка обязательна при плане; вход: `parse(text: String, plan: String?): CommitMessage`, `planTask: String?`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitMessageSpec.kt` — случаи с планом и без
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitMessageSpecData.kt` — текст без строки, ожидание с `planTask = null`

Отвечает за поиск плана ветки при коммите:
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/CommitCommand.kt:9-38` — `addedPaths` → путь плана → `parse`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitCommandSpec.kt` — ветка с планом и без
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitCommandSpecData.kt` — второй файл сообщения без строки

Отвечает за подсказку скилла:
- Изменить: `skills/groom/SKILL.md:30` — команды для «сразу в работу»

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: из груминга — строка обязательна, только если в ветке есть `plans/*.md`.
- Решения:
  - `parse(text, plan: String?)` вместо `parse(text, planRequired: Boolean)` из груминга — изменено при плане: отказ должен назвать план, булево его не несёт.
  - планов в ветке несколько — в отказе первый: обычно план один (`task plan` — один slug на ветку).
  - `addedPaths` вызывается после проверки аргументов и файла сообщения, до `parse`: ошибки использования не требуют git.
  - `origin/dev` недоступен — ошибка git как в `task pr`, без отдельной обработки (груминг).
- Как вписывается в систему: `execute` и исполнитель плана пишут строку как раньше (`agents/plan-executor.md:72-76`) — в их ветке план есть, проверка прежняя.
- Обратная совместимость: не нужна — `CommitMessage` используется только в `CommitCommand`.
- Отклонено:
  - строка необязательна всегда — исполнитель плана молча пропустит строку;
  - правило в `rules/` про коммит без плана — контекст каждой сессии, формат сообщения туда не помещается.

## Технические детали
- Структуры данных и изменения: `CommitMessage.planTask: String?` — `null`, если строки нет и плана нет.
- Параметры и форматы: `plan` — путь от корня, `plans/<slug>.md`.
- Поток обработки: аргументы → файл сообщения → `git.addedPaths(base = "origin/dev")` → первый путь по `isPlan` → `CommitMessage.parse(text, plan)` → `git.changedPaths()` → проверка файлов → `git.commitPaths` → «коммит: …».

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: `task commit` принимает коммит без «Задачи плана» в ветке без плана
  - [ ] Задача 1: `CommitMessage.parse` требует строку только при плане
  - [ ] Задача 2: `CommitCommand` передаёт план ветки в `parse`
  - [ ] Задача 3: груминг называет команды для «сразу в работу»
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Следующая задача «сразу в работу» коммитит через `task commit` без строки «Задача плана».

## PR 1: `task commit` принимает коммит без «Задачи плана» в ветке без плана
**Объём:** `CommitMessage.kt`, `CommitCommand.kt` — правило; их спеки и данные — тесты; `skills/groom/SKILL.md` — подсказка; ≈ 7 файлов

### Задача 1: `CommitMessage.parse` требует строку только при плане
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/CommitMessage.kt:5-40` — параметр `plan`, `planTask: String?`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitMessageSpec.kt` — новые случаи, старые — с `plan`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitMessageSpecData.kt` — данные без строки

- [ ] 1. Падающий тест: без плана без строки → `planTask = null`; без плана со строкой → принят; с планом без строки → отказ с именем плана; старые случаи — с `plan = makePlanPath()`.

  `CommitMessageSpecData.kt` — дописать в конец:
  ```kotlin
  fun makePlanPath(): String {
      return "plans/commit-commands.md"
  }

  fun makeNoPlanTaskCommitText(): String {
      return "Поправить опечатку\n\nФайлы:\n- `scripts/a.kt` — опечатка\n"
  }

  fun makeNoPlanTaskCommitMessage(): CommitMessage {
      return CommitMessage(title = "Поправить опечатку", planTask = null, files = listOf("scripts/a.kt"))
  }
  ```

  `CommitMessageSpec.kt` — целиком:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.standards.task.cli.TaskError
  import io.kotest.core.spec.style.BehaviorSpec

  class CommitMessageSpec :
      BehaviorSpec({
          shouldReturnTest(makeCommitMessage()) {
              CommitMessage.parse(text = makeCommitText(), plan = makePlanPath())
          }

          shouldReturnTest(makeCommitMessage()) {
              CommitMessage.parse(text = makeCommitText(), plan = null)
          }

          shouldReturnTest(makeNoPlanTaskCommitMessage()) {
              CommitMessage.parse(text = makeNoPlanTaskCommitText(), plan = null)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("первая строка сообщения пуста — напишите, что сделано") {
              CommitMessage.parse(text = makeNoTitleText(), plan = makePlanPath())
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "в ветке план `plans/commit-commands.md` — нет строки «Задача плана: …», добавьте заголовок задачи плана",
          ) {
              CommitMessage.parse(text = makeNoPlanTaskText(), plan = makePlanPath())
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "раздел «Файлы:» пуст — перечислите файлы строками «- `путь` — что изменено»",
          ) {
              CommitMessage.parse(text = makeNoFilesText(), plan = makePlanPath())
          }
      })
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*CommitMessageSpec*'` — падает: `No parameter with name 'plan' found` (компиляция теста).
- [ ] 3. Минимальный код — `CommitMessage.kt` целиком:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.cli.TaskError

  /**
   * Сообщение `task commit` по шаблону задачи плана (#53): заголовок, «Задача плана: …», «Файлы:» — пути для коммита.
   * «Задача плана» обязательна, только если в ветке есть план (#82): задача «сразу в работу» коммитит без неё.
   */
  data class CommitMessage(
      val title: String,
      val planTask: String?,
      val files: List<String>,
  ) {
      companion object {
          private const val PLAN_TASK = "Задача плана:"
          private const val FILES = "Файлы:"
          private val FILE_LINE = Regex(pattern = "^- `([^`]+)` — .+$")

          /** [plan] — план ветки `plans/<slug>.md`; `null` — плана нет, строка «Задача плана» не нужна. */
          fun parse(
              text: String,
              plan: String?,
          ): CommitMessage {
              val lines = text.lines()
              val title = lines.first().trim()
              if (title.isEmpty()) {
                  throw TaskError.Refusal(message = "первая строка сообщения пуста — напишите, что сделано")
              }
              val planTask = lines.firstOrNull { line -> line.startsWith(prefix = PLAN_TASK) }
                  ?.removePrefix(prefix = PLAN_TASK)
                  ?.trim()
                  ?.takeIf { task -> task.isNotEmpty() }
              if (planTask == null && plan != null) {
                  throw TaskError.Refusal(
                      message = "в ветке план `$plan` — нет строки «Задача плана: …», добавьте заголовок задачи плана",
                  )
              }
              val files = lines.dropWhile { line -> line.trim() != FILES }
                  .drop(n = 1)
                  .mapNotNull { line -> FILE_LINE.matchEntire(input = line.trim())?.groupValues?.get(index = 1) }
              if (files.isEmpty()) {
                  throw TaskError.Refusal(
                      message = "раздел «Файлы:» пуст — перечислите файлы строками «- `путь` — что изменено»",
                  )
              }
              return CommitMessage(title = title, planTask = planTask, files = files)
          }
      }
  }
  ```
  `CommitCommand.kt:30` — временно, чтобы собиралось до задачи 2: `CommitMessage.parse(text = file.readText(), plan = null)`.
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*CommitMessageSpec*'` — `BUILD SUCCESSFUL`.
- [ ] 5. Коммит — тест и код вместе (`CommitMessage.kt`, `CommitMessageSpec.kt`, `CommitMessageSpecData.kt`, `CommitCommand.kt`).

### Задача 2: `CommitCommand` передаёт план ветки в `parse`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/CommitCommand.kt:9-38` — поиск плана через `addedPaths`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitCommandSpec.kt` — ветка с планом и без
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/CommitCommandSpecData.kt` — файл `no-plan-task.txt`

- [ ] 1. Падающий тест.

  `CommitCommandSpecData.kt` — целиком:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import java.io.File
  import java.nio.file.Files

  /**
   * Папка запуска: `message.txt` по шаблону (файлы `scripts/a.kt`, `scripts/b.kt`) и `no-plan-task.txt` —
   * без «Задачи плана» (файл `scripts/a.kt`).
   */
  fun makeCommitDirectory(): File {
      val base = File(System.getProperty("task.cache"), "commit")
      base.mkdirs()
      val directory = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
      File(directory, "message.txt").writeText(makeCommitText())
      File(directory, "no-plan-task.txt").writeText(makeNoPlanTaskCommitText())
      return directory
  }
  ```

  `CommitCommandSpec.kt` — целиком:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.kotestkit.then
  import io.github.nakulov.kotestkit.`when`
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.Git
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.mockk.every
  import io.mockk.justRun
  import io.mockk.mockk
  import io.mockk.verifySequence
  import java.io.File

  class CommitCommandSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val git = mockk<Git>()
          val console = mockk<Console>()
          val directory = makeCommitDirectory()
          val command = CommitCommand(git = git, directory = directory)
          val messageFile = File(directory, "message.txt").path
          val noPlanTaskFile = File(directory, "no-plan-task.txt").path
          justRun { console.line(text = any()) }
          justRun { git.commitPaths(paths = any(), messageFile = any()) }
          every { git.addedPaths(base = "origin/dev") } returns listOf(makePlanPath(), "scripts/a.kt")

          given("файлы изменены, план отмечен") {
              every { git.changedPaths() } returns
                  listOf("scripts/a.kt", "scripts/b.kt", "plans/commit-commands.md", "other.txt")
              `when` {
                  command.run(args = listOf("--message-file", "message.txt"), console = console)
                  then {
                      verifySequence {
                          git.addedPaths(base = "origin/dev")
                          git.changedPaths()
                          git.commitPaths(
                              paths = listOf("scripts/a.kt", "scripts/b.kt", "plans/commit-commands.md"),
                              messageFile = messageFile,
                          )
                          console.line(text = "коммит: Добавить task commit")
                      }
                  }
              }
          }

          given("в ветке нет плана, в сообщении нет «Задачи плана»") {
              every { git.addedPaths(base = "origin/dev") } returns listOf("scripts/a.kt", "plans/done/old.md")
              every { git.changedPaths() } returns listOf("scripts/a.kt")
              `when` {
                  command.run(args = listOf("--message-file", "no-plan-task.txt"), console = console)
                  then {
                      verifySequence {
                          git.addedPaths(base = "origin/dev")
                          git.changedPaths()
                          git.commitPaths(paths = listOf("scripts/a.kt"), messageFile = noPlanTaskFile)
                          console.line(text = "коммит: Поправить опечатку")
                      }
                  }
              }
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "в ветке план `plans/commit-commands.md` — нет строки «Задача плана: …», добавьте заголовок задачи плана",
          ) {
              command.run(args = listOf("--message-file", "no-plan-task.txt"), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "`scripts/b.kt` не изменён или не существует — уберите из «Файлов»",
          ) {
              every { git.changedPaths() } returns listOf("scripts/a.kt")
              command.run(args = listOf("--message-file", "message.txt"), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Failure>("нет файла сообщения nope.txt") {
              command.run(args = listOf("--message-file", "nope.txt"), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Failure>("использование: task commit ${command.usage}") {
              command.run(args = emptyList(), console = console)
          }
      })
  ```
  `plans/done/old.md` в ветке без плана — проверка, что архив не считается планом (`isPlan` — один `/`).
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*CommitCommandSpec*'` — падает: в «файлы изменены, план отмечен» `verifySequence` не находит `git.addedPaths(base = "origin/dev")`; отказ с именем плана не брошен — коммит прошёл (`plan = null` из задачи 1).
- [ ] 3. Минимальный код — `CommitCommand.kt:9-38`:
  ```kotlin
  /**
   * `task commit --message-file <файл>` (#53): коммит по шаблону задачи плана. Пути — из раздела «Файлы:» сообщения;
   * изменённый план `plans/<имя>.md` добавляется сам — отметки `[x]` идут в коммит задачи.
   * «Задача плана» обязательна, только если в ветке есть план (#82).
   */
  class CommitCommand(
      private val git: Git,
      private val directory: File,
  ) : Command {
      override val usage: String = "--message-file <файл> — коммит по шаблону задачи плана: файлы — из раздела «Файлы:»"

      override fun run(
          args: List<String>,
          console: Console,
      ) {
          if (args.size != 2 || args.first() != MESSAGE_FILE) {
              throw TaskError.Failure(message = "использование: task commit $usage")
          }
          val file = File(args.last()).let { path -> if (path.isAbsolute) path else File(directory, args.last()) }
          if (!file.isFile) {
              throw TaskError.Failure(message = "нет файла сообщения ${args.last()}")
          }
          val plan = git.addedPaths(base = BASE).firstOrNull { path -> isPlan(path = path) }
          val message = CommitMessage.parse(text = file.readText(), plan = plan)
          val changed = git.changedPaths()
          val unchanged = message.files.firstOrNull { path -> path !in changed }
          if (unchanged != null) {
              throw TaskError.Refusal(message = "`$unchanged` не изменён или не существует — уберите из «Файлов»")
          }
          val plans = changed.filter { path -> isPlan(path = path) && path !in message.files }
          git.commitPaths(paths = message.files + plans, messageFile = file.path)
          console.line(text = "коммит: ${message.title}")
      }
  ```
  `CommitCommand.kt:47-49` — companion:
  ```kotlin
      private companion object {
          const val MESSAGE_FILE = "--message-file"
          const val BASE = "origin/dev"
      }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*CommitCommandSpec*'` — `BUILD SUCCESSFUL`.
- [ ] 5. Коммит — тест и код вместе (`CommitCommand.kt`, `CommitCommandSpec.kt`, `CommitCommandSpecData.kt`).

### Задача 3: груминг называет команды для «сразу в работу»
- Изменить: `skills/groom/SKILL.md:30` — строка про ветку, коммит и PR

- [ ] 1. Тест: не применим — текст скилла, логики нет.
- [ ] 2. Правка `skills/groom/SKILL.md:30`:

  было:
  ```
     - **сразу в работу** — в этой сессии, без плана и `execute`.
  ```
  стало:
  ```
     - **сразу в работу** — в этой сессии, без плана и `execute`: ветка — `task start <N> <slug>`, коммит — `task commit --message-file <файл>` (заголовок и «Файлы:» строками «- `путь` — что изменено»), PR — `task pr`.
  ```
- [ ] 3. Проверить: `grep -n 'сразу в работу' skills/groom/SKILL.md` → строка 30 содержит `task commit --message-file`.
- [ ] 4. Коммит (`skills/groom/SKILL.md`).

### Приёмка
- [ ] Что даёт этот PR — проверено: без плана коммит без строки проходит, с планом — отказ с именем плана
- [ ] Крайние случаи обработаны: строка есть без плана — принята; `plans/done/*.md` — не план
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам

## Открыто
- Нет

## После завершения
- Ручная проверка: следующая задача, которую груминг отправит «сразу в работу» — `task start`, сообщение без «Задачи плана», `task commit --message-file <файл>` → «коммит: …», без обхода через git.
