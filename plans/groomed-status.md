# Статус Groomed после груминга и плана

**Цель:** задача из Backlog сама переходит в Groomed после груминга (`task groomed <N>`) и после сохранения плана (`task plan`). Тогда `task start` больше не отказывает с «сначала груминг».
**Архитектура:** правило «Backlog → Groomed, другой статус не трогаем» живёт в одном классе `Grooming`. Его вызывают новая команда `GroomedCommand` и `PlanCommand` (последним шагом). Скилл `groom` вызывает `task groomed` на шаге 4, а скилл `decompose` описывает новое поведение `task plan`.
**Стек:** Kotlin, kotest `BehaviorSpec` + mockk, `kotestkit` (`shouldThrowWithMessageTest`, `shouldReturnTest`, `withGivens`), `gh project item-edit` через `Board`.

---

## Ограничения
- Не трогать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/board/Board.kt` — он низкоуровневый, а правило процесса живёт в командах, как `checkReady` в `StartCommand.kt:92` (решение груминга #69).
- Статус меняется только из Backlog. Groomed, In progress, In review и Done не трогаем.
- Если `task plan` не смог сменить статус, сообщение ведёт на повтор через `task groomed N`, а не через `task plan`: иначе получим лишний коммит «Обновить план».
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: команда `task groomed <N>`; перевод Backlog → Groomed в `task plan`; строки в скиллах `groom` и `decompose`.
- Какую проблему решает: Groomed сейчас не ставит никто, поэтому `task start` отказывает (`StartCommand.kt:92`). На пилотах #53 и #52 статус переводили на доске руками.
- Как встраивается: `task groomed` регистрируется в `Main.kt` рядом с остальными командами; `PlanCommand` получает `board`.
- Критерии цели:
  1. `task groomed <N>` для задачи в Backlog переводит её в Groomed и выходит с кодом 0;
  2. `task groomed <N>` для задачи в Groomed, In progress, In review или Done выводит «#N уже в «<статус>»», выходит с кодом 0 и статус не меняет;
  3. `task groomed <N>` для закрытой задачи или эпика отказывает с кодом ≠ 0, к доске не обращается;
  4. `task plan` переводит задачу из Backlog в Groomed после обновления тела, а другой статус не трогает;
  5. если `task plan` не смог сменить статус, он сообщает «план сохранён, статус — нет: task groomed N (<причина>)»;
  6. скилл `groom` на шаге 4 вызывает `task groomed <N>`, а в его «Ничего не меняешь» есть исключение для этого;
  7. скилл `decompose` в «После утверждения» описывает перевод в Groomed.

## Контекст
- Задействованные файлы:
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/PlanCommand.kt`: `task plan`. Отказы «закрыта» и «эпик» — `:48–52`, ошибка тела — `:74–79`; kdoc `:14` и `usage` `:23` говорят «без смены статуса».
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/StartCommand.kt:85–98`: `checkReady` — образец правила процесса над доской.
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/board/Board.kt`: `item(issue, nodeId): BoardItem` (`:15`), `move(item, status)` (`:102`), константы `BACKLOG` и `GROOMED` (`:121–122`).
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/github/Issues.kt`: `info(number): IssueInfo` (`:11`), `nodeId(number): String` (`:52`).
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:36` (`val board`), `:61` (`"plan"`).
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Task.kt:37`: справка строится в порядке карты команд.
  - `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/StartCommandSpecData.kt`: `makeTaskInfo`, `makeEpicInfo`, `makeClosedInfo` (number = 13), `makeBacklogItem`, `makeGroomedItem`, `makeInProgressItem`, `makeDoneItem` (options: Groomed=g1, In progress=p1, Done=d1).
  - `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt:15–29`: ожидаемая справка `task --help`.
- Паттерны: команда — класс `Command` с `usage` и `run`. Отказ — `TaskError.Refusal` (код 1), сбой — `TaskError.Failure` (код 2). Тесты — `BehaviorSpec`, `IsolationMode.InstancePerRoot`, `mockk`, общие данные из `*SpecData.kt` того же пакета.
- Зависимости: новых нет.

## Структура файлов
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/Grooming.kt` — перевод задачи из Backlog в Groomed; вход: `groom(number: Int): String`.
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/GroomingSpec.kt` — тест: из Backlog есть переход, из остальных статусов — нет.
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/GroomedCommand.kt` — команда `task groomed <N>`; вход: `GroomedCommand(issues: Issues, board: Board)`.
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/GroomedCommandSpec.kt` — тест: отказы, разбор аргументов, вызов `Grooming`.
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:61` — регистрация `"groomed"`, `board` для `PlanCommand`.
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt:27–28` — справка: строка `groomed` и новый `usage` у `plan`.
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/PlanCommand.kt:12–23, 81–82` — `board` в конструкторе, перевод статуса последним шагом, kdoc и `usage`.
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/PlanCommandSpec.kt` — мок `board`; тесты для Backlog, для не-Backlog и для сбоя статуса.
- Изменить: `skills/groom/SKILL.md:11, 28–30` — исключение в «Ничего не меняешь», вызов `task groomed` на шаге 4.
- Изменить: `skills/decompose/SKILL.md:190` — `task plan` переводит задачу из Backlog в Groomed.

## Прогресс
- `[x]` ставится сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Если работа выходит за объём, обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: подход А из груминга. `task plan` сам переводит задачу через общий с `task groomed` класс `Grooming`, поэтому переход нельзя забыть, и он не зависит от того, выполнит ли модель шаг скилла (#43, #70).
- Решения:
  - `Grooming` — отдельный класс, а не метод `Board`: `Board` низкоуровневый (решение груминга).
  - `GroomedCommand` и `PlanCommand` получают `board` и создают `Grooming(issues, board)` в инициализаторе свойства. Так в тестах мокается `Board`, как в `StartCommandSpec`.
  - В `task plan` статус меняется после `plan.delete()` и строки про тело. Если статус не сменился, локального плана уже нет, и повторять нужно `task groomed`, а не `task plan`.
  - Отказы «закрыта» и «эпик» проверяет `GroomedCommand`, а не `Grooming`: в `task plan` их уже проверил `PlanCommand.kt:48–52`.
- Как вписывается в систему: `task start` (`StartCommand.kt:92`) уже принимает Groomed, его не меняем.
- Обратная совместимость: не нужна. Аргументы `task plan` те же, только добавился побочный эффект на доске.
- Запасной: нет. Подход выбран на груминге, а отклонённый Б держится только на модели.
- Отклонено: Б — скилл `decompose` вызывает `task groomed` перед `task plan`. Держится на модели: если шаг пропущен, `task start` снова откажет (#43, #70).

## Технические детали
- Структуры данных и изменения: новых типов нет; `BoardItem.status` сравнивается с `Board.BACKLOG`.
- Форматы вывода:
  - переход — `#N → Groomed`;
  - нет перехода — `#N уже в «<статус>»`;
  - отказы — `#N закрыта`, `#N — эпик: в Groomed переводятся его подзадачи`;
  - сбой в `task plan` — `план сохранён, статус — нет: task groomed N (<сообщение ошибки>)`.
- Поток обработки: `issues.info` (только в `task groomed`) → `issues.nodeId` → `board.item` → `board.move(item, GROOMED)` только из Backlog → строка в консоль.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: задача из Backlog сама переходит в Groomed после груминга и плана
  - [ ] Задача 1: `Grooming` — перевод из Backlog в Groomed
  - [ ] Задача 2: команда `task groomed`
  - [ ] Задача 3: `task plan` переводит задачу в Groomed
  - [ ] Задача 4: скиллы `groom` и `decompose`
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- ручная проверка на доске (см. «После завершения»)

## PR 1: задача из Backlog сама переходит в Groomed после груминга и плана
**Объём:** 2 новых класса, 2 новых спека, `PlanCommand`, `PlanCommandSpec`, `Main`, `TaskLauncherSpecData`, 2 скилла; ≈ 10 файлов

### Задача 1: `Grooming` — перевод из Backlog в Groomed
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/Grooming.kt` — правило перехода
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/GroomingSpec.kt` — тесты статусов

- [ ] 1. Падающий тест: Backlog → `move` и строка «→ Groomed»; Groomed, In progress, In review, Done → без `move`, строка «уже в»
  ```kotlin
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
  ```
  Перед запуском сверь сигнатуры `withGivens` и `case` с использованием в `StartCommandSpec.kt:271–281` и `:302–310`.
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GroomingSpec*'` — падает: `Unresolved reference: Grooming`
- [ ] 3. Минимальный код:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.board.Board
  import io.github.nakulov.standards.task.github.Issues

  /**
   * Груминг закончен (#69): задача из Backlog — в Groomed; другой статус не меняется. Общее правило `task groomed` и
   * `task plan`; отказы по закрытой задаче и эпику — у вызывающей команды.
   */
  class Grooming(
      private val issues: Issues,
      private val board: Board,
  ) {
      /** Строка для консоли: переход или текущий статус. */
      fun groom(number: Int): String {
          val item = board.item(issue = number, nodeId = issues.nodeId(number = number))
          if (item.status != Board.BACKLOG) {
              return "#$number уже в «${item.status}»"
          }
          board.move(item = item, status = Board.GROOMED)
          return "#$number → ${Board.GROOMED}"
      }
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GroomingSpec*'` → `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Задача 2: команда `task groomed`
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/GroomedCommand.kt` — команда
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/GroomedCommandSpec.kt` — тесты команды
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:61` — регистрация `"groomed"`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt:27` — строка справки

- [ ] 1. Падающий тест: открытая задача в Backlog → `move` и строка; закрытая или эпик → `Refusal` без обращения к доске; аргументы `[]`, `["x"]`, `["13", "14"]` → `Failure` с usage
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.kotestkit.then
  import io.github.nakulov.kotestkit.`when`
  import io.github.nakulov.kotestkit.withGivens
  import io.github.nakulov.standards.task.board.Board
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.github.Issues
  import io.kotest.assertions.throwables.shouldThrowWithMessage
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.mockk.every
  import io.mockk.justRun
  import io.mockk.mockk
  import io.mockk.verify
  import io.mockk.verifySequence

  class GroomedCommandSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val issues = mockk<Issues>()
          val board = mockk<Board>()
          val console = mockk<Console>()
          val command = GroomedCommand(issues = issues, board = board)
          justRun { console.line(text = any()) }
          justRun { board.move(item = any(), status = any()) }
          every { issues.nodeId(number = 13) } returns "I_issue13"
          every { board.item(issue = 13, nodeId = "I_issue13") } returns makeBacklogItem()

          given("открытая задача в Backlog") {
              every { issues.info(number = 13) } returns makeTaskInfo()
              `when` {
                  command.run(args = listOf("13"), console = console)
                  then {
                      verifySequence {
                          issues.info(number = 13)
                          issues.nodeId(number = 13)
                          board.item(issue = 13, nodeId = "I_issue13")
                          board.move(item = makeBacklogItem(), status = Board.GROOMED)
                          console.line(text = "#13 → Groomed")
                      }
                  }
              }
          }

          withGivens(
              nameFn = { given -> "отказ: ${given.arg1}" },
              case(arg1 = "#13 закрыта", arg2 = makeClosedInfo()),
              case(arg1 = "#13 — эпик: в Groomed переводятся его подзадачи", arg2 = makeEpicInfo()),
          ) { message, info ->
              every { issues.info(number = 13) } returns info
              `when` {
                  shouldThrowWithMessage<TaskError.Refusal>(message) {
                      command.run(args = listOf("13"), console = console)
                  }
                  then {
                      verify(exactly = 0) { board.item(issue = any(), nodeId = any()) }
                  }
              }
          }

          withGivens(
              nameFn = { given -> "неверные аргументы ${given.arg1}" },
              case(arg1 = emptyList<String>()),
              case(arg1 = listOf("x")),
              case(arg1 = listOf("13", "14")),
          ) { args ->
              shouldThrowWithMessageTest<TaskError.Failure>("использование: task groomed ${command.usage}") {
                  command.run(args = args, console = console)
              }
          }
      })
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GroomedCommandSpec*'` — падает: `Unresolved reference: GroomedCommand`
- [ ] 3. Минимальный код — команда `GroomedCommand.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.board.Board
  import io.github.nakulov.standards.task.cli.Command
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.github.Issues

  /**
   * `task groomed <N>` (#69): груминг закончен — задача из Backlog в Groomed, после чего её берёт `task start`. Задача
   * уже в Groomed или дальше — статус не меняется; закрытая задача и эпик — отказ.
   */
  class GroomedCommand(
      private val issues: Issues,
      board: Board,
  ) : Command {
      private val grooming = Grooming(issues = issues, board = board)

      override val usage: String = "<N> — задача из Backlog в Groomed: груминг закончен"

      override fun run(
          args: List<String>,
          console: Console,
      ) {
          val number = args.singleOrNull()?.toIntOrNull()
              ?: throw TaskError.Failure(message = "использование: task groomed $usage")
          val info = issues.info(number = number)
          if (!info.open) {
              throw TaskError.Refusal(message = "#$number закрыта")
          }
          if (info.type == EPIC) {
              throw TaskError.Refusal(message = "#$number — эпик: в Groomed переводятся его подзадачи")
          }
          console.line(text = grooming.groom(number = number))
      }

      private companion object {
          const val EPIC = "type:epic"
      }
  }
  ```
  Регистрация в `Main.kt` — строка перед `"plan"` (`:61`):
  ```kotlin
              "groomed" to GroomedCommand(issues = issues, board = board),
  ```
  Справка в `TaskLauncherSpecData.kt` — строка перед `"  task plan …"` (`:27`):
  ```kotlin
              "  task groomed <N> — задача из Backlog в Groomed: груминг закончен\n" +
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GroomedCommandSpec*' --tests '*TaskLauncherSpec*'` → `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Задача 3: `task plan` переводит задачу в Groomed
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/PlanCommand.kt:12–23, 81–82` — `board`, `Grooming`, kdoc, `usage`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:61` — `board` для `PlanCommand`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/PlanCommandSpec.kt` — мок `board`, новые случаи
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt:28` — новый `usage` у `plan`

- [ ] 1. Падающий тест. В `PlanCommandSpec.kt`:
  - добавить импорты `io.github.nakulov.standards.task.board.Board` и `io.mockk.verifyOrder`;
  - после `val console = mockk<Console>()` добавить `val board = mockk<Board>()`;
  - конструктор заменить на `PlanCommand(git = git, issues = issues, board = board, directory = root)`;
  - к общим заглушкам добавить:
  ```kotlin
          justRun { board.move(item = any(), status = any()) }
          every { issues.nodeId(number = 53) } returns "I_issue53"
          every { board.item(issue = 53, nodeId = "I_issue53") } returns makeGroomedItem()
  ```
  - в `verifySequence` случая «ветки нет», после `console.line(text = "#53: тело задачи обновлено, локальный plans/commit-commands.md удалён")`, добавить:
  ```kotlin
                          issues.nodeId(number = 53)
                          board.item(issue = 53, nodeId = "I_issue53")
                          console.line(text = "#53 уже в «Groomed»")
  ```
  - новые случаи — перед `shouldThrowWithMessageTest` «план сохранён, тело задачи — нет»:
  ```kotlin
          given("задача в Backlog") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              every { board.item(issue = 53, nodeId = "I_issue53") } returns makeBacklogItem()
              `when` {
                  command.run(args = makePlanArgs(), console = console)
                  then {
                      verifyOrder {
                          issues.editBody(number = 53, body = merged)
                          board.move(item = makeBacklogItem(), status = Board.GROOMED)
                          console.line(text = "#53 → Groomed")
                      }
                  }
              }
          }

          given("статус не сменился") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              every {
                  board.item(issue = 53, nodeId = "I_issue53")
              } throws TaskError.Refusal(message = "#53 не на доске — добавьте её на доску проекта")
              `when` {
                  val error = shouldThrow<TaskError.Failure> { command.run(args = makePlanArgs(), console = console) }
                  then {
                      error.message shouldBe "план сохранён, статус — нет: task groomed 53 " +
                          "(#53 не на доске — добавьте её на доску проекта)"
                      File(root, "plans/commit-commands.md").exists() shouldBe false
                      verify { issues.editBody(number = 53, body = merged) }
                  }
              }
          }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PlanCommandSpec*'` — падает: `No parameter with name 'board' found` (конструктор `PlanCommand`)
- [ ] 3. Минимальный код в `PlanCommand.kt`:
  - импорт `io.github.nakulov.standards.task.board.Board`;
  - kdoc (`:12–16`):
  ```kotlin
  /**
   * `task plan <N> <slug> [--long] --issue-parts <файл>` (#53): утверждённый план — в ветку задачи в `origin` через
   * временный worktree (рабочая папка и ветка пользователя не трогаются), без PR; части плана — в тело задачи между
   * метками. Повтор — «Обновить план». Локальный план после сохранения удаляется. Последним шагом задача из Backlog —
   * в Groomed, другой статус не меняется (#69); сбой статуса — повтор `task groomed`.
   */
  ```
  - конструктор и `usage` (`:17–23`):
  ```kotlin
  class PlanCommand(
      private val git: Git,
      private val issues: Issues,
      board: Board,
      private val directory: File,
  ) : Command {
      private val grooming = Grooming(issues = issues, board = board)

      override val usage: String =
          "<N> <slug> [--long] --issue-parts <файл> — план plans/<slug>.md в ветку задачи; задача из Backlog — " +
              "в Groomed"
  ```
  - конец `run` (`:81–82`):
  ```kotlin
          plan.delete()
          console.line(text = "#$number: тело задачи обновлено, локальный plans/$slug.md удалён")
          val status = try {
              grooming.groom(number = number)
          } catch (error: TaskError) {
              throw TaskError.Failure(message = "план сохранён, статус — нет: task groomed $number (${error.message})")
          }
          console.line(text = status)
  ```
  - `Main.kt:61`:
  ```kotlin
              "plan" to PlanCommand(git = git, issues = issues, board = board, directory = directory),
  ```
  - `TaskLauncherSpecData.kt:27–28` (строка `plan`):
  ```kotlin
              "  task plan <N> <slug> [--long] --issue-parts <файл> — " +
              "план plans/<slug>.md в ветку задачи; задача из Backlog — в Groomed\n",
  ```
- [ ] 4. Запустить `./gradlew :scripts:test` → `BUILD SUCCESSFUL` (полный набор: `PlanCommandSpec`, `TaskLauncherSpec`, остальные)
- [ ] 5. Коммит — тест и код вместе

### Задача 4: скиллы `groom` и `decompose`
- Изменить: `skills/groom/SKILL.md:11` — исключение в «Ничего не меняешь»
- Изменить: `skills/groom/SKILL.md:28–30` — вызов `task groomed` на шаге 4
- Изменить: `skills/decompose/SKILL.md:190` — поведение `task plan`

Тест не применим: это текст скиллов, проверки формата у них нет.

- [ ] 1. `skills/groom/SKILL.md:11` — заменить «исключение — свой журнал `plans/journal/groom-<N>.md`.» на:
  ```
  исключения — свой журнал `plans/journal/groom-<N>.md` и статус своей задачи командой `task groomed` (шаг 4).
  ```
- [ ] 2. `skills/groom/SKILL.md` — шаг 4: после строки `:30` («**сразу в работу** — …») добавить абзац:
  ```

     Выбор сделан — вызови `task groomed <N>` и покажи его ответ до «Продолжи в новой сессии…» или до работы: задача переходит из Backlog в Groomed, иначе `task start` откажет. Тема без задачи — не вызывай. Отказ (закрыта, эпик) — покажи ответ и продолжай шаг 4.
  ```
- [ ] 3. `skills/decompose/SKILL.md:190` — заменить «в `origin` без смены статуса и без PR;» на:
  ```
  в `origin` без PR; задачу из Backlog переводит в Groomed, другой статус не трогает — ответ команды покажи;
  ```
- [ ] 4. Проверить: `grep -c 'task groomed' skills/groom/SKILL.md` → `2`; `grep -n 'без смены статуса' skills/decompose/SKILL.md` → пусто
- [ ] 5. Коммит

### Приёмка
- [ ] Что даёт этот PR — проверено: `task groomed` и `task plan` переводят задачу из Backlog в Groomed (тесты задач 1–3)
- [ ] Крайние случаи обработаны: задача уже в Groomed или дальше, закрытая, эпик, неверные аргументы, сбой доски в `task plan`
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`
- [ ] Критерии цели из «Обзора» — по пунктам: 1–3 → задачи 1–2, 4–5 → задача 3, 6–7 → задача 4
- [ ] Документация: строки скиллов (задача 4), kdoc `PlanCommand`; `grep -rn 'без смены статуса' --exclude-dir=build --exclude-dir=.git --exclude-dir=plans .` → пусто

## Открыто
- нет

## После завершения
- Ручная проверка:
  1. Задача в Backlog → `task groomed <N>` → на доске Groomed → `task start <N> <slug>` проходит без «сначала груминг».
  2. `/decompose` на задаче в Backlog → после `task plan` задача в Groomed.
