# Команды коммита

**Цель:** команды `task commit` и `task plan` и шаг «Убрать план» в `task pr`/`task final` вместо временных git-блоков в скиллах.
**Архитектура:** новые команды и шаги встраиваются в существующие слои `scripts/src/main/kotlin/io/github/nakulov/standards/task/`: `command/` (команды, архив плана), `github/` (тело задачи по меткам), `git/` (новые методы `Git`). `task plan` коммитит план через временный ворктри, не переключая ветку пользователя; архив плана — общий шаг `Publisher` при PR в `dev`.
**Стек:** Kotlin 2.2 (`languageVersion`), Kotest `BehaviorSpec` + `kotestkit`, MockK, `git`, `gh`.

---

## Ограничения
- Не трогать: `drafts/`, `.claude/` — вне git (`.git/info/exclude`), правка после слияния (#53, «Вручную»).
- Не трогать: поведение `task start`, `pr`, `done`, `final`, `stage`, `new` — кроме шага архива в `Publisher`.
- Только Kotlin ≤ 2.2 — `scripts/task:8`, `-language-version 2.2` (#13, решение 1).
- `git` — только через методы `Git`, `gh` — только через `GitHub`/`Issues`.
- Без новых зависимостей.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: `task commit --message-file <файл>`, `task plan <N> <slug> [--long] --issue-parts <файл>`, шаг `PlanArchive` в `Publisher`.
- Какую проблему решает, что даёт: агенты коммитят по шаблону через команду, а не руками; план сохраняется на ветку без переключения и без смены статуса; план не попадает в `dev`.
- Как встраивается: команды регистрируются в `cli/Main.kt`; `Publisher` общий для `task pr` и `task final`.
- Критерии цели:
  - `./gradlew :scripts:test` проходит.
  - Спека `task plan`: ветки нет → ветка только в `origin` от `origin/dev`, «Добавить план: …», статус не меняется; ветка есть → «Обновить план: …»; локальная отстала → перемотка (и когда план не изменился), расходится или не отправлена → отказ, открыта в папке → отказ с путём; закрытая или эпик → отказ; временный ворктри удаляется и при ошибке; локальный `plans/<slug>.md` удаляется.
  - Спека тела задачи: метки дописываются; при повторе содержимое между ними заменяется, вне меток не меняется; сломанные метки или раздела нет → отказ с названием раздела.
  - Спека `task commit`: только перечисленные файлы, сообщение по шаблону; файл не изменён → отказ.
  - Спеки `task pr`/`task final`: `plans/*.md` в разнице с `dev` → копия в `plans/done/` основной папки, удаление, «Убрать план: …»; `--part` и ветка без плана — без коммита.

## Контекст
- Задействованные файлы:
  - `…/task/git/Git.kt` — все вызовы `git` через `Shell`; `read` (`:113`) обрезает вывод и бросает `TaskError.Failure`.
  - `…/task/command/Publisher.kt:19` — `publish(draft, console)`: чистота → проверки → push → PR.
  - `…/task/command/PrCommand.kt:40` — `base = git.base(...) ?: "dev"`; `FinalCommand.kt:37` — `base = "dev"`.
  - `…/task/command/StartCommand.kt:70-71` — отказ закрытой задаче и эпику.
  - `…/task/github/Issues.kt` — `gitHub.read`/`gitHub.write`, тела задачи нет.
  - `…/task/cli/Main.kt:36-49` — карта команд.
  - `…/task/cli/TaskError.kt` — `Refusal` (1), `Failure` (2).
- Паттерны: спека + `…SpecData.kt` (функции `make…`/`read…`); команды — `mockk` + `verifySequence` (`StartCommandSpec`); `Git` — настоящий `git` во временных репозиториях (`GitSpec`, «вне стандарта», #13 решение 7); разбор текста — объект без вызовов (`PrBody`, `StageBody`); именованные аргументы везде.
- Зависимости: блоки выполняются по порядку. PR 4 опирается на `IssueParts`, `Issues.body/editBody` (PR 3), `commonDir()` и `makeCloneWithPlanBranch()` (PR 2), `directory` в `Main.kt` (PR 1); PR 2 от PR 1 не зависит.

## Подход к разработке
- Завершай задачу целиком до следующей.
- Небольшие сфокусированные изменения.
- **ВАЖНО:** каждая задача — с новыми или обновлёнными тестами для её кода.
- **ВАЖНО:** все тесты зелёные до начала следующей задачи.
- **ВАЖНО:** изменение объёма — в этот план, не молча.
- После каждого изменения — конкретный тест; полный набор — в конце задачи.
- Строки длиннее 120 знаков (`.editorconfig:11`) в коде плана переносятся при вставке по стилю соседнего кода (именованные аргументы — по строке); логика не меняется.
- Обратная совместимость — нужна для существующих команд (ограничение 2); `Publisher` получает новый параметр конструктора — вызов в `Main.kt` и спека обновляются в той же задаче.

## Структура файлов
Корень кода — `scripts/src/main/kotlin/io/github/nakulov/standards/task/`, тестов — `scripts/src/test/kotlin/io/github/nakulov/standards/task/`.

Отвечает за коммит по шаблону (PR 1):
- Изменить: `git/Git.kt` — `changedPaths(): List<String>`, `commitPaths(paths, messageFile)`
- Изменить: `git/GitSpec.kt`, `git/GitSpecData.kt` — изменённые пути, коммит только путей
- Создать: `command/CommitMessage.kt` — разбор и проверка шаблона; вход `CommitMessage.parse(text): CommitMessage`
- Создать: `command/CommitMessageSpec.kt`, `CommitMessageSpecData.kt` — шаблон верный / нет
- Создать: `command/CommitCommand.kt` — `task commit`; вход `CommitCommand(git, directory)`
- Создать: `command/CommitCommandSpec.kt`, `CommitCommandSpecData.kt` — пути из «Файлов» + план, отказы
- Изменить: `cli/Main.kt` — `"commit"`

Отвечает за архив плана (PR 2):
- Изменить: `git/Git.kt` — `commonDir()`, `mainRoot()`, `addedPaths(base)`, `removeAndCommit(paths, message)`
- Изменить: `git/GitSpec.kt`, `git/GitSpecData.kt` — основная папка из ворктри, добавленные пути, удаление
- Создать: `command/PlanArchive.kt` — «Убрать план»; вход `PlanArchive(git).run(console)`
- Создать: `command/PlanArchiveSpec.kt`, `PlanArchiveSpecData.kt` — копия, коммит, без плана — ничего
- Изменить: `command/Publisher.kt:13` — параметр `archive`, вызов при PR в `dev` со связью `Closes`
- Изменить: `command/PublisherSpec.kt`, `PublisherSpecData.kt` — архив в последовательности; PR в long — без архива
- Изменить: `cli/Main.kt` — `Publisher(…, archive = PlanArchive(git = git))`

Отвечает за тело задачи (PR 3):
- Создать: `github/IssueParts.kt` — раскладка частей между метками; вход `IssueParts.merge(body, parts, planLine): String`
- Создать: `github/IssuePartsSpec.kt`, `IssuePartsSpecData.kt` — дописать, заменить, вне меток, отказы
- Изменить: `github/Issues.kt` — `body(number)`, `editBody(number, body)`
- Изменить: `github/IssuesSpec.kt`, `IssuesSpecData.kt` — аргументы `gh`

Отвечает за сохранение плана (PR 4):
- Изменить: `git/Git.kt` — `openedAt`, `isAncestor`, `addWorktree`, `removeWorktree`, `hasChanges`, `commitAll`, `pushHead`, `head`, `moveBranch`
- Изменить: `git/GitSpec.kt`, `git/GitSpecData.kt` — ворктри, push ссылки, перемотка
- Создать: `command/PlanCommand.kt` — `task plan`; вход `PlanCommand(git, issues, directory)`
- Создать: `command/PlanCommandSpec.kt`, `PlanCommandSpecData.kt` — поток, повтор, отказы, уборка
- Изменить: `cli/Main.kt` — `"plan"`

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: временный ворктри `--detach` для `task plan`; метки `<!-- план -->` в теле задачи по файлу частей от скилла; архив плана в `Publisher` при PR в `dev`; `task commit --message-file`.
- Решения:
  - пути коммита — из «Файлов:» сообщения, изменённый `plans/*.md` добавляется сам — один источник правды, отметки `[x]` без шума (#53, решение 2);
  - `git commit --file <файл> -- <пути>` — в коммит не попадает чужое, уже добавленное в индекс;
  - команды во временном ворктри — `git -C <папка>` через `Git`, не второй `Git` — `PlanCommand` тестируется одной подменой `Git`;
  - проверка «файл плана» (`plans/<имя>.md` без подпапок) — дублируется одной строкой в `CommitCommand` и `PlanArchive`: две строки проще общего файла ради одной функции;
  - тело задачи собирается до git-действий, пишется после push (#53, решение 4).
- Как вписывается в систему: `task start` берёт ветку из `origin` (`StartCommand.kt:90`), поэтому `task plan` создаёт её только в `origin`.
- Запасной: разбор плана командой — переходим, если шаблон плана устоялся, скиллы в git, а модель путает таблицу переноса.
- Отклонено:
  - исключение для файла плана + переключение веток — с правками откажет (stash), при сбое пользователь не на своей ветке;
  - повтор `task plan` — отказ — отдельный путь правки с переключением;
  - отдельный раздел «План» в теле — ломает таблицу переноса;
  - разбор плана командой — шаблон ещё обсуждается, отбор по смыслу (остаётся запасным);
  - `task commit --remove-plan`, `task plan done` — риск забыть шаг;
  - архив после проверок (ревью плана, М4) — проверки идут по состоянию ветки, которое уйдёт в PR (дизайн, поток данных); при упавшей проверке план не потерян — копия в `plans/done/` основной папки.

## Технические детали
- Шаблон сообщения `task commit`:
  ```
  <что сделано — повелительно>

  Задача плана: <заголовок секции>
  Файлы:
  - `путь` — что изменено
  ```
- Файл частей `--issue-parts`: разделы `## <название раздела задачи>` (`## Отклонено`, `## Автоматически`, `## Вручную`, `## Решение`, `## Не входит в задачу`, `## Открытые вопросы`, `## Объём изменений`). В задаче раздел — строка `## <название>` или `**<название>:**`; конец раздела — следующая такая строка.
- Раздел «Решение» в задаче — таблица; части для него — текстовые строки под таблицей (по таблице переноса — «Запасной: … — переходим, если …»). Строки таблицы в файл частей не кладутся: после метки они не продолжают таблицу (ревью плана, В2).
- Метки: `<!-- план -->` … `<!-- /план -->`, каждая — отдельной строкой. Строка «План: ветка `<ветка>`, `plans/<slug>.md`» — в метках перед первой строкой `## `.
- Временный ворктри — `<git-common-dir>/task-plan/<slug>`: внутри `.git`, вне рабочей папки.
- Поток `task plan`: аргументы → план и строка `# ` → файл частей → `info` (закрыта / эпик) → `IssueParts.merge` → `fetch` → `openedAt` → проверка локальной → ворктри: копия, коммит, push → удаление ворктри (`finally`) → перемотка локальной → `editBody` → удаление локального плана.
- Поток `Publisher`: чистота → `PlanArchive` (только PR в `dev` со связью `Closes`, без `--part`) → проверки → push → PR.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: исполнитель коммитит по шаблону командой `task commit`
  - [x] Задача 1: изменённые пути и коммит путей в `Git`
  - [x] Задача 2: разбор сообщения `CommitMessage`
  - [x] Задача 3: команда `task commit`
  - [ ] ➕ Задача 11: удаление и переименование из индекса в `task commit`
  - [ ] Приёмка
  - [ ] Документация
- PR 2: план не попадает в `dev`
  - [ ] Задача 4: основная папка, добавленные пути, удаление в `Git`
  - [ ] Задача 5: `PlanArchive`
  - [ ] Задача 6: архив в `Publisher`
  - [ ] Приёмка
  - [ ] Документация
- PR 3: части плана записываются в тело задачи без дублей
  - [ ] Задача 7: `IssueParts`
  - [ ] Задача 8: тело задачи в `Issues`
  - [ ] Приёмка
  - [ ] Документация
- PR 4: утверждённый план сохраняется командой `task plan`
  - [ ] Задача 9: ворктри, push ссылки, перемотка в `Git`
  - [ ] Задача 10: команда `task plan`
  - [ ] Приёмка
  - [ ] Документация

**После завершения** (внешнее, без чекбоксов):
- После слияния и `git pull` в основной папке: временные блоки убраны, вызовы переписаны на `task plan … --issue-parts`, `task commit --message-file` (переименование — оба пути в «Файлах»), шаг «убери план перед PR» удалён — в `drafts/plan/SKILL.md`, `drafts/execute/SKILL.md`, `drafts/agents/plan-executor.md`, копии в `.claude/`; `grep -rn "пока нет (временно)" drafts .claude` пуст.
- Живой прогон на первой задаче, которую ведём через план (скорее всего #52).

## PR 1: исполнитель коммитит по шаблону командой `task commit`
**Объём:** `git/Git.kt`, `GitSpec.kt`, `GitSpecData.kt`, `command/CommitMessage.kt` + Spec + SpecData, `command/CommitCommand.kt` + Spec + SpecData, `cli/Main.kt`; ≈ 10 файлов

### Задача 1: изменённые пути и коммит путей в `Git`
- Изменить: `git/Git.kt` — `changedPaths()`, `commitPaths(paths, messageFile)`
- Изменить: `git/GitSpecData.kt` — `makeCloneWithChanges()`, `makeMessageFile(directory)`
- Изменить: `git/GitSpec.kt` — два случая

- [x] 1. Падающий тест: изменённые пути и коммит только перечисленных.
  В `GitSpecData.kt` (рядом с `makeCloneBehindOrigin`):
  ```kotlin
  /** Клон с личностью в настройке: `a.txt` изменён, `b.txt` удалён, `c.txt` и `plans/x.md` — новые. */
  fun makeCloneWithChanges(): File {
      val clone = makeClone()
      git(directory = clone, args = listOf("config", "user.name", "task"))
      git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
      File(clone, "a.txt").writeText("1\n")
      File(clone, "b.txt").writeText("1\n")
      git(directory = clone, args = listOf("add", "a.txt", "b.txt"))
      git(directory = clone, args = listOf("commit", "--quiet", "--message", "файлы"))
      File(clone, "a.txt").writeText("2\n")
      File(clone, "b.txt").delete()
      File(clone, "c.txt").writeText("новый\n")
      File(clone, "plans").mkdirs()
      File(clone, "plans/x.md").writeText("# План X\n")
      return clone
  }

  /** Файл сообщения коммита рядом с клоном. */
  fun makeMessageFile(directory: File): String {
      val file = File(directory.parentFile, "message.txt")
      file.writeText("Добавить c\n\nЗадача плана: Задача 1\nФайлы:\n- `a.txt` — изменён\n- `c.txt` — создан\n")
      return file.path
  }
  ```
  В `GitSpec.kt` (в конец `BehaviorSpec`):
  ```kotlin
  shouldReturnTest(listOf("a.txt", "b.txt", "c.txt", "plans/x.md"), options = TestOptions(name = "изменённые пути")) {
      Git(shell = ProcessShell(directory = makeCloneWithChanges())).changedPaths().sorted()
  }

  shouldReturnTest(listOf("Добавить c", "b.txt", "plans/x.md"), options = TestOptions(name = "коммит только путей")) {
      val clone = makeCloneWithChanges()
      val git = Git(shell = ProcessShell(directory = clone))
      git.commitPaths(paths = listOf("a.txt", "c.txt"), messageFile = makeMessageFile(directory = clone))
      listOf(readHeadSubject(directory = clone)) + git.changedPaths().sorted()
  }

  shouldReturnTest("Добавить c", options = TestOptions(name = "коммит путей из подпапки")) {
      val clone = makeCloneWithChanges()
      Git(shell = ProcessShell(directory = File(clone, "plans")))
          .commitPaths(paths = listOf("a.txt", "c.txt"), messageFile = makeMessageFile(directory = clone))
      readHeadSubject(directory = clone)
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: `Unresolved reference 'changedPaths'`
- [x] 3. Минимальный код в `Git.kt` (после `firstCommit`, константы — в `companion object`):
  ```kotlin
  /** Изменённые пути рабочей папки: изменённые, удалённые, новые; переименование — новым путём. */
  fun changedPaths(): List<String> {
      val args = listOf("status", "--porcelain", "-z", "--untracked-files=all")
      val result = run(args = args)
      if (result.code != 0) {
          throw TaskError.Failure(message = "git status: ${result.err.trim().lineSequence().first()}")
      }
      val entries = result.out.split(NUL).filter { entry -> entry.isNotEmpty() }
      val paths = mutableListOf<String>()
      var index = 0
      while (index < entries.size) {
          val entry = entries[index]
          paths += entry.substring(startIndex = STATUS_PREFIX)
          index += if (entry.first() == RENAMED || entry.first() == COPIED) 2 else 1
      }
      return paths
  }

  /**
   * Коммит только [paths] с сообщением из [messageFile]: чужое из индекса в коммит не попадает. Пути — от корня
   * репозитория (`:/`, как в `git status --porcelain`): команда работает и из подпапки.
   */
  fun commitPaths(
      paths: List<String>,
      messageFile: String,
  ) {
      val fromRoot = paths.map { path -> "$FROM_ROOT$path" }
      read(args = listOf("add", "--") + fromRoot)
      read(args = listOf("commit", "--quiet", "--file", messageFile, "--") + fromRoot)
  }
  ```
  ```kotlin
  const val NUL = '\u0000'
  const val STATUS_PREFIX = 3
  const val RENAMED = 'R'
  const val COPIED = 'C'
  const val FROM_ROOT = ":/"
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — зелёный
- [x] 5. Коммит — тест и код вместе

### Задача 2: разбор сообщения `CommitMessage`
- Создать: `command/CommitMessage.kt` — разбор и проверка шаблона
- Создать: `command/CommitMessageSpec.kt`, `CommitMessageSpecData.kt` — случаи шаблона

- [x] 1. Падающий тест.
  `CommitMessageSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  fun makeCommitText(): String {
      return "Добавить task commit\n\nЗадача плана: Задача 3: команда task commit\nФайлы:\n" +
          "- `scripts/a.kt` — команда\n- `scripts/b.kt` — тест\n"
  }

  fun makeCommitMessage(): CommitMessage {
      return CommitMessage(
          title = "Добавить task commit",
          planTask = "Задача 3: команда task commit",
          files = listOf("scripts/a.kt", "scripts/b.kt"),
      )
  }

  fun makeNoTitleText(): String {
      return "\nЗадача плана: Задача 3\nФайлы:\n- `a.kt` — код\n"
  }

  fun makeNoPlanTaskText(): String {
      return "Добавить\n\nФайлы:\n- `a.kt` — код\n"
  }

  fun makeNoFilesText(): String {
      return "Добавить\n\nЗадача плана: Задача 3\nФайлы:\n"
  }
  ```
  `CommitMessageSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.standards.task.cli.TaskError
  import io.kotest.core.spec.style.BehaviorSpec

  class CommitMessageSpec :
      BehaviorSpec({
          shouldReturnTest(makeCommitMessage()) {
              CommitMessage.parse(text = makeCommitText())
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("первая строка сообщения пуста — напишите, что сделано") {
              CommitMessage.parse(text = makeNoTitleText())
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("нет строки «Задача плана: …» — добавьте заголовок задачи плана") {
              CommitMessage.parse(text = makeNoPlanTaskText())
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "раздел «Файлы:» пуст — перечислите файлы строками «- `путь` — что изменено»",
          ) {
              CommitMessage.parse(text = makeNoFilesText())
          }
      })
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*CommitMessageSpec*'` — падает: `Unresolved reference 'CommitMessage'`
- [x] 3. Минимальный код, `command/CommitMessage.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.cli.TaskError

  /**
   * Сообщение `task commit` по шаблону задачи плана (#53): заголовок, «Задача плана: …», «Файлы:» — пути для коммита.
   */
  data class CommitMessage(
      val title: String,
      val planTask: String,
      val files: List<String>,
  ) {
      companion object {
          private const val PLAN_TASK = "Задача плана:"
          private const val FILES = "Файлы:"
          private val FILE_LINE = Regex(pattern = "^- `([^`]+)` — .+$")

          fun parse(text: String): CommitMessage {
              val lines = text.lines()
              val title = lines.first().trim()
              if (title.isEmpty()) {
                  throw TaskError.Refusal(message = "первая строка сообщения пуста — напишите, что сделано")
              }
              val planTask = lines.firstOrNull { line -> line.startsWith(prefix = PLAN_TASK) }
                  ?.removePrefix(prefix = PLAN_TASK)
                  ?.trim()
                  .orEmpty()
              if (planTask.isEmpty()) {
                  throw TaskError.Refusal(message = "нет строки «Задача плана: …» — добавьте заголовок задачи плана")
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
- [x] 4. Запустить `./gradlew :scripts:test --tests '*CommitMessageSpec*'` — зелёный
- [x] 5. Коммит — тест и код вместе

### Задача 3: команда `task commit`
- Создать: `command/CommitCommand.kt` — `task commit`
- Создать: `command/CommitCommandSpec.kt`, `CommitCommandSpecData.kt` — пути из «Файлов» + план, отказы
- Изменить: `cli/Main.kt:48` — `"commit" to CommitCommand(…)`

- [x] 1. Падающий тест.
  `CommitCommandSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import java.io.File
  import java.nio.file.Files

  /** Папка запуска с `message.txt` по шаблону; файлы: `scripts/a.kt`, `scripts/b.kt`. */
  fun makeCommitDirectory(): File {
      val base = File(System.getProperty("task.cache"), "commit")
      base.mkdirs()
      val directory = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
      File(directory, "message.txt").writeText(makeCommitText())
      return directory
  }
  ```
  `CommitCommandSpec.kt`:
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
          justRun { console.line(text = any()) }
          justRun { git.commitPaths(paths = any(), messageFile = any()) }

          given("файлы изменены, план отмечен") {
              every { git.changedPaths() } returns
                  listOf("scripts/a.kt", "scripts/b.kt", "plans/commit-commands.md", "other.txt")
              `when` {
                  command.run(args = listOf("--message-file", "message.txt"), console = console)
                  then {
                      verifySequence {
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

          shouldThrowWithMessageTest<TaskError.Refusal>("`scripts/b.kt` не изменён или не существует — уберите из «Файлов»") {
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
- [x] 2. Запустить `./gradlew :scripts:test --tests '*CommitCommandSpec*'` — падает: `Unresolved reference 'CommitCommand'`
- [x] 3. Минимальный код, `command/CommitCommand.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.cli.Command
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.Git
  import java.io.File

  /**
   * `task commit --message-file <файл>` (#53): коммит по шаблону задачи плана. Пути — из раздела «Файлы:» сообщения;
   * изменённый план `plans/<имя>.md` добавляется сам — отметки `[x]` идут в коммит задачи.
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
          val message = CommitMessage.parse(text = file.readText())
          val changed = git.changedPaths()
          val unchanged = message.files.firstOrNull { path -> path !in changed }
          if (unchanged != null) {
              throw TaskError.Refusal(message = "`$unchanged` не изменён или не существует — уберите из «Файлов»")
          }
          val plans = changed.filter { path -> isPlan(path = path) && path !in message.files }
          git.commitPaths(paths = message.files + plans, messageFile = file.path)
          console.line(text = "коммит: ${message.title}")
      }

      private fun isPlan(path: String): Boolean {
          return path.startsWith(prefix = "plans/") && path.endsWith(suffix = ".md") && path.count { char -> char == '/' } == 1
      }

      private companion object {
          const val MESSAGE_FILE = "--message-file"
      }
  }
  ```
  В `cli/Main.kt`: `import io.github.nakulov.standards.task.command.CommitCommand`; в начале `main` (`Main.kt:25`) — `val directory = File(System.getProperty("user.dir"))` и `ProcessShell(directory = directory)`; в карту после `"new"`:
  ```kotlin
  "commit" to CommitCommand(git = git, directory = directory),
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*CommitCommandSpec*'` — зелёный; `./gradlew :scripts:test` — `BUILD SUCCESSFUL`
- [x] 5. Коммит — тест и код вместе

### ➕ Задача 11: удаление и переименование из индекса в `task commit`
Добавлена с согласия пользователя после приёмки PR 1: `git rm` — сбой `git add` (pathspec не найден), `git mv` — коммит наполовину.
- Изменить: `git/Git.kt` — `changedPaths()` с `--no-renames`; `commitPaths` добавляет в индекс только пути, которые есть в рабочей папке
- Изменить: `git/GitSpecData.kt` — `makeCloneWithIndexChanges()`
- Изменить: `git/GitSpec.kt` — три случая

- [ ] 1. Падающий тест.
  `GitSpecData.kt`:
  ```kotlin
  /** Клон с личностью: `a.txt` удалён через `git rm`, `r.txt` переименован через `git mv` в `r2.txt`. */
  fun makeCloneWithIndexChanges(): File {
      val clone = makeClone()
      git(directory = clone, args = listOf("config", "user.name", "task"))
      git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
      File(clone, "a.txt").writeText("1\n")
      File(clone, "r.txt").writeText("1\n")
      git(directory = clone, args = listOf("add", "a.txt", "r.txt"))
      git(directory = clone, args = listOf("commit", "--quiet", "--message", "файлы"))
      git(directory = clone, args = listOf("rm", "--quiet", "a.txt"))
      git(directory = clone, args = listOf("mv", "r.txt", "r2.txt"))
      return clone
  }
  ```
  `GitSpec.kt`:
  ```kotlin
  shouldReturnTest(
      listOf("a.txt", "r.txt", "r2.txt"),
      options = TestOptions(name = "изменённые пути: git rm и git mv — каждый путь"),
  ) {
      Git(shell = ProcessShell(directory = makeCloneWithIndexChanges())).changedPaths().sorted()
  }

  shouldReturnTest(emptyList<String>(), options = TestOptions(name = "коммит удаления и переименования из индекса")) {
      val clone = makeCloneWithIndexChanges()
      val git = Git(shell = ProcessShell(directory = clone))
      git.commitPaths(paths = listOf("a.txt", "r.txt", "r2.txt"), messageFile = makeMessageFile(directory = clone))
      git.changedPaths()
  }

  shouldReturnTest(listOf("c.txt", "plans/x.md"), options = TestOptions(name = "коммит удаления без git rm")) {
      val clone = makeCloneWithChanges()
      val git = Git(shell = ProcessShell(directory = clone))
      git.commitPaths(paths = listOf("a.txt", "b.txt"), messageFile = makeMessageFile(directory = clone))
      git.changedPaths().sorted()
  }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: первый случай — `r2.txt` вместо `r.txt, r2.txt`; второй — `TaskError.Failure: git add: fatal: pathspec ':/a.txt' did not match any files`
- [ ] 3. Минимальный код в `Git.kt`. В `changedPaths` — `listOf("status", "--porcelain", "-z", "--no-renames", "--untracked-files=all")`, KDoc: «…; переименование — двумя путями: старый удалён, новый добавлен». `commitPaths`:
  ```kotlin
  fun commitPaths(
      paths: List<String>,
      messageFile: String,
  ) {
      val root = File(root())
      val present = paths.filter { path -> File(root, path).exists() }.map { path -> "$FROM_ROOT$path" }
      if (present.isNotEmpty()) {
          read(args = listOf("add", "--") + present)
      }
      read(args = listOf("commit", "--quiet", "--file", messageFile, "--") + paths.map { path -> "$FROM_ROOT$path" })
  }
  ```
  KDoc `commitPaths` дополнить: «В индекс добавляются только пути, которые есть в рабочей папке; удалённые (и через `git rm`) коммит берёт сам».
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — зелёный; `./gradlew :scripts:test` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [x] Что даёт этот PR — проверено: `scripts/task commit --message-file <файл>` в черновой ветке коммитит только перечисленное
- [ ] Крайние случаи обработаны: файл не изменён, удалённый файл, новый файл, план в подпапке `plans/done/` не добавляется
- [x] Полный набор тестов зелёный
- [x] Последний блок: критерии цели из «Обзора» — по пунктам (не этот блок)

### Документация
- [ ] README, `docs/` — не нужно: команды описывает #34
- [ ] Новые паттерны — в файл проекта и карту: нет

## PR 2: план не попадает в `dev`
**Объём:** `git/Git.kt`, `GitSpec.kt`, `GitSpecData.kt`, `command/PlanArchive.kt` + Spec + SpecData, `command/Publisher.kt`, `PublisherSpec.kt`, `PublisherSpecData.kt`, `cli/Main.kt`; ≈ 10 файлов

### Задача 4: основная папка, добавленные пути, удаление в `Git`
- Изменить: `git/Git.kt` — `commonDir()`, `mainRoot()`, `addedPaths(base)`, `removeAndCommit(paths, message)`
- Изменить: `git/GitSpecData.kt` — `makeCloneWithPlanBranch()`
- Изменить: `git/GitSpec.kt` — три случая

- [ ] 1. Падающий тест.
  `GitSpecData.kt`:
  ```kotlin
  /** Клон с личностью на ветке `task/7-p`: в ней добавлен `plans/p.md` («# План П»). */
  fun makeCloneWithPlanBranch(): File {
      val clone = makeClone()
      git(directory = clone, args = listOf("config", "user.name", "task"))
      git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
      git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/7-p"))
      File(clone, "plans").mkdirs()
      File(clone, "plans/p.md").writeText("# План П\n")
      git(directory = clone, args = listOf("add", "plans/p.md"))
      git(directory = clone, args = listOf("commit", "--quiet", "--message", "Добавить план: П"))
      return clone
  }
  ```
  `GitSpec.kt`:
  ```kotlin
  shouldReturnTest(true, options = TestOptions(name = "основная папка из worktree")) {
      val worktree = makeWorktree()
      val main = Git(shell = ProcessShell(directory = worktree)).mainRoot()
      File(main).canonicalPath == File(worktree.parentFile, "clone").canonicalPath
  }

  shouldReturnTest(listOf("plans/p.md"), options = TestOptions(name = "добавленные в ветке пути")) {
      Git(shell = ProcessShell(directory = makeCloneWithPlanBranch())).addedPaths(base = "origin/dev")
  }

  shouldReturnTest(listOf("Убрать план: П"), options = TestOptions(name = "удаление и коммит")) {
      val clone = makeCloneWithPlanBranch()
      Git(shell = ProcessShell(directory = clone)).removeAndCommit(paths = listOf("plans/p.md"), message = "Убрать план: П")
      listOf(readHeadSubject(directory = clone)) + readHeadFiles(directory = clone).filter { path -> path.startsWith(prefix = "plans/") }
  }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: `Unresolved reference 'mainRoot'`
- [ ] 3. Минимальный код в `Git.kt` (`import java.io.File`):
  ```kotlin
  /** Общий `.git` — у основной папки и всех её worktree один. */
  fun commonDir(): String {
      return read(args = listOf("rev-parse", "--path-format=absolute", "--git-common-dir"))
  }

  /** Основная папка проекта — и из worktree: там лежат `plans/done/` и `plans/journal/`. */
  fun mainRoot(): String {
      return File(commonDir()).parentFile.path
  }

  /** Файлы, добавленные в ветке относительно общего предка с [base]. */
  fun addedPaths(base: String): List<String> {
      return read(args = listOf("diff", "--name-only", "--diff-filter=A", "$base...HEAD"))
          .lines()
          .filter { line -> line.isNotBlank() }
  }

  /** Удалить [paths] из ветки и закоммитить с [message]. */
  fun removeAndCommit(
      paths: List<String>,
      message: String,
  ) {
      read(args = listOf("rm", "--quiet", "--") + paths)
      read(args = listOf("commit", "--quiet", "--message", message))
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Задача 5: `PlanArchive`
- Создать: `command/PlanArchive.kt` — «Убрать план»
- Создать: `command/PlanArchiveSpec.kt`, `PlanArchiveSpecData.kt` — копия и коммит; плана нет — ничего

- [ ] 1. Падающий тест.
  `PlanArchiveSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import java.io.File
  import java.nio.file.Files

  /** Папка ветки с `plans/commit-commands.md` и пустая основная папка — во временной папке модуля. */
  fun makeArchiveFolders(): Pair<File, File> {
      val base = File(System.getProperty("task.cache"), "archive")
      base.mkdirs()
      val root = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
      val branch = File(root, "worktree")
      File(branch, "plans").mkdirs()
      File(branch, "plans/commit-commands.md").writeText("# Команды коммита\n\n- [x] Задача 1\n")
      val main = File(root, "main")
      main.mkdirs()
      return branch to main
  }
  ```
  `PlanArchiveSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.then
  import io.github.nakulov.kotestkit.`when`
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.git.Git
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.kotest.matchers.shouldBe
  import io.mockk.every
  import io.mockk.justRun
  import io.mockk.mockk
  import io.mockk.verifySequence
  import java.io.File

  class PlanArchiveSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val git = mockk<Git>()
          val console = mockk<Console>()
          val (branch, main) = makeArchiveFolders()
          val archive = PlanArchive(git = git)
          justRun { console.line(text = any()) }
          justRun { git.removeAndCommit(paths = any(), message = any()) }
          every { git.root() } returns branch.path
          every { git.mainRoot() } returns main.path

          given("в ветке добавлен план") {
              every { git.addedPaths(base = "origin/dev") } returns
                  listOf("scripts/a.kt", "plans/commit-commands.md", "plans/done/old.md")
              `when` {
                  archive.run(console = console)
                  then {
                      File(main, "plans/done/commit-commands.md").readText() shouldBe "# Команды коммита\n\n- [x] Задача 1\n"
                      verifySequence {
                          git.addedPaths(base = "origin/dev")
                          git.root()
                          git.mainRoot()
                          git.removeAndCommit(paths = listOf("plans/commit-commands.md"), message = "Убрать план: Команды коммита")
                          console.line(text = "план убран из ветки, копия — ${File(main, "plans/done").path}")
                      }
                  }
              }
          }

          given("плана в ветке нет") {
              every { git.addedPaths(base = "origin/dev") } returns listOf("scripts/a.kt")
              `when` {
                  archive.run(console = console)
                  then {
                      verifySequence { git.addedPaths(base = "origin/dev") }
                  }
              }
          }
      })
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PlanArchiveSpec*'` — падает: `Unresolved reference 'PlanArchive'`
- [ ] 3. Минимальный код, `command/PlanArchive.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.git.Git
  import java.io.File

  /**
   * «Убрать план» перед PR в `dev` (#53): план ветки копируется в `plans/done/` основной папки (в `.gitignore` —
   * остаётся у пользователя) и удаляется из ветки коммитом «Убрать план: <название>». Иначе план попадёт в `dev`.
   */
  class PlanArchive(
      private val git: Git,
  ) {
      fun run(console: Console) {
          val plans = git.addedPaths(base = BASE).filter { path -> isPlan(path = path) }
          if (plans.isEmpty()) {
              return
          }
          val root = File(git.root())
          val done = File(git.mainRoot(), DONE)
          done.mkdirs()
          val titles = plans.map { path ->
              val source = File(root, path)
              source.copyTo(target = File(done, source.name), overwrite = true)
              title(source = source)
          }
          git.removeAndCommit(paths = plans, message = "Убрать план: ${titles.joinToString(separator = "; ")}")
          console.line(text = "план убран из ветки, копия — ${done.path}")
      }

      private fun isPlan(path: String): Boolean {
          return path.startsWith(prefix = "plans/") && path.endsWith(suffix = ".md") && path.count { char -> char == '/' } == 1
      }

      private fun title(source: File): String {
          return source.readLines().firstOrNull { line -> line.startsWith(prefix = "# ") }
              ?.removePrefix(prefix = "# ")
              ?.trim()
              ?: source.nameWithoutExtension
      }

      private companion object {
          const val BASE = "origin/dev"
          const val DONE = "plans/done"
      }
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*PlanArchiveSpec*'` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Задача 6: архив в `Publisher`
- Изменить: `command/Publisher.kt:13` — параметр `archive: PlanArchive`, вызов после проверки чистоты при `draft.base == "dev"` и связи `Closes #…`: `task pr --part` в `dev` оставляет базу `dev`, меняет только связь на `Part of` (`PrCommand.kt:40-41`)
- Изменить: `command/PublisherSpec.kt` — `archive` в трёх `verifySequence`, новые `given`: PR в long-ветку, `--part` в `dev`
- Изменить: `command/PublisherSpecData.kt` — `makePartDraft()`
- Изменить: `cli/Main.kt:34` — `Publisher(…, archive = PlanArchive(git = git))`

- [ ] 1. Падающий тест.
  `PublisherSpecData.kt`:
  ```kotlin
  /** Черновик блока PR в long-ветку — архив плана не нужен. */
  fun makePartDraft(): PullRequestDraft {
      return makeParsedDraft().copy(base = "long/53-commit-commands", link = "Part of #13")
  }
  ```
  `PublisherSpec.kt`: после `val checks` — `val archive = mockk<PlanArchive>()`; `Publisher(…, checks = checks, archive = archive)`; `justRun { archive.run(console = any()) }`. В `given("PR ещё нет")` и `given("PR уже открыт")` после `git.isClean()` в `verifySequence` — строка `archive.run(console = console)`. В `given("проверка не прошла")` — тоже после `git.isClean()`. Новый `given`:
  ```kotlin
  given("PR блока в long-ветку") {
      every { issues.pullRequest(branch = "task/13-x") } returns null
      every {
          pullRequests.create(base = "long/53-commit-commands", head = "task/13-x", title = "Заголовок", body = any(), draft = true)
      } returns "https://github.com/owner/repo/pull/33"
      `when` {
          publisher.publish(draft = makePartDraft(), console = console)
          then {
              verify(exactly = 0) { archive.run(console = any()) }
          }
      }
  }

  given("PR --part в dev") {
      every { issues.pullRequest(branch = "task/13-x") } returns null
      every {
          pullRequests.create(base = "dev", head = "task/13-x", title = "Заголовок", body = any(), draft = true)
      } returns "https://github.com/owner/repo/pull/34"
      `when` {
          publisher.publish(draft = makeParsedDraft().copy(link = "Part of #13"), console = console)
          then {
              verify(exactly = 0) { archive.run(console = any()) }
          }
      }
  }
  ```
  (`import io.mockk.verify`)
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PublisherSpec*'` — падает: `No parameter with name 'archive' found`
- [ ] 3. Минимальный код. `Publisher.kt`: в конструктор после `checks` — `private val archive: PlanArchive,`; в `publish` после проверки `isClean`:
  ```kotlin
  if (draft.base == DEV && draft.link.startsWith(prefix = CLOSES)) {
      archive.run(console = console)
  }
  ```
  и в конец класса:
  ```kotlin
  private companion object {
      const val DEV = "dev"
      const val CLOSES = "Closes #"
  }
  ```
  KDoc класса: «Общее у `task pr` и `task final`: проверки, план из ветки — в архив при PR в `dev` (#53), тело по шаблону, push, PR — новый или обновлённый (повтор безопасен).»
  `Main.kt`: `import io.github.nakulov.standards.task.command.PlanArchive`;
  ```kotlin
  val publisher = Publisher(
      git = git,
      issues = issues,
      pullRequests = pullRequests,
      checks = checks,
      archive = PlanArchive(git = git),
  )
  ```
- [ ] 4. Запустить `./gradlew :scripts:test` — `BUILD SUCCESSFUL` (`PrCommandSpec`, `FinalCommandSpec` подменяют `Publisher` целиком — не меняются)
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Что даёт этот PR — проверено: в черновой ветке с `plans/x.md` `scripts/task pr` делает коммит «Убрать план: …», копия в `plans/done/` основной папки
- [ ] Крайние случаи обработаны: из ворктри копия — в основной папке; PR в long-ветку и ветка без плана — без коммита; план в `plans/done/` не трогается
- [ ] Полный набор тестов зелёный
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам (не этот блок)

### Документация
- [ ] README, `docs/` — не нужно (#34)
- [ ] Новые паттерны — в файл проекта и карту: нет

## PR 3: части плана записываются в тело задачи без дублей
**Объём:** `github/IssueParts.kt` + Spec + SpecData, `github/Issues.kt`, `IssuesSpec.kt`, `IssuesSpecData.kt`; ≈ 6 файлов

### Задача 7: `IssueParts`
- Создать: `github/IssueParts.kt` — раскладка частей между метками
- Создать: `github/IssuePartsSpec.kt`, `IssuePartsSpecData.kt` — дописать, заменить, вне меток, отказы

- [ ] 1. Падающий тест.
  `IssuePartsSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.github

  const val PLAN_LINE = "План: ветка `long/53-commit-commands`, `plans/commit-commands.md`"

  fun makeIssueBody(): String {
      return "<!-- подсказка шаблона -->\n\n## Проблема\nТекст.\n\n## Результаты обсуждения\n\n" +
          "**Отклонено:**\n- груминг — причина\n\n## Критерии готовности\n**Автоматически:**\n- [ ] старое\n" +
          "**Вручную:**\n- [ ] руками\n"
  }

  fun makeParts(): String {
      return "## Отклонено\n- план — причина\n\n## Автоматически\n- [ ] `./gradlew :scripts:test`\n"
  }

  fun makeMergedBody(): String {
      return "<!-- подсказка шаблона -->\n\n<!-- план -->\n$PLAN_LINE\n<!-- /план -->\n\n## Проблема\nТекст.\n\n" +
          "## Результаты обсуждения\n\n**Отклонено:**\n- груминг — причина\n\n<!-- план -->\n- план — причина\n" +
          "<!-- /план -->\n\n## Критерии готовности\n**Автоматически:**\n- [ ] старое\n\n<!-- план -->\n" +
          "- [ ] `./gradlew :scripts:test`\n<!-- /план -->\n**Вручную:**\n- [ ] руками\n"
  }

  fun makeNewParts(): String {
      return "## Отклонено\n- другой — причина\n\n## Автоматически\n- [ ] `./gradlew :scripts:test`\n"
  }

  /** Повтор: в «Отклонено» строка плана заменена, строка груминга на месте. */
  fun makeRemergedBody(): String {
      return makeMergedBody().replace(oldValue = "- план — причина", newValue = "- другой — причина")
  }

  fun makeBrokenBody(): String {
      return makeMergedBody().replaceFirst(oldValue = "<!-- /план -->\n\n## Критерии", newValue = "\n## Критерии")
  }
  ```
  `IssuePartsSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.github

  import io.github.nakulov.kotestkit.TestOptions
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.standards.task.cli.TaskError
  import io.kotest.core.spec.style.BehaviorSpec

  class IssuePartsSpec :
      BehaviorSpec({
          shouldReturnTest(makeMergedBody(), options = TestOptions(name = "дописать метки")) {
              IssueParts.merge(body = makeIssueBody(), parts = makeParts(), planLine = PLAN_LINE)
          }

          shouldReturnTest(makeRemergedBody(), options = TestOptions(name = "повтор заменяет содержимое меток")) {
              IssueParts.merge(body = makeMergedBody(), parts = makeNewParts(), planLine = PLAN_LINE)
          }

          shouldReturnTest(makeMergedBody(), options = TestOptions(name = "повтор без изменений")) {
              IssueParts.merge(body = makeMergedBody(), parts = makeParts(), planLine = PLAN_LINE)
          }

          shouldReturnTest(makeRemergedBody(), options = TestOptions(name = "повтор на теле с \\r\\n")) {
              val body = makeMergedBody().replace(oldValue = "\n", newValue = "\r\n")
              IssueParts.merge(body = body, parts = makeNewParts(), planLine = PLAN_LINE)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("в задаче нет раздела «Решено» — исправьте заголовок в файле частей") {
              IssueParts.merge(body = makeIssueBody(), parts = "## Решено\n- x\n", planLine = PLAN_LINE)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "в разделе «Отклонено» метка <!-- план --> без пары — поправьте тело задачи",
          ) {
              IssueParts.merge(body = makeBrokenBody(), parts = makeParts(), planLine = PLAN_LINE)
          }
      })
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*IssuePartsSpec*'` — падает: `Unresolved reference 'IssueParts'`
- [ ] 3. Минимальный код, `github/IssueParts.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.github

  import io.github.nakulov.standards.task.cli.TaskError

  /**
   * Части плана в теле задачи (#53): каждая — между метками `<!-- план -->` … `<!-- /план -->` в своём разделе.
   * Повтор заменяет содержимое меток; строки вне меток (груминг, ручные правки) не трогаются. Тело из веб-интерфейса
   * GitHub приходит с `\r\n` — переводы строк приводятся к `\n`, иначе метки не найдутся и блок задвоится.
   */
  object IssueParts {
      private const val OPEN = "<!-- план -->"
      private const val CLOSE = "<!-- /план -->"
      private const val HEADING = "## "
      private const val BODY_START = "начало тела"
      private val BOLD = Regex(pattern = "^\\*\\*[^*]+:\\*\\*$")

      fun merge(
          body: String,
          parts: String,
          planLine: String,
      ): String {
          var lines = body.replace(oldValue = "\r\n", newValue = "\n").lines()
          val first = lines.indexOfFirst { line -> line.startsWith(prefix = HEADING) }.takeIf { index -> index >= 0 } ?: lines.size
          lines = place(lines = lines, start = 0, end = first, content = listOf(planLine), name = BODY_START)
          sections(text = parts).forEach { (name, content) ->
              val heading = lines.indexOfFirst { line -> line.trim() == "$HEADING$name" || line.trim() == "**$name:**" }
              if (heading < 0) {
                  throw TaskError.Refusal(message = "в задаче нет раздела «$name» — исправьте заголовок в файле частей")
              }
              val end = (heading + 1 until lines.size).firstOrNull { index -> isHeading(line = lines[index]) } ?: lines.size
              lines = place(lines = lines, start = heading + 1, end = end, content = content, name = name)
          }
          return lines.joinToString(separator = "\n")
      }

      private fun place(
          lines: List<String>,
          start: Int,
          end: Int,
          content: List<String>,
          name: String,
      ): List<String> {
          val span = lines.subList(fromIndex = start, toIndex = end)
          val opens = span.count { line -> line == OPEN }
          val closes = span.count { line -> line == CLOSE }
          val open = span.indexOf(element = OPEN)
          val close = span.indexOf(element = CLOSE)
          if (opens != closes || opens > 1 || close < open) {
              throw TaskError.Refusal(message = "в разделе «$name» метка $OPEN без пары — поправьте тело задачи")
          }
          val block = listOf(OPEN) + content + CLOSE
          if (opens == 1) {
              return lines.take(n = start + open) + block + lines.drop(n = start + close + 1)
          }
          val last = span.indexOfLast { line -> line.isNotBlank() }
          val at = start + last + 1
          val gap = if (last >= 0) listOf("") else emptyList()
          return lines.take(n = at) + gap + block + lines.drop(n = at)
      }

      private fun isHeading(line: String): Boolean {
          return line.startsWith(prefix = HEADING) || BOLD.matches(input = line.trim())
      }

      /** Разделы файла частей: `## <название>` → строки без пустых по краям. */
      private fun sections(text: String): List<Pair<String, List<String>>> {
          val result = mutableListOf<Pair<String, MutableList<String>>>()
          text.lines().forEach { line ->
              if (line.startsWith(prefix = HEADING)) {
                  result += line.removePrefix(prefix = HEADING).trim() to mutableListOf()
              } else {
                  result.lastOrNull()?.second?.add(line)
              }
          }
          return result.map { (name, content) ->
              name to content.dropWhile { line -> line.isBlank() }.dropLastWhile { line -> line.isBlank() }
          }
      }
  }
  ```
  Образец `makeMergedBody` выведен по коду: новый блок встаёт после последней непустой строки раздела, перед ним — пустая строка; пустые строки раздела после блока сохраняются.
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*IssuePartsSpec*'` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Задача 8: тело задачи в `Issues`
- Изменить: `github/Issues.kt` — `body(number)`, `editBody(number, body)`
- Изменить: `github/IssuesSpec.kt`, `IssuesSpecData.kt` — аргументы `gh`

- [ ] 1. Падающий тест.
  `IssuesSpecData.kt`:
  ```kotlin
  fun makeBodyArgs(): List<String> {
      return listOf("issue", "view", "13", "--json", "body", "--jq", ".body")
  }

  fun makeEditBodyArgs(): List<String> {
      return listOf("issue", "edit", "13", "--body", "Новое тело")
  }
  ```
  `IssuesSpec.kt`:
  ```kotlin
  shouldReturnTest("Тело задачи") {
      every { gitHub.read(args = makeBodyArgs()) } returns "Тело задачи"
      issues.body(number = 13)
  }

  shouldReturnTest(Unit) {
      every { gitHub.write(args = makeEditBodyArgs()) } returns "https://github.com/owner/repo/issues/13"
      issues.editBody(number = 13, body = "Новое тело")
  }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*IssuesSpec*'` — падает: `Unresolved reference 'body'`
- [ ] 3. Минимальный код в `Issues.kt` (после `nodeId`):
  ```kotlin
  /** Тело задачи — для частей плана (#53). */
  fun body(number: Int): String {
      return gitHub.read(args = listOf("issue", "view", "$number", "--json", "body", "--jq", ".body"))
  }

  fun editBody(
      number: Int,
      body: String,
  ) {
      gitHub.write(args = listOf("issue", "edit", "$number", "--body", body))
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*IssuesSpec*'` — зелёный; `./gradlew :scripts:test` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Что даёт этот PR — проверено: спеки `IssueParts` на образце тела #53
- [ ] Крайние случаи обработаны: повтор без изменений даёт то же тело; сломанные метки; жирный подзаголовок и `##`; раздела нет
- [ ] Полный набор тестов зелёный
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам (не этот блок)

### Документация
- [ ] README, `docs/` — не нужно (#34)
- [ ] Новые паттерны — в файл проекта и карту: нет

## PR 4: утверждённый план сохраняется командой `task plan`
**Объём:** `git/Git.kt`, `GitSpec.kt`, `GitSpecData.kt`, `command/PlanCommand.kt` + Spec + SpecData, `cli/Main.kt`; ≈ 7 файлов

### Задача 9: ворктри, push ссылки, перемотка в `Git`
- Изменить: `git/Git.kt` — `openedAt`, `isAncestor`, `addWorktree`, `removeWorktree`, `hasChanges`, `commitAll`, `pushHead`, `head`, `moveBranch`, приватный `readIn`
- Изменить: `git/GitSpecData.kt` — `makeIdentityClone()`
- Изменить: `git/GitSpec.kt` — четыре случая

- [ ] 1. Падающий тест.
  `GitSpecData.kt`:
  ```kotlin
  /** Клон с личностью в настройке — для коммитов во временном worktree. */
  fun makeIdentityClone(): File {
      val clone = makeClone()
      git(directory = clone, args = listOf("config", "user.name", "task"))
      git(directory = clone, args = listOf("config", "user.email", "task@example.invalid"))
      return clone
  }
  ```
  `GitSpec.kt`:
  ```kotlin
  shouldReturnTest(true, options = TestOptions(name = "где открыта ветка")) {
      val worktree = makeWorktree()
      val clone = File(worktree.parentFile, "clone")
      File(checkNotNull(Git(shell = ProcessShell(directory = clone)).openedAt(branch = "task/1-x"))).canonicalPath ==
          worktree.canonicalPath
  }

  shouldReturnTest(listOf(true, false), options = TestOptions(name = "предок ветки")) {
      val git = Git(shell = ProcessShell(directory = makeCloneWithPlanBranch()))
      listOf(git.isAncestor(ancestor = "origin/dev", descendant = "task/7-p"), git.isAncestor(ancestor = "task/7-p", descendant = "origin/dev"))
  }

  shouldReturnTest(listOf(true, true, false), options = TestOptions(name = "коммит во временном worktree и push")) {
      val clone = makeIdentityClone()
      val git = Git(shell = ProcessShell(directory = clone))
      val temp = File(git.commonDir(), "task-plan/q").path
      git.addWorktree(path = temp, ref = "origin/dev")
      File(temp, "plans").mkdirs()
      File(temp, "plans/q.md").writeText("# План Q\n")
      val changed = git.hasChanges(path = temp)
      git.commitAll(path = temp, message = "Добавить план: Q")
      git.pushHead(path = temp, branch = "task/8-q")
      git.removeWorktree(path = temp)
      git.fetch()
      listOf(changed, git.hasRemoteBranch(name = "task/8-q"), File(temp).exists())
  }

  shouldReturnTest("Добавить план: Q", options = TestOptions(name = "локальная ветка перематывается на коммит")) {
      val clone = makeIdentityClone()
      val git = Git(shell = ProcessShell(directory = clone))
      val temp = File(git.commonDir(), "task-plan/q").path
      git.addWorktree(path = temp, ref = "origin/dev")
      File(temp, "q.md").writeText("q\n")
      git.commitAll(path = temp, message = "Добавить план: Q")
      val commit = git.head(path = temp)
      git.removeWorktree(path = temp)
      git.moveBranch(branch = "task/8-q", commit = commit)
      git.switchTo(name = "task/8-q")
      readHeadSubject(directory = clone)
  }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: `Unresolved reference 'openedAt'`
- [ ] 3. Минимальный код в `Git.kt`:
  ```kotlin
  /** Папка, где открыта ветка [branch] — основная или worktree; не открыта — `null`. */
  fun openedAt(branch: String): String? {
      var path: String? = null
      for (line in read(args = listOf("worktree", "list", "--porcelain")).lines()) {
          if (line.startsWith(prefix = WORKTREE)) {
              path = line.removePrefix(prefix = WORKTREE)
          } else if (line == "branch refs/heads/$branch") {
              return path
          }
      }
      return null
  }

  fun isAncestor(
      ancestor: String,
      descendant: String,
  ): Boolean {
      return run(args = listOf("merge-base", "--is-ancestor", ancestor, descendant)).code == 0
  }

  /** Временный worktree без ветки на [ref]: в нём `task plan` коммитит, не трогая рабочую папку (#53). */
  fun addWorktree(
      path: String,
      ref: String,
  ) {
      read(args = listOf("worktree", "add", "--quiet", "--detach", path, ref))
  }

  fun removeWorktree(path: String) {
      read(args = listOf("worktree", "remove", "--force", path))
  }

  fun hasChanges(path: String): Boolean {
      return readIn(path = path, args = listOf("status", "--porcelain")).isNotEmpty()
  }

  fun commitAll(
      path: String,
      message: String,
  ) {
      readIn(path = path, args = listOf("add", "--all"))
      readIn(path = path, args = listOf("commit", "--quiet", "--message", message))
  }

  /** Коммит worktree [path] — в ветку [branch] в `origin`, без локальной ветки. */
  fun pushHead(
      path: String,
      branch: String,
  ) {
      readIn(path = path, args = listOf("push", "--quiet", "origin", "HEAD:refs/heads/$branch"))
  }

  fun head(path: String): String {
      return readIn(path = path, args = listOf("rev-parse", "HEAD"))
  }

  /** Локальная ветка — на [commit] (хеш или ссылка); вызывающий проверил, что это перемотка и ветка нигде не открыта. */
  fun moveBranch(
      branch: String,
      commit: String,
  ) {
      read(args = listOf("update-ref", "refs/heads/$branch", commit))
  }

  /** `git -C [path]`: ошибка называет команду, а не `-C`. */
  private fun readIn(
      path: String,
      args: List<String>,
  ): String {
      val result = run(args = listOf("-C", path) + args)
      if (result.code != 0) {
          throw TaskError.Failure(message = "git ${args.first()}: ${result.err.trim().lineSequence().first()}")
      }
      return result.out.trim()
  }
  ```
  В `companion object`: `const val WORKTREE = "worktree "`.
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Задача 10: команда `task plan`
- Создать: `command/PlanCommand.kt` — `task plan`
- Создать: `command/PlanCommandSpec.kt`, `PlanCommandSpecData.kt` — поток, повтор, отказы, уборка
- Изменить: `cli/Main.kt` — `"plan" to PlanCommand(…)`

- [ ] 1. Падающий тест.
  `PlanCommandSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import java.io.File
  import java.nio.file.Files

  const val PLAN_BRANCH = "long/53-commit-commands"
  const val PLAN_LINE = "План: ветка `long/53-commit-commands`, `plans/commit-commands.md`"

  /** Корень репозитория с `plans/commit-commands.md` и `parts.md`, общий `.git` — во временной папке модуля. */
  fun makePlanRoot(): File {
      val base = File(System.getProperty("task.cache"), "plan")
      base.mkdirs()
      val root = Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "run").toFile()
      File(root, "plans").mkdirs()
      File(root, "plans/commit-commands.md").writeText("# Команды коммита\n")
      File(root, "parts.md").writeText("## Отклонено\n- вариант — причина\n")
      File(root, ".git").mkdirs()
      return root
  }

  fun makePlanArgs(): List<String> {
      return listOf("53", "commit-commands", "--long", "--issue-parts", "parts.md")
  }

  fun makePlanIssueBody(): String {
      return "## Проблема\nТекст.\n\n**Отклонено:**\n- груминг\n"
  }

  fun makePlanTemp(root: File): String {
      return File(File(root, ".git"), "task-plan/commit-commands").path
  }
  ```
  `PlanCommandSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.kotestkit.then
  import io.github.nakulov.kotestkit.`when`
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.Git
  import io.github.nakulov.standards.task.github.IssueParts
  import io.github.nakulov.standards.task.github.Issues
  import io.kotest.assertions.throwables.shouldThrow
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.kotest.matchers.shouldBe
  import io.mockk.every
  import io.mockk.justRun
  import io.mockk.mockk
  import io.mockk.verify
  import io.mockk.verifySequence
  import java.io.File

  class PlanCommandSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val git = mockk<Git>()
          val issues = mockk<Issues>()
          val console = mockk<Console>()
          val root = makePlanRoot()
          val temp = makePlanTemp(root = root)
          val command = PlanCommand(git = git, issues = issues, directory = root)
          val merged = IssueParts.merge(
              body = makePlanIssueBody(),
              parts = File(root, "parts.md").readText(),
              planLine = PLAN_LINE,
          )
          justRun { console.line(text = any()) }
          justRun { console.error(text = any()) }
          justRun { git.fetch() }
          justRun { git.addWorktree(path = any(), ref = any()) }
          justRun { git.removeWorktree(path = any()) }
          justRun { git.commitAll(path = any(), message = any()) }
          justRun { git.pushHead(path = any(), branch = any()) }
          justRun { git.moveBranch(branch = any(), commit = any()) }
          justRun { issues.editBody(number = any(), body = any()) }
          every { git.root() } returns root.path
          every { git.commonDir() } returns File(root, ".git").path
          every { git.openedAt(branch = PLAN_BRANCH) } returns null
          every { git.hasChanges(path = temp) } returns true
          every { git.head(path = temp) } returns "abc123"
          every { issues.info(number = 53) } returns makeTaskInfo().copy(number = 53)
          every { issues.body(number = 53) } returns makePlanIssueBody()

          given("ветки нет") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              `when` {
                  command.run(args = makePlanArgs(), console = console)
                  then {
                      File(root, "plans/commit-commands.md").exists() shouldBe false
                      verifySequence {
                          git.root()
                          issues.info(number = 53)
                          issues.body(number = 53)
                          git.fetch()
                          git.openedAt(branch = PLAN_BRANCH)
                          git.hasRemoteBranch(name = PLAN_BRANCH)
                          git.hasLocalBranch(name = PLAN_BRANCH)
                          git.commonDir()
                          git.addWorktree(path = temp, ref = "origin/dev")
                          git.hasChanges(path = temp)
                          git.commitAll(path = temp, message = "Добавить план: Команды коммита")
                          git.pushHead(path = temp, branch = PLAN_BRANCH)
                          console.line(text = "Добавить план: Команды коммита → $PLAN_BRANCH")
                          git.head(path = temp)
                          git.removeWorktree(path = temp)
                          issues.editBody(number = 53, body = merged)
                          console.line(text = "#53: тело задачи обновлено, локальный plans/commit-commands.md удалён")
                      }
                  }
              }
          }

          given("ветка есть, локальная отстала") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
              every { git.isAncestor(ancestor = "refs/heads/$PLAN_BRANCH", descendant = "origin/$PLAN_BRANCH") } returns true
              `when` {
                  command.run(args = makePlanArgs(), console = console)
                  then {
                      verify { git.addWorktree(path = temp, ref = "origin/$PLAN_BRANCH") }
                      verify { git.commitAll(path = temp, message = "Обновить план: Команды коммита") }
                      verify { git.moveBranch(branch = PLAN_BRANCH, commit = "abc123") }
                  }
              }
          }

          given("план не изменился") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              every { git.hasChanges(path = temp) } returns false
              `when` {
                  command.run(args = makePlanArgs(), console = console)
                  then {
                      verify(exactly = 0) { git.commitAll(path = any(), message = any()) }
                      verify { console.line(text = "план не изменился — коммита нет") }
                      verify { issues.editBody(number = 53, body = merged) }
                  }
              }
          }

          given("план не изменился, локальная отстала") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
              every { git.isAncestor(ancestor = "refs/heads/$PLAN_BRANCH", descendant = "origin/$PLAN_BRANCH") } returns true
              every { git.hasChanges(path = temp) } returns false
              `when` {
                  command.run(args = makePlanArgs(), console = console)
                  then {
                      verify { git.moveBranch(branch = PLAN_BRANCH, commit = "origin/$PLAN_BRANCH") }
                  }
              }
          }

          given("коммит упал") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              every { git.commitAll(path = any(), message = any()) } throws TaskError.Failure(message = "git commit: сбой")
              `when` {
                  shouldThrow<TaskError.Failure> { command.run(args = makePlanArgs(), console = console) }
                  then {
                      verify { git.removeWorktree(path = temp) }
                      verify(exactly = 0) { issues.editBody(number = any(), body = any()) }
                  }
              }
          }

          given("worktree не удалился") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              every { git.removeWorktree(path = temp) } throws TaskError.Failure(message = "git worktree: занят")
              `when` {
                  command.run(args = makePlanArgs(), console = console)
                  then {
                      verify { console.error(text = "удалите вручную: git worktree remove --force $temp") }
                      verify { issues.editBody(number = 53, body = merged) }
                  }
              }
          }

          shouldThrowWithMessageTest<TaskError.Failure>(
              "план сохранён, тело задачи — нет: повторите task plan (gh issue: сбой)",
          ) {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns false
              every { issues.editBody(number = 53, body = any()) } throws TaskError.Failure(message = "gh issue: сбой")
              command.run(args = makePlanArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("#53 закрыта") {
              every { issues.info(number = 53) } returns makeClosedInfo().copy(number = 53)
              command.run(args = makePlanArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("#53 — эпик: план — на подзадачу") {
              every { issues.info(number = 53) } returns makeEpicInfo().copy(number = 53)
              command.run(args = makePlanArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(
              "ветка $PLAN_BRANCH открыта в /work/wt — выполнение начато, план правит исполнитель",
          ) {
              every { git.openedAt(branch = PLAN_BRANCH) } returns "/work/wt"
              command.run(args = makePlanArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("локальная $PLAN_BRANCH расходится с origin/$PLAN_BRANCH — сведите вручную") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns true
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
              every { git.isAncestor(ancestor = "refs/heads/$PLAN_BRANCH", descendant = "origin/$PLAN_BRANCH") } returns false
              command.run(args = makePlanArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("локальная $PLAN_BRANCH не отправлена в origin — отправьте её или удалите") {
              every { git.hasRemoteBranch(name = PLAN_BRANCH) } returns false
              every { git.hasLocalBranch(name = PLAN_BRANCH) } returns true
              command.run(args = makePlanArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Failure>("нет plans/other.md — сначала напишите план") {
              command.run(args = listOf("53", "other", "--issue-parts", "parts.md"), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Failure>("использование: task plan ${command.usage}") {
              command.run(args = listOf("53", "commit-commands"), console = console)
          }
      })
  ```
  (`makeTaskInfo`, `makeClosedInfo`, `makeEpicInfo` — `StartCommandSpecData.kt:6-20`, номер 13 → `copy(number = 53)`.)
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PlanCommandSpec*'` — падает: `Unresolved reference 'PlanCommand'`
- [ ] 3. Минимальный код, `command/PlanCommand.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.cli.Command
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.BranchName
  import io.github.nakulov.standards.task.git.Git
  import io.github.nakulov.standards.task.github.IssueParts
  import io.github.nakulov.standards.task.github.Issues
  import java.io.File

  /**
   * `task plan <N> <slug> [--long] --issue-parts <файл>` (#53): утверждённый план — в ветку задачи в `origin` через
   * временный worktree (рабочая папка и ветка пользователя не трогаются), без смены статуса и без PR; части плана —
   * в тело задачи между метками. Повтор — «Обновить план». Локальный план после сохранения удаляется.
   */
  class PlanCommand(
      private val git: Git,
      private val issues: Issues,
      private val directory: File,
  ) : Command {
      override val usage: String =
          "<N> <slug> [--long] --issue-parts <файл> — план plans/<slug>.md в ветку задачи без смены статуса"

      override fun run(
          args: List<String>,
          console: Console,
      ) {
          val request = parse(args = args)
          val slug = request.branch.slug
          val number = request.branch.issue
          val branch = request.branch.toString()
          val plan = File(git.root(), "plans/$slug.md")
          if (!plan.isFile) {
              throw TaskError.Failure(message = "нет plans/$slug.md — сначала напишите план")
          }
          val title = plan.readLines().firstOrNull { line -> line.startsWith(prefix = "# ") }?.removePrefix(prefix = "# ")?.trim()
              ?: throw TaskError.Failure(message = "в plans/$slug.md нет строки «# название»")
          val parts = File(request.parts).let { path -> if (path.isAbsolute) path else File(directory, request.parts) }
          if (!parts.isFile) {
              throw TaskError.Failure(message = "нет файла частей ${request.parts}")
          }
          val info = issues.info(number = number)
          if (!info.open) {
              throw TaskError.Refusal(message = "#$number закрыта")
          }
          if (info.type == EPIC) {
              throw TaskError.Refusal(message = "#$number — эпик: план — на подзадачу")
          }
          val planLine = "План: ветка `$branch`, `plans/$slug.md`"
          val body = IssueParts.merge(body = issues.body(number = number), parts = parts.readText(), planLine = planLine)
          git.fetch()
          val opened = git.openedAt(branch = branch)
          if (opened != null) {
              throw TaskError.Refusal(message = "ветка $branch открыта в $opened — выполнение начато, план правит исполнитель")
          }
          val remote = git.hasRemoteBranch(name = branch)
          val local = git.hasLocalBranch(name = branch)
          if (local && !remote) {
              throw TaskError.Refusal(message = "локальная $branch не отправлена в origin — отправьте её или удалите")
          }
          if (local && !git.isAncestor(ancestor = "refs/heads/$branch", descendant = "origin/$branch")) {
              throw TaskError.Refusal(message = "локальная $branch расходится с origin/$branch — сведите вручную")
          }
          val commit = save(plan = plan, title = title, branch = branch, remote = remote, console = console)
          if (local) {
              git.moveBranch(branch = branch, commit = commit ?: "origin/$branch")
          }
          try {
              issues.editBody(number = number, body = body)
          } catch (error: TaskError) {
              throw TaskError.Failure(message = "план сохранён, тело задачи — нет: повторите task plan (${error.message})")
          }
          plan.delete()
          console.line(text = "#$number: тело задачи обновлено, локальный plans/$slug.md удалён")
      }

      /** Коммит плана во временном worktree и push; план не изменился — `null`. Worktree удаляется всегда. */
      private fun save(
          plan: File,
          title: String,
          branch: String,
          remote: Boolean,
          console: Console,
      ): String? {
          val temp = File(git.commonDir(), "task-plan/${plan.nameWithoutExtension}").path
          git.addWorktree(path = temp, ref = if (remote) "origin/$branch" else BASE)
          try {
              plan.copyTo(target = File(temp, "plans/${plan.name}"), overwrite = true)
              if (!git.hasChanges(path = temp)) {
                  console.line(text = "план не изменился — коммита нет")
                  return null
              }
              val message = "${if (remote) "Обновить" else "Добавить"} план: $title"
              git.commitAll(path = temp, message = message)
              git.pushHead(path = temp, branch = branch)
              console.line(text = "$message → $branch")
              return git.head(path = temp)
          } finally {
              try {
                  git.removeWorktree(path = temp)
              } catch (error: TaskError.Failure) {
                  console.error(text = "удалите вручную: git worktree remove --force $temp")
              }
          }
      }

      private fun parse(args: List<String>): Request {
          val partsIndex = args.indexOf(PARTS_FLAG)
          val parts = if (partsIndex >= 0) args.getOrNull(index = partsIndex + 1) else null
          val rest = if (partsIndex >= 0) args.take(n = partsIndex) + args.drop(n = partsIndex + 2) else args
          val long = LONG_FLAG in rest
          val positional = rest - LONG_FLAG
          val issue = positional.firstOrNull()?.toIntOrNull()
          if (parts == null || positional.size != 2 || issue == null) {
              throw TaskError.Failure(message = "использование: task plan $usage")
          }
          return Request(branch = BranchName.of(issue = issue, slug = positional.last(), long = long), parts = parts)
      }

      /** Разобранные аргументы: ветка плана и файл частей задачи. */
      private data class Request(
          val branch: BranchName,
          val parts: String,
      )

      private companion object {
          const val LONG_FLAG = "--long"
          const val PARTS_FLAG = "--issue-parts"
          const val BASE = "origin/dev"
          const val EPIC = "type:epic"
      }
  }
  ```
  `BranchName.slug` — `git/BranchName.kt:11`. Порядок вызовов в спеке «ветки нет» совпадает с кодом выше: `git.root()` до `issues.info`.
  `Main.kt`: `import io.github.nakulov.standards.task.command.PlanCommand`; в карту:
  ```kotlin
  "plan" to PlanCommand(git = git, issues = issues, directory = directory),
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*PlanCommandSpec*'` — зелёный; `./gradlew :scripts:test` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Что даёт этот PR — проверено: `scripts/task --help` показывает `plan` и `commit`
- [ ] Крайние случаи обработаны: повтор, план не изменился, ветка открыта, локальная расходится, сбой после push, уборка worktree
- [ ] Полный набор тестов зелёный
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам

### Документация
- [ ] README, `docs/` — не нужно (#34)
- [ ] Новые паттерны — в файл проекта и карту: нет

## Открыто
- Нет.
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка: после слияния `task final` и `git pull` — убрать временные блоки и переписать вызовы в `drafts/` (plan, execute, agents/plan-executor), скопировать в `.claude/`; `grep -rn "пока нет (временно)" drafts .claude` пуст.
- Ручная проверка: живой прогон на следующей задаче через план — `task plan` с другой ветки с незакоммиченными правками (ветка в `origin`, правки и ветка не тронуты); повторный `task plan` (одна строка «План: …», груминг цел); `execute` коммитит через `task commit`, план уходит в `plans/done/`. #53 закрывается после прогона.
