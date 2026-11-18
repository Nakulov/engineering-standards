# Ворктри задачи

**Цель:** `task start <N> <slug> --worktree` создаёт ветку задачи в копии `.claude/worktrees/<slug>` и настраивает копию по `.worktreeinclude`; `task done` в своей копии удаляет её.
**Архитектура:** `StartCommand` получает флаг `--worktree` и ветку «открыть в копии» рядом с «переключиться здесь»; новые операции git — в `Git`; копирование файлов — отдельный класс `WorktreeFiles` (файловые операции, как `PlanArchive`); `DoneCommand` последним шагом удаляет свою копию.
**Стек:** Kotlin (min 2.2), Kotest + KotestKit, mockk, настоящий `git` в спеках `Git`.

---

## Ограничения
- Удаление копии — `git worktree remove <путь>` изнутри копии, без `git -C <основная папка>`: защита сессии в ворктри запрещает `-C` наружу (опыт 2026-11-18, #52 решение 6).
- Ветки, коммиты, PR — только командами `task` (`task commit --message-file`, `task pr --part`, `task final`).
- Не трогать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/PlanCommand.kt` — использует `addWorktree --detach` для временной копии плана (#53); для задачи — отдельные методы.
- Не трогать: `drafts/`, `.claude/` — скиллы правятся в #48.
- Только Kotlin не ниже 2.2 (#13, решение 1).
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: флаг `--worktree` у `task start`, настройка копии по `.worktreeinclude`, удаление своей копии в `task done`, `.claude/worktrees/` в общем блоке `.gitignore`.
- Какую проблему решает, что даёт: работа над несколькими задачами одновременно без переключения веток; скилл `worktree` и вариант «Ворктри» в `execute` (#48) получают команду.
- Как встраивается: копия — обычный git worktree основной папки; путь печатается последней строкой — скилл передаёт его в `EnterWorktree path`.
- Критерии цели:
  - `./gradlew :scripts:test` зелёный, спеки покрывают случаи из «Критериев готовности» #52;
  - `scripts/setup --check` в этом репозитории проходит;
  - живой прогон `task start … --worktree` → `EnterWorktree path` → сборка в копии → `task done` → `ExitWorktree keep` удаётся (после завершения).

## Контекст
- Задействованные файлы:
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/StartCommand.kt` — разбор флагов (`parse`, :52), проверка чистоты (:34), `checkReady` (:66), `switchToBranch` (:86), `createBranch` (:128)
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/DoneCommand.kt` — `run` (:23), `leaveBranch` (:46): в worktree — `detach`
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt` — `openedAt` (:183), `mainRoot` (:162), `root` (:25), `removeWorktree` (:210, `--force`), `readIn` (:262, `git -C`), `fastForward` (:63)
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt` — сборка команд (:44)
  - `templates/project/gitignore`, `.gitignore` — общий блок стандарта
- Паттерны: спеки команд — `mockk` + `verifySequence` (`StartCommandSpec.kt`, `DoneCommandSpec.kt`); спеки `Git` — настоящий git во временных репозиториях (`GitSpecData.kt`, `makeClone` :11); данные — в `…SpecData.kt`; файловые операции рядом с git — `PlanArchive.kt`; именованные аргументы везде.
- Зависимости: `git worktree add` (≥ 2.17 для `--quiet`), `git ls-files --others --ignored`.

## Подход к разработке
- Завершай задачу целиком до следующей.
- Небольшие сфокусированные изменения.
- **ВАЖНО:** каждая задача — с новыми или обновлёнными тестами для её кода.
- **ВАЖНО:** все тесты зелёные до начала следующей задачи.
- **ВАЖНО:** изменение объёма — в этот план, не молча.
- После каждого изменения — конкретный тест; полный набор — в конце задачи.
- Обратная совместимость — нужна: `task start` без `--worktree` и `task done` в основной папке ведут себя как раньше (кроме нового отказа «ветка открыта в копии»).

## Структура файлов
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt` — копия на ветке, перемотка в копии, путь копии, файлы по `.worktreeinclude`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt` — новые методы на настоящем git
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpecData.kt` — клон с игнорируемыми файлами
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/StartCommand.kt:20` — `--worktree`, «ветка открыта», копирование файлов
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/StartCommandSpec.kt` — случаи копии и «ветка открыта»
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/StartCommandSpecData.kt` — сообщения и путь копии
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/WorktreeFiles.kt` — копирует в копию файлы основной папки по `.worktreeinclude`; вход: `copyInto(copy: String, console: Console)`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/WorktreeFilesSpec.kt` — копирование, нет списка, отсутствующие строки
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/WorktreeFilesSpecData.kt` — временная основная папка
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:44` — `StartCommand(…, files = WorktreeFiles(git = git))`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/DoneCommand.kt:42` — удаление своей копии последним шагом
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/DoneCommandSpec.kt` — своя копия / копия long-ветки на ветке блока
- Изменить: `templates/project/gitignore`, `.gitignore` — `.claude/worktrees/` в общем блоке
- Создать: `.worktreeinclude` — `local.properties`

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: копия создаётся `git worktree add` сразу на ветке задачи (`-b` для новой, имя ветки для существующей — git сам создаёт локальную с отслеживанием, если ветка только в `origin`). Путь копии один — `Git.copyPath(slug)`: `<основная папка>/.claude/worktrees/<slug>`, им пользуются и `start`, и `done`.
- Решения:
  - `Git.copyPath` — одна точка правды пути для `StartCommand` и `DoneCommand` — вместо константы в двух командах;
  - сравнение путей — через `File.canonicalPath`: `openedAt` и `root` дают пути из разных команд git;
  - файлы копируются только при создании копии, не при повторе и не при переключении существующей копии — не затирают правки в копии;
  - папка копии есть, ветка в ней не открыта (копия long-ветки отсоединена после `task done` ветки блока): своя чистая копия (есть в `git worktree list`, без правок) и ветка существует — копия переключается на ветку; иначе — отказ «папка занята». Дополняет решение 3 #52 — изменено при плане (ревью плана, 2026-11-18): иначе следующий блок в копии падает сырой ошибкой git;
  - отбор файлов — сначала короткий список по `.worktreeinclude` (`ls-files --others --ignored --exclude-from`), затем из него — игнорируемые `.gitignore` (`ls-files --others --ignored --exclude-standard -- <пути>`): «игнорируется и подходит под список», как у Claude Code; шаблоны разбирает git. Не перечислять все игнорируемые файлы основной папки — там сборки всех копий (ревью плана, В1);
  - «нет в основной папке» — только для строк без шаблонных символов (`*?[`), не комментариев и не исключений (`#`, `!`);
  - `task done` удаляет копию после смены статуса: `gh` нужна текущая папка, после удаления её нет.
- Как вписывается в систему: `execute` и скилл `worktree` (#48) берут путь из последней строки вывода; `task done` в копии — последний шаг, сразу за ним `ExitWorktree keep`.
- Запасной: `task done <ветка>` из основной папки после `ExitWorktree keep` — переходим, если живой прогон покажет, что защита сессии блокирует `scripts/task done` в копии.
- Отклонено: список файлов в файле проекта (#49) — задача не сделана, второй список рядом с `.worktreeinclude`; фиксированный список в стандарте — противоречит «список объявляет проект»; `--worktree <папка>` — произвольный путь ломает `EnterWorktree`; уборка отдельной задачей или вручную — копии копятся.

## Технические детали
- Структуры данных и изменения: `StartCommand.Request` получает `worktree: Boolean`; `StartCommand` — параметр конструктора `files: WorktreeFiles` (PR 2); `DoneCommand.leaveBranch` возвращает `Boolean` (был ли worktree).
- Параметры и форматы:
  - usage: `<N> <slug> [--long | --from long/<M>-…] [--worktree] — ветка task/<N>-<slug> от origin/dev, статус In progress; --worktree — в копии .claude/worktrees/<slug>`
  - отказ «открыта»: `ветка <имя> открыта в <папка> — работайте там` (`TaskError.Refusal`, код 1)
  - отказ исследования: `#<N> — исследование: ветка и копия не нужны`
  - отказ «занята»: `папка <путь> занята — работайте там или удалите её`
  - строки вывода копии: `ветка <имя> от <база> — копия <путь>`; `ветка <имя> уже есть — копия <путь>` (+ ` перемотана до origin/<имя>`); `копия уже есть: <путь>` (+ ` — перемотал до origin/<имя>`); последняя строка — `<путь>`
  - `WorktreeFiles`: `из .worktreeinclude скопировано: a, b`; `.worktreeinclude: <строка> — нет в основной папке, пропущено`
  - `task done`: `копия <путь> удалена — текущей папки больше нет`
- Поток обработки `task start --worktree`: разбор → (без проверки чистоты) → задача и статус → `checkReady` → исследование? отказ → `copyPath` → `fetch` → `openedAt`: своя копия — перемотка при отставании, «копия уже есть»; другая папка — отказ; не открыта — существующая ветка: папка копии есть — переключить свою чистую копию (иначе отказ «занята»), нет — `worktree add`; новая ветка: папка есть — отказ «занята», нет — `worktree add -b`, `--from` — `setBase`, long — `push`; файлы по `.worktreeinclude` (PR 2) → статус → путь последней строкой.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: `task start --worktree` создаёт копию задачи на ветке
  - [ ] Задача 1: Git — копия на ветке, перемотка и переключение в копии, путь копии
  - [ ] Задача 2: `task start --worktree` и отказ «ветка открыта»
  - [ ] Задача 3: `.claude/worktrees/` в общем блоке `.gitignore`
  - [ ] Приёмка
- PR 2: копия настраивается по `.worktreeinclude`
  - [ ] Задача 4: Git — файлы основной папки по `.worktreeinclude`
  - [ ] Задача 5: `WorktreeFiles` копирует файлы в копию
  - [ ] Задача 6: `task start --worktree` копирует файлы, `.worktreeinclude` этого репозитория
  - [ ] Приёмка
- PR 3: `task done` удаляет свою копию
  - [ ] Задача 7: удаление своей копии в `task done`
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Живой прогон в этом репозитории (ручной критерий #52).
- #48: шаг 2 «Где выполняем» в `execute` и черновик `drafts/worktree/SKILL.md` — путь из последней строки, `ExitWorktree keep` сразу после `task done`.
- #53: ручной критерий «живой прогон» — отметить по итогам этого выполнения.

## PR 1: `task start --worktree` создаёт копию задачи на ветке
**Объём:** `Git.kt`, `GitSpec.kt`, `StartCommand.kt`, `StartCommandSpec.kt`, `StartCommandSpecData.kt`, `templates/project/gitignore`, `.gitignore`; ≈ 7 файлов

### Задача 1: Git — копия на ветке, перемотка и переключение в копии, путь копии
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt:210` — после `removeWorktree` шесть методов; в `companion object` — `WORKTREES`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt` — шесть случаев в конце спеки

- [ ] 1. Падающий тест: в конец `GitSpec` (перед закрывающим `})`) добавить
  ```kotlin
        shouldReturnTest(listOf("task/5-c", true), options = TestOptions(name = "копия на новой ветке от origin/dev")) {
            val clone = makeClone()
            val git = Git(shell = ProcessShell(directory = clone))
            val copy = File(clone.parentFile, "copy")
            git.addBranchWorktree(path = copy.path, name = "task/5-c", base = "origin/dev")
            val opened = git.openedAt(branch = "task/5-c")
            listOf(
                Git(shell = ProcessShell(directory = copy)).currentBranch(),
                File(checkNotNull(opened)).canonicalPath == copy.canonicalPath,
            )
        }

        shouldReturnTest("task/3-z", options = TestOptions(name = "копия на ветке, которая есть только в origin")) {
            val clone = makeCloneWithRemoteBranch()
            val copy = File(clone.parentFile, "copy")
            Git(shell = ProcessShell(directory = clone)).addWorktreeOn(path = copy.path, name = "task/3-z")
            Git(shell = ProcessShell(directory = copy)).currentBranch()
        }

        shouldReturnTest(true, options = TestOptions(name = "ветка в копии перематывается до origin")) {
            val clone = makeCloneOnBranchBehindOrigin()
            val git = Git(shell = ProcessShell(directory = clone))
            git.switchTo(name = "dev")
            val copy = File(clone.parentFile, "copy")
            git.addWorktreeOn(path = copy.path, name = "task/9-f")
            git.fastForwardIn(path = copy.path, name = "task/9-f")
            File(copy, "f.txt").exists()
        }

        shouldReturnTest(true, options = TestOptions(name = "путь копии — в основной папке, и из другой копии")) {
            val worktree = makeWorktree()
            val path = Git(shell = ProcessShell(directory = worktree)).copyPath(slug = "x")
            File(path).canonicalPath == File(worktree.parentFile, "clone/.claude/worktrees/x").canonicalPath
        }

        shouldReturnTest(true, options = TestOptions(name = "список копий — с основной папкой")) {
            val worktree = makeWorktree()
            val paths = Git(shell = ProcessShell(directory = worktree)).worktrees().map { path -> File(path).canonicalPath }
            paths.containsAll(listOf(worktree.canonicalPath, File(worktree.parentFile, "clone").canonicalPath))
        }

        shouldReturnTest("task/1-x", options = TestOptions(name = "отсоединённая копия переключается на ветку")) {
            val worktree = makeWorktree()
            Git(shell = ProcessShell(directory = worktree)).detach(ref = "origin/dev")
            Git(shell = ProcessShell(directory = File(worktree.parentFile, "clone"))).switchIn(
                path = worktree.path,
                name = "task/1-x",
            )
            Git(shell = ProcessShell(directory = worktree)).currentBranch()
        }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: `Unresolved reference 'addBranchWorktree'`
- [ ] 3. Минимальный код: в `Git.kt` после `removeWorktree` (:210–212)
  ```kotlin
    /** Копия [path] на новой ветке [name] от [base] без отслеживания — как [createBranch] (#52). */
    fun addBranchWorktree(
        path: String,
        name: String,
        base: String,
    ) {
        read(args = listOf("worktree", "add", "--quiet", "--no-track", "-b", name, path, base))
    }

    /** Копия [path] на существующей ветке [name]; есть только в `origin` — создаётся локальная с отслеживанием. */
    fun addWorktreeOn(
        path: String,
        name: String,
    ) {
        read(args = listOf("worktree", "add", "--quiet", path, name))
    }

    /** [fastForward] в копии [path]: ветка открыта там, а не в текущей папке (#52). */
    fun fastForwardIn(
        path: String,
        name: String,
    ) {
        readIn(path = path, args = listOf("merge", "--quiet", "--ff-only", "origin/$name"))
    }

    /** Копия задачи со [slug] — `.claude/worktrees/<slug>` основной папки, и при запуске из другой копии (#52). */
    fun copyPath(slug: String): String {
        return File(mainRoot(), "$WORKTREES/$slug").path
    }

    /** Папки всех копий репозитория, с основной (#52). */
    fun worktrees(): List<String> {
        return read(args = listOf("worktree", "list", "--porcelain"))
            .lines()
            .filter { line -> line.startsWith(prefix = WORKTREE) }
            .map { line -> line.removePrefix(prefix = WORKTREE) }
    }

    /** Копию [path] — на существующую ветку [name]: копия long-ветки отсоединена после `task done` ветки блока (#52). */
    fun switchIn(
        path: String,
        name: String,
    ) {
        readIn(path = path, args = listOf("switch", "--quiet", name))
    }
  ```
  и в `companion object` (после `const val WORKTREE = "worktree "`): `const val WORKTREES = ".claude/worktrees"`
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе: `task commit --message-file <файл>`

### Задача 2: `task start --worktree` и отказ «ветка открыта»
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/StartCommand.kt` — целиком (код ниже)
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/StartCommandSpec.kt` — заглушки по умолчанию, `openedAt` в старых последовательностях, новые случаи
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/StartCommandSpecData.kt` — usage и новые данные

- [ ] 1. Падающий тест.
  В `StartCommandSpecData.kt` заменить `makeUsageMessage` и добавить функции:
  ```kotlin
  fun makeUsageMessage(): String {
      return "использование: task start <N> <slug> [--long | --from long/<M>-…] [--worktree] — ветка task/<N>-<slug> " +
          "от origin/dev, статус In progress; --worktree — в копии .claude/worktrees/<slug>"
  }

  fun makeCopyPath(): String {
      return "/repo/.claude/worktrees/task-scripts"
  }

  fun makeOpenedMessage(folder: String): String {
      return "ветка task/13-task-scripts открыта в $folder — работайте там"
  }

  fun makeOccupiedMessage(folder: String): String {
      return "папка $folder занята — работайте там или удалите её"
  }

  /** Существующая папка копии: `File.exists` в команде — настоящий, папка во временном каталоге `build/` модуля. */
  fun makeExistingCopy(): File {
      val base = File(System.getProperty("task.cache"), "start")
      base.mkdirs()
      return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ "copy").toFile()
  }
  ```
  и импорты `java.io.File`, `java.nio.file.Files`.
  В `StartCommandSpec.kt`:
  - к заглушкам по умолчанию (после `every { issues.nodeId(number = 13) } returns "I_issue13"`) добавить
    ```kotlin
        justRun { git.addBranchWorktree(path = any(), name = any(), base = any()) }
        justRun { git.addWorktreeOn(path = any(), name = any()) }
        justRun { git.fastForwardIn(path = any(), name = any()) }
        justRun { git.switchIn(path = any(), name = any()) }
        val existing = makeExistingCopy().path
        every { git.openedAt(branch = any()) } returns null
        every { git.root() } returns "/repo"
        every { git.copyPath(slug = "task-scripts") } returns makeCopyPath()
    ```
  - в каждом `verifySequence`, где есть `git.fetch()` (случаи «задача Groomed, ветки нет», «повторный запуск: long-ветка…», «ветка есть только в origin», «локальная long-ветка отстала…», «локальная ветка разошлась…», «новая long-ветка», «часть long-ветки: --from» — 7 мест), сразу после `git.fetch()` вставить `git.openedAt(branch = "<ветка этого случая>")` — та же ветка, что в следующем `git.hasLocalBranch(name = …)`;
  - перед последним `})` добавить случаи:
  ```kotlin
        given("--worktree, ветки нет, основная папка грязная") {
            every { git.isClean() } returns false
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.addBranchWorktree(path = makeCopyPath(), name = "task/13-task-scripts", base = "origin/dev")
                        console.line(text = "ветка task/13-task-scripts от origin/dev — копия ${makeCopyPath()}")
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--worktree, ветка есть только в origin") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.addWorktreeOn(path = makeCopyPath(), name = "task/13-task-scripts")
                        console.line(text = "ветка task/13-task-scripts уже есть — копия ${makeCopyPath()}")
                        console.line(text = "#13 уже In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--worktree повторно: копия уже есть, ветка отстала от origin") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.openedAt(branch = "task/13-task-scripts") } returns makeCopyPath()
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            every {
                git.isAncestor(ancestor = "origin/task/13-task-scripts", descendant = "refs/heads/task/13-task-scripts")
            } returns false
            every {
                git.isAncestor(ancestor = "refs/heads/task/13-task-scripts", descendant = "origin/task/13-task-scripts")
            } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/task/13-task-scripts",
                            descendant = "refs/heads/task/13-task-scripts",
                        )
                        git.isAncestor(
                            ancestor = "refs/heads/task/13-task-scripts",
                            descendant = "origin/task/13-task-scripts",
                        )
                        git.fastForwardIn(path = makeCopyPath(), name = "task/13-task-scripts")
                        console.line(
                            text = "копия уже есть: ${makeCopyPath()} — перемотал до origin/task/13-task-scripts",
                        )
                        console.line(text = "#13 уже In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--long --worktree: новая long-ветка в копии") {
            every { git.copyPath(slug = "big") } returns "/repo/.claude/worktrees/big"
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "long/13-big") } returns false
            every { git.hasRemoteBranch(name = "long/13-big") } returns false
            `when` {
                command.run(args = listOf("13", "big", "--long", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "big")
                        git.fetch()
                        git.openedAt(branch = "long/13-big")
                        git.hasLocalBranch(name = "long/13-big")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.addBranchWorktree(path = "/repo/.claude/worktrees/big", name = "long/13-big", base = "origin/dev")
                        console.line(text = "ветка long/13-big от origin/dev — копия /repo/.claude/worktrees/big")
                        git.push(name = "long/13-big")
                        console.line(text = "ветка long/13-big отправлена в origin")
                        board.move(item = makeGroomedItem(), status = "In progress")
                        console.line(text = "#13 → In progress")
                        console.line(text = "/repo/.claude/worktrees/big")
                    }
                }
            }
        }

        given("--worktree, локальная ветка отстала от origin, копии нет") {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns true
            every {
                git.isAncestor(ancestor = "origin/task/13-task-scripts", descendant = "refs/heads/task/13-task-scripts")
            } returns false
            every {
                git.isAncestor(ancestor = "refs/heads/task/13-task-scripts", descendant = "origin/task/13-task-scripts")
            } returns true
            `when` {
                command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "task-scripts")
                        git.fetch()
                        git.openedAt(branch = "task/13-task-scripts")
                        git.hasLocalBranch(name = "task/13-task-scripts")
                        git.hasRemoteBranch(name = "task/13-task-scripts")
                        git.isAncestor(
                            ancestor = "origin/task/13-task-scripts",
                            descendant = "refs/heads/task/13-task-scripts",
                        )
                        git.isAncestor(
                            ancestor = "refs/heads/task/13-task-scripts",
                            descendant = "origin/task/13-task-scripts",
                        )
                        git.addWorktreeOn(path = makeCopyPath(), name = "task/13-task-scripts")
                        git.fastForwardIn(path = makeCopyPath(), name = "task/13-task-scripts")
                        console.line(
                            text = "ветка task/13-task-scripts уже есть — копия ${makeCopyPath()} перемотана до " +
                                "origin/task/13-task-scripts",
                        )
                        console.line(text = "#13 уже In progress")
                        console.line(text = makeCopyPath())
                    }
                }
            }
        }

        given("--from long/… --worktree: ветка блока в своей копии") {
            every { git.copyPath(slug = "part-a") } returns "/repo/.claude/worktrees/part-a"
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = "task/13-part-a") } returns false
            every { git.hasRemoteBranch(name = "long/13-big") } returns true
            `when` {
                command.run(args = listOf("13", "part-a", "--from", "long/13-big", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "part-a")
                        git.fetch()
                        git.openedAt(branch = "task/13-part-a")
                        git.hasLocalBranch(name = "task/13-part-a")
                        git.hasRemoteBranch(name = "task/13-part-a")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.addBranchWorktree(
                            path = "/repo/.claude/worktrees/part-a",
                            name = "task/13-part-a",
                            base = "origin/long/13-big",
                        )
                        git.setBase(branch = "task/13-part-a", base = "long/13-big")
                        console.line(
                            text = "ветка task/13-part-a от origin/long/13-big — копия /repo/.claude/worktrees/part-a",
                        )
                        console.line(text = "#13 уже In progress")
                        console.line(text = "/repo/.claude/worktrees/part-a")
                    }
                }
            }
        }

        given("--long --worktree: своя копия long-ветки отсоединена после task done ветки блока") {
            every { git.copyPath(slug = "big") } returns existing
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "long/13-big") } returns true
            every { git.hasRemoteBranch(name = "long/13-big") } returns true
            every { git.isAncestor(ancestor = "origin/long/13-big", descendant = "refs/heads/long/13-big") } returns true
            every { git.worktrees() } returns listOf("/repo", existing)
            every { git.hasChanges(path = existing) } returns false
            `when` {
                command.run(args = listOf("13", "big", "--long", "--worktree"), console = console)
                then {
                    verifySequence {
                        issues.info(number = 13)
                        issues.nodeId(number = 13)
                        board.item(issue = 13, nodeId = "I_issue13")
                        git.copyPath(slug = "big")
                        git.fetch()
                        git.openedAt(branch = "long/13-big")
                        git.hasLocalBranch(name = "long/13-big")
                        git.hasRemoteBranch(name = "long/13-big")
                        git.isAncestor(ancestor = "origin/long/13-big", descendant = "refs/heads/long/13-big")
                        git.worktrees()
                        git.hasChanges(path = existing)
                        git.switchIn(path = existing, name = "long/13-big")
                        console.line(text = "ветка long/13-big уже есть — копия $existing")
                        console.line(text = "#13 уже In progress")
                        console.line(text = existing)
                    }
                }
            }
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOccupiedMessage(folder = existing)) {
            every { git.copyPath(slug = "task-scripts") } returns existing
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns true
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            every { git.worktrees() } returns listOf("/repo")
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOccupiedMessage(folder = existing)) {
            every { git.copyPath(slug = "task-scripts") } returns existing
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            every { git.hasLocalBranch(name = "task/13-task-scripts") } returns false
            every { git.hasRemoteBranch(name = "task/13-task-scripts") } returns false
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOpenedMessage(folder = "/repo")) {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.openedAt(branch = "task/13-task-scripts") } returns "/repo"
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>(makeOpenedMessage(folder = makeCopyPath())) {
            every { issues.info(number = 13) } returns makeTaskInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
            every { git.openedAt(branch = "task/13-task-scripts") } returns makeCopyPath()
            command.run(args = listOf("13", "task-scripts"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#13 — исследование: ветка и копия не нужны") {
            every { issues.info(number = 13) } returns makeResearchInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            command.run(args = listOf("13", "wiki-spike", "--worktree"), console = console)
        }

        shouldThrowWithMessageTest<TaskError.Refusal>("#13 закрыта") {
            every { issues.info(number = 13) } returns makeClosedInfo()
            every { board.item(issue = 13, nodeId = "I_issue13") } returns makeGroomedItem()
            command.run(args = listOf("13", "task-scripts", "--worktree"), console = console)
        }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*StartCommandSpec*'` — падает: новые случаи — `TaskError.Failure: использование: task start …` (старый `parse` не знает `--worktree` и считает его третьим позиционным аргументом); старые случаи с `git.fetch()` — лишний `git.openedAt` в ожидании; случай неверных аргументов — другое сообщение usage
- [ ] 3. Минимальный код: `StartCommand.kt` целиком
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.board.Board
  import io.github.nakulov.standards.task.cli.Command
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.BranchName
  import io.github.nakulov.standards.task.git.Git
  import io.github.nakulov.standards.task.github.IssueInfo
  import io.github.nakulov.standards.task.github.Issues
  import java.io.File

  /**
   * `task start <N> <slug> [--long | --from long/<M>-…] [--worktree]`: проверка готовности (#2, `take`, пункт 1 — статус
   * и блокеры), ветка от свежего `origin/dev` (или от long-ветки — для маленького PR в неё), статус In progress.
   * Long-ветка сразу отправляется в `origin`, чтобы от неё начинались части. Исследование — без ветки, эпик в работу не
   * берётся. Существующая ветка, отставшая от `origin`, перематывается; разошедшаяся — отказ до переключения (#61).
   * `--worktree` — ветка в копии `.claude/worktrees/<slug>` основной папки: основная папка не переключается и может быть
   * грязной, последняя строка вывода — путь копии (#52). Ветка открыта в другой папке — отказ с путём.
   * Повторный запуск безопасен.
   */
  class StartCommand(
      private val git: Git,
      private val issues: Issues,
      private val board: Board,
  ) : Command {
      override val usage: String =
          "<N> <slug> [--long | --from long/<M>-…] [--worktree] — ветка task/<N>-<slug> от origin/dev, статус " +
              "In progress; --worktree — в копии .claude/worktrees/<slug>"

      override fun run(
          args: List<String>,
          console: Console,
      ) {
          val request = parse(args = args)
          val branch = request.branch
          if (!request.worktree && !git.isClean()) {
              throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
          }
          val info = issues.info(number = branch.issue)
          val item = board.item(issue = branch.issue, nodeId = issues.nodeId(number = branch.issue))
          checkReady(info = info, status = item.status)
          val copy = when {
              info.type == RESEARCH -> {
                  research(request = request, console = console)
                  null
              }
              request.worktree -> openCopy(request = request, console = console)
              else -> {
                  switchToBranch(request = request, console = console)
                  null
              }
          }
          if (item.status == Board.IN_PROGRESS) {
              console.line(text = "#${branch.issue} уже ${Board.IN_PROGRESS}")
          } else {
              board.move(item = item, status = Board.IN_PROGRESS)
              console.line(text = "#${branch.issue} → ${Board.IN_PROGRESS}")
          }
          if (copy != null) {
              console.line(text = copy)
          }
      }

      private fun parse(args: List<String>): Request {
          val fromIndex = args.indexOf(FROM_FLAG)
          val from = if (fromIndex >= 0) args.getOrNull(index = fromIndex + 1) else null
          val rest = if (fromIndex >= 0) args.take(n = fromIndex) + args.drop(n = fromIndex + 2) else args
          val long = LONG_FLAG in rest
          val worktree = WORKTREE_FLAG in rest
          val positional = rest - LONG_FLAG - WORKTREE_FLAG
          val issue = positional.firstOrNull()?.toIntOrNull()
          val badFrom = fromIndex >= 0 && (long || from == null || BranchName.parse(branch = from)?.long != true)
          if (positional.size != 2 || issue == null || badFrom) {
              throw TaskError.Failure(message = "использование: task start $usage")
          }
          return Request(
              branch = BranchName.of(issue = issue, slug = positional.last(), long = long),
              from = from,
              worktree = worktree,
          )
      }

      private fun checkReady(
          info: IssueInfo,
          status: String,
      ) {
          val reason = when {
              !info.open -> "закрыта"
              info.type == EPIC -> "— эпик: в работу берутся его подзадачи"
              status != Board.GROOMED && status != Board.IN_PROGRESS -> "в статусе «$status» — сначала груминг"
              info.openBlockers.isNotEmpty() -> "заблокирована: ${blockers(info = info)} открыты"
              else -> null
          }
          if (reason != null) {
              throw TaskError.Refusal(message = "#${info.number} $reason")
          }
      }

      private fun blockers(info: IssueInfo): String {
          return info.openBlockers.joinToString(separator = ", ") { blocker -> "#$blocker" }
      }

      private fun research(
          request: Request,
          console: Console,
      ) {
          val issue = request.branch.issue
          if (request.worktree) {
              throw TaskError.Refusal(message = "#$issue — исследование: ветка и копия не нужны")
          }
          console.line(text = "#$issue — исследование: ветка не нужна")
      }

      private fun switchToBranch(
          request: Request,
          console: Console,
      ) {
          val name = request.branch.toString()
          git.fetch()
          val opened = git.openedAt(branch = name)
          if (opened != null && !isSamePath(first = opened, second = git.root())) {
              throw openedRefusal(name = name, folder = opened)
          }
          val local = git.hasLocalBranch(name = name)
          val remote = git.hasRemoteBranch(name = name)
          if (local || remote) {
              switchToExisting(name = name, behind = local && remote && isBehind(name = name), console = console)
          } else {
              createBranch(request = request, copy = null, console = console)
          }
      }

      /** Ветка в копии `.claude/worktrees/<slug>` (#52); возвращает путь копии. */
      private fun openCopy(
          request: Request,
          console: Console,
      ): String {
          val name = request.branch.toString()
          val copy = git.copyPath(slug = request.branch.slug)
          git.fetch()
          val opened = git.openedAt(branch = name)
          if (opened != null) {
              if (!isSamePath(first = opened, second = copy)) {
                  throw openedRefusal(name = name, folder = opened)
              }
              val behind = git.hasRemoteBranch(name = name) && isBehind(name = name)
              if (behind) {
                  git.fastForwardIn(path = copy, name = name)
                  console.line(text = "копия уже есть: $copy — перемотал до origin/$name")
              } else {
                  console.line(text = "копия уже есть: $copy")
              }
              return copy
          }
          val local = git.hasLocalBranch(name = name)
          val remote = git.hasRemoteBranch(name = name)
          val occupied = File(copy).exists()
          if (local || remote) {
              val behind = local && remote && isBehind(name = name)
              if (occupied) {
                  switchOwnCopy(copy = copy, name = name)
              } else {
                  git.addWorktreeOn(path = copy, name = name)
              }
              if (behind) {
                  git.fastForwardIn(path = copy, name = name)
                  console.line(text = "ветка $name уже есть — копия $copy перемотана до origin/$name")
              } else {
                  console.line(text = "ветка $name уже есть — копия $copy")
              }
          } else {
              if (occupied) {
                  throw occupiedRefusal(copy = copy)
              }
              createBranch(request = request, copy = copy, console = console)
          }
          return copy
      }

      /**
       * Папка копии есть, ветка в ней не открыта: своя чистая копия (копия long-ветки после `task done` ветки блока)
       * переключается на ветку; чужая папка или правки — отказ (#52, изменено при плане).
       */
      private fun switchOwnCopy(
          copy: String,
          name: String,
      ) {
          val registered = git.worktrees().any { path -> isSamePath(first = path, second = copy) }
          if (!registered || git.hasChanges(path = copy)) {
              throw occupiedRefusal(copy = copy)
          }
          git.switchIn(path = copy, name = name)
      }

      private fun occupiedRefusal(copy: String): TaskError.Refusal {
          return TaskError.Refusal(message = "папка $copy занята — работайте там или удалите её")
      }

      private fun switchToExisting(
          name: String,
          behind: Boolean,
          console: Console,
      ) {
          git.switchTo(name = name)
          if (behind) {
              git.fastForward(name = name)
              console.line(text = "ветка $name уже есть — переключился и перемотал до origin/$name")
          } else {
              console.line(text = "ветка $name уже есть — переключился на неё")
          }
      }

      /** Локальная [name] отстала от `origin/[name]`; совпадает или впереди — нет; разошлась — отказ (#61). */
      private fun isBehind(name: String): Boolean {
          val local = "refs/heads/$name"
          val remote = "origin/$name"
          if (git.isAncestor(ancestor = remote, descendant = local)) {
              return false
          }
          if (!git.isAncestor(ancestor = local, descendant = remote)) {
              throw TaskError.Refusal(message = "локальная $name расходится с origin/$name — сведите вручную")
          }
          return true
      }

      /** Новая ветка — здесь ([copy] `null`) или сразу в копии [copy] (#52). */
      private fun createBranch(
          request: Request,
          copy: String?,
          console: Console,
      ) {
          val name = request.branch.toString()
          val from = request.from
          if (from != null && !git.hasRemoteBranch(name = from)) {
              throw TaskError.Refusal(message = "нет ветки origin/$from — сначала task start <N> <slug> --long")
          }
          val base = "origin/${from ?: DEV}"
          if (copy == null) {
              git.createBranch(name = name, base = base)
          } else {
              git.addBranchWorktree(path = copy, name = name, base = base)
          }
          if (from != null) {
              git.setBase(branch = name, base = from)
          }
          console.line(text = if (copy == null) "ветка $name от $base" else "ветка $name от $base — копия $copy")
          if (request.branch.long) {
              git.push(name = name)
              console.line(text = "ветка $name отправлена в origin")
          }
      }

      private fun openedRefusal(
          name: String,
          folder: String,
      ): TaskError.Refusal {
          return TaskError.Refusal(message = "ветка $name открыта в $folder — работайте там")
      }

      /** Пути из разных команд `git` (`worktree list`, `rev-parse`) сравниваются после разрешения ссылок. */
      private fun isSamePath(
          first: String,
          second: String,
      ): Boolean {
          return File(first).canonicalPath == File(second).canonicalPath
      }

      /** Разобранные аргументы: ветка задачи, long-ветка, от которой она начинается (`--from`), и `--worktree`. */
      private data class Request(
          val branch: BranchName,
          val from: String?,
          val worktree: Boolean,
      )

      private companion object {
          const val LONG_FLAG = "--long"
          const val FROM_FLAG = "--from"
          const val WORKTREE_FLAG = "--worktree"
          const val DEV = "dev"
          const val EPIC = "type:epic"
          const val RESEARCH = "type:research"
      }
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*StartCommandSpec*'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Задача 3: `.claude/worktrees/` в общем блоке `.gitignore`
- Изменить: `templates/project/gitignore` — строка после `node_modules/`
- Изменить: `.gitignore` — та же строка в блоке `# >>> engineering-standards` … `# <<< engineering-standards`, после `node_modules/`

Тест: не применим — шаблон без логики; проверка формата — `scripts/setup --check` (сверяет блок `.gitignore` с шаблоном).
- [ ] 1. В `templates/project/gitignore` после строки `node_modules/` добавить строку `.claude/worktrees/`
- [ ] 2. В `.gitignore` после `node_modules/` внутри блока (перед `# <<< engineering-standards`) добавить `.claude/worktrees/`
- [ ] 3. Запустить `scripts/setup --check` — код выхода 0, в выводе нет расхождения по `.gitignore`
- [ ] 4. Коммит

### Приёмка
- [ ] Что даёт этот PR — проверено: `./gradlew :scripts:test --tests '*StartCommandSpec*' --tests '*GitSpec*'` зелёный
- [ ] Крайние случаи обработаны: грязная основная папка с `--worktree`, повтор с перемоткой, отставшая локальная ветка без копии, `--from` с `--worktree`, отсоединённая своя копия переключается, чужая папка / занятый slug — отказ, ветка открыта в основной папке / в копии, исследование + `--worktree`, отказ `checkReady` с `--worktree`
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`

## PR 2: копия настраивается по `.worktreeinclude`
**Объём:** `Git.kt`, `GitSpec.kt`, `GitSpecData.kt`, `WorktreeFiles.kt`, `WorktreeFilesSpec.kt`, `WorktreeFilesSpecData.kt`, `StartCommand.kt`, `StartCommandSpec.kt`, `Main.kt`, `.worktreeinclude`; ≈ 10 файлов

### Задача 4: Git — файлы основной папки по `.worktreeinclude`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt` — метод `includedPaths` после `copyPath`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpecData.kt` — `makeCloneWithIgnoredFiles`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt` — случай в конце

- [ ] 1. Падающий тест. В `GitSpecData.kt` (после `makeCloneWithPlanBranch`):
  ```kotlin
  /**
   * Клон с неотслеживаемыми файлами: `.gitignore` игнорирует `local.properties`, `conf/` и `build/`; `notes.txt` не
   * игнорируется. Рядом с клоном — список `include` (`local.properties`, `conf/`, `notes.txt`) в формате
   * `.worktreeinclude`.
   */
  fun makeCloneWithIgnoredFiles(): File {
      val clone = makeClone()
      File(clone, ".gitignore").writeText("local.properties\nconf/\nbuild/\n")
      File(clone, "local.properties").writeText("sdk.dir=/sdk\n")
      File(clone, "conf").mkdirs()
      File(clone, "conf/a.xml").writeText("<a/>\n")
      File(clone, "build").mkdirs()
      File(clone, "build/out.txt").writeText("out\n")
      File(clone, "notes.txt").writeText("заметки\n")
      File(clone.parentFile, "include").writeText("local.properties\nconf/\nnotes.txt\n")
      return clone
  }
  ```
  В конец `GitSpec`:
  ```kotlin
        shouldReturnTest(
            listOf("conf/a.xml", "local.properties"),
            options = TestOptions(name = "файлы по списку — только игнорируемые git"),
        ) {
            val clone = makeCloneWithIgnoredFiles()
            Git(shell = ProcessShell(directory = clone)).includedPaths(
                path = clone.path,
                listFile = File(clone.parentFile, "include").path,
            )
        }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: `Unresolved reference 'includedPaths'`
- [ ] 3. Минимальный код: в `Git.kt` после `copyPath`
  ```kotlin
    /**
     * Неотслеживаемые файлы папки [path], которые игнорирует git и которые подходят под шаблоны файла [listFile] — так
     * отбирает `.worktreeinclude` Claude Code (#52). Шаблоны разбирает git.
     */
    fun includedPaths(
        path: String,
        listFile: String,
    ): List<String> {
        val listed = readIn(path = path, args = listOf("ls-files", "--others", "--ignored", "--exclude-from=$listFile"))
            .lines()
            .filter { line -> line.isNotBlank() }
        if (listed.isEmpty()) {
            return emptyList()
        }
        val ignoredArgs = listOf("ls-files", "--others", "--ignored", "--exclude-standard", "--")
        return readIn(path = path, args = ignoredArgs + listed)
            .lines()
            .filter { line -> line.isNotBlank() }
    }
  ```
  Порядок вызовов — короткий список по `.worktreeinclude` первым, проверка игнорирования только его путей: не перечислять все игнорируемые файлы основной папки (`build/`, `.gradle/`, сборки копий) — ревью плана, В1.
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — `BUILD SUCCESSFUL`. Упал на `conf/a.xml` (git выдал `conf/` вместо файла) — ⚠️ в план и сообщить: разбор папок расходится с ожиданием, не подгонять тест молча
- [ ] 5. Коммит — тест и код вместе

### Задача 5: `WorktreeFiles` копирует файлы в копию
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/WorktreeFiles.kt` — копирует в копию файлы основной папки по `.worktreeinclude`; вход: `copyInto(copy: String, console: Console)`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/WorktreeFilesSpec.kt` — три случая
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/WorktreeFilesSpecData.kt` — временные папки

- [ ] 1. Падающий тест. `WorktreeFilesSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import java.io.File
  import java.nio.file.Files

  /** Основная папка с `local.properties` и `.idea/x.xml`; [include] — содержимое `.worktreeinclude`, `null` — файла нет. */
  fun makeMainFolder(include: String?): File {
      val main = makeFolder(prefix = "main")
      File(main, "local.properties").writeText("sdk.dir=/sdk\n")
      File(main, ".idea").mkdirs()
      File(main, ".idea/x.xml").writeText("<x/>\n")
      if (include != null) {
          File(main, ".worktreeinclude").writeText(include)
      }
      return main
  }

  fun makeCopyFolder(): File {
      return makeFolder(prefix = "copy")
  }

  /** Временная папка в `build/` модуля: её убирает `./gradlew clean`. */
  private fun makeFolder(prefix: String): File {
      val base = File(System.getProperty("task.cache"), "worktree-files")
      base.mkdirs()
      return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ prefix).toFile()
  }
  ```
  `WorktreeFilesSpec.kt`:
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
  import io.mockk.verify
  import io.mockk.verifySequence
  import java.io.File

  class WorktreeFilesSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val git = mockk<Git>()
          val console = mockk<Console>()
          val files = WorktreeFiles(git = git)
          justRun { console.line(text = any()) }

          given("нет .worktreeinclude") {
              val main = makeMainFolder(include = null)
              val copy = makeCopyFolder()
              every { git.mainRoot() } returns main.path
              `when` {
                  files.copyInto(copy = copy.path, console = console)
                  then {
                      verifySequence { git.mainRoot() }
                      verify(exactly = 0) { console.line(text = any()) }
                  }
              }
          }

          given("список — файл и папка") {
              val main = makeMainFolder(include = "local.properties\n.idea/\n")
              val copy = makeCopyFolder()
              every { git.mainRoot() } returns main.path
              every {
                  git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
              } returns listOf(".idea/x.xml", "local.properties")
              `when` {
                  files.copyInto(copy = copy.path, console = console)
                  then {
                      File(copy, "local.properties").readText() shouldBe "sdk.dir=/sdk\n"
                      File(copy, ".idea/x.xml").readText() shouldBe "<x/>\n"
                      verifySequence {
                          git.mainRoot()
                          git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
                          console.line(text = "из .worktreeinclude скопировано: .idea/x.xml, local.properties")
                      }
                  }
              }
          }

          given("строки-пути, которых нет в основной папке; шаблоны, комментарии и исключения не проверяются") {
              val main = makeMainFolder(include = "local.properties\nsecret.env\nkeys/\n*.jks\n# комментарий\n!a.txt\n")
              val copy = makeCopyFolder()
              every { git.mainRoot() } returns main.path
              every {
                  git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
              } returns listOf("local.properties")
              `when` {
                  files.copyInto(copy = copy.path, console = console)
                  then {
                      verifySequence {
                          git.mainRoot()
                          git.includedPaths(path = main.path, listFile = File(main, ".worktreeinclude").path)
                          console.line(text = "из .worktreeinclude скопировано: local.properties")
                          console.line(text = ".worktreeinclude: secret.env — нет в основной папке, пропущено")
                          console.line(text = ".worktreeinclude: keys/ — нет в основной папке, пропущено")
                      }
                  }
              }
          }
      })
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*WorktreeFilesSpec*'` — падает: `Unresolved reference 'WorktreeFiles'`
- [ ] 3. Минимальный код: `WorktreeFiles.kt`
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.git.Git
  import java.io.File

  /**
   * Настройка копии задачи (#52): неотслеживаемые файлы основной папки, которые игнорирует git и которые подходят под
   * шаблоны `.worktreeinclude` (его понимает и Claude Code), копируются в копию — без них не собрать
   * (`local.properties`). Нет `.worktreeinclude` — ничего. Строка-путь без шаблона, которого нет в основной папке, —
   * строка в выводе.
   */
  class WorktreeFiles(
      private val git: Git,
  ) {
      fun copyInto(
          copy: String,
          console: Console,
      ) {
          val main = File(git.mainRoot())
          val list = File(main, LIST)
          if (!list.isFile) {
              return
          }
          val paths = git.includedPaths(path = main.path, listFile = list.path)
          for (path in paths) {
              File(main, path).copyTo(target = File(copy, path), overwrite = true)
          }
          if (paths.isNotEmpty()) {
              console.line(text = "из $LIST скопировано: ${paths.joinToString(separator = ", ")}")
          }
          for (line in missing(list = list, main = main)) {
              console.line(text = "$LIST: $line — нет в основной папке, пропущено")
          }
      }

      /** Строки-пути без шаблонных символов, комментариев и исключений, которых нет в основной папке. */
      private fun missing(
          list: File,
          main: File,
      ): List<String> {
          return list.readLines()
              .map { line -> line.trim() }
              .filter { line -> line.isNotEmpty() && line.first() !in SPECIAL && line.none { char -> char in PATTERN } }
              .filter { line -> !File(main, line.removeSuffix(suffix = "/")).exists() }
      }

      private companion object {
          const val LIST = ".worktreeinclude"
          const val SPECIAL = "#!"
          const val PATTERN = "*?["
      }
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*WorktreeFilesSpec*'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Задача 6: `task start --worktree` копирует файлы, `.worktreeinclude` этого репозитория
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/StartCommand.kt` — параметр `files`, вызов после создания копии
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/StartCommandSpec.kt` — мок `files`, вызов в последовательностях
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:44` — `files = WorktreeFiles(git = git)`
- Создать: `.worktreeinclude` — `local.properties`

- [ ] 1. Падающий тест. В `StartCommandSpec.kt`:
  - после `val console = mockk<Console>()` — `val files = mockk<WorktreeFiles>()`; конструктор — `StartCommand(git = git, issues = issues, board = board, files = files)`; к заглушкам — `justRun { files.copyInto(copy = any(), console = any()) }`;
  - в случаях «--worktree, ветки нет, основная папка грязная», «--long --worktree: новая long-ветка в копии» и «--from long/… --worktree…» сразу после строки `console.line(text = "ветка … от origin/… — копия …")` вставить `files.copyInto(copy = <путь копии случая>, console = console)` (до `git.push` в long-случае);
  - в случаях «--worktree, ветка есть только в origin» и «--worktree, локальная ветка отстала…» после строки `console.line(text = "ветка task/13-task-scripts уже есть — копия …")` вставить `files.copyInto(copy = makeCopyPath(), console = console)`;
  - в случаях «--worktree повторно…» и «…своя копия long-ветки отсоединена…» — без вызова (`verifySequence` подтверждает, что существующая копия файлы не получает).
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*StartCommandSpec*'` — падает: `No parameter with name 'files' found`
- [ ] 3. Минимальный код в `StartCommand.kt`:
  - конструктор: после `private val board: Board,` — `private val files: WorktreeFiles,`
  - KDoc класса: в фразу про `--worktree` после «основной папки:» добавить «файлы из `.worktreeinclude` копируются при создании,»
  - в `openCopy`, ветка «существующая»: после `if (behind) { … } else { … }` добавить
    ```kotlin
              if (!occupied) {
                  files.copyInto(copy = copy, console = console)
              }
    ```
  - в `createBranch`: после `console.line(text = if (copy == null) … )` и до `if (request.branch.long)` добавить
    ```kotlin
          if (copy != null) {
              files.copyInto(copy = copy, console = console)
          }
    ```
  В `Main.kt:44`: `"start" to StartCommand(git = git, issues = issues, board = board, files = WorktreeFiles(git = git)),` и импорт `io.github.nakulov.standards.task.command.WorktreeFiles`.
  Файл `.worktreeinclude` в корне репозитория — одна строка `local.properties`.
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*StartCommandSpec*'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Что даёт этот PR — проверено: `./gradlew :scripts:test --tests '*WorktreeFilesSpec*' --tests '*StartCommandSpec*' --tests '*GitSpec*'` зелёный
- [ ] Крайние случаи обработаны: нет `.worktreeinclude`, путь из списка отсутствует, шаблоны / комментарии / исключения не дают ложных строк, повтор не копирует
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`

## PR 3: `task done` удаляет свою копию
**Объём:** `DoneCommand.kt`, `DoneCommandSpec.kt`, `GitSpec.kt`; ≈ 3 файла

### Задача 7: удаление своей копии в `task done`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/DoneCommand.kt:42` — `leaveBranch` возвращает `Boolean`, новый `removeOwnCopy`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/DoneCommandSpec.kt` — заглушки, случай «PR с Part of слит в worktree», новый случай
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt` — удаление копии изнутри неё на настоящем git (ревью плана, М4)

- [ ] 1. Падающий тест. В конец `GitSpec` — проверка, на которую опирается решение 6 (метод `removeWorktree` уже есть, случай должен пройти сразу — это проверка поведения git, не новый код):
  ```kotlin
        shouldReturnTest(false, options = TestOptions(name = "копия удаляет себя изнутри, без -C")) {
            val worktree = makeWorktree()
            Git(shell = ProcessShell(directory = worktree)).removeWorktree(path = worktree.path)
            worktree.exists()
        }
  ```
  В `DoneCommandSpec.kt`:
  - к заглушкам (после `every { issues.nodeId(number = 13) } returns "I_issue13"`):
    ```kotlin
        justRun { git.removeWorktree(path = any()) }
        every { git.root() } returns "/repo/.claude/worktrees/task-scripts"
        every { git.copyPath(slug = "task-scripts") } returns "/repo/.claude/worktrees/task-scripts"
    ```
  - в случае «PR с Part of слит в worktree» в конец `verifySequence` (после строки статуса `console.line(text = "#13 открыта — …")`) добавить
    ```kotlin
                        git.root()
                        git.copyPath(slug = "task-scripts")
                        git.removeWorktree(path = "/repo/.claude/worktrees/task-scripts")
                        console.line(
                            text = "копия /repo/.claude/worktrees/task-scripts удалена — текущей папки больше нет",
                        )
    ```
  - новый случай перед последним `})`:
    ```kotlin
        given("ветка блока task/13-big-pr1 слита в копии long-ветки .claude/worktrees/big") {
            every { git.currentBranch() } returns "task/13-big-pr1"
            every { issues.pullRequest(branch = "task/13-big-pr1") } returns PullRequest(number = 40, merged = true)
            every { git.isWorktree() } returns true
            every { git.root() } returns "/repo/.claude/worktrees/big"
            every { git.copyPath(slug = "big-pr1") } returns "/repo/.claude/worktrees/big-pr1"
            every { issues.info(number = 13) } returns makeTaskInfo()
            `when` {
                command.run(args = emptyList(), console = console)
                then {
                    verifySequence {
                        git.currentBranch()
                        git.isClean()
                        issues.pullRequest(branch = "task/13-big-pr1")
                        git.fetch()
                        git.isWorktree()
                        git.detach(ref = "origin/dev")
                        console.line(text = "worktree отсоединён на origin/dev, dev не тронут")
                        git.deleteBranch(name = "task/13-big-pr1")
                        console.line(text = "ветка task/13-big-pr1 удалена")
                        issues.info(number = 13)
                        console.line(text = "#13 открыта — не последний PR задачи, статус остаётся")
                        git.root()
                        git.copyPath(slug = "big-pr1")
                    }
                }
            }
        }
    ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*DoneCommandSpec*' --tests '*GitSpec*'` — `GitSpec` зелёный (git удаляет копию изнутри; упал — ⚠️ и стоп: решение 6 не держится, нужен запасной вариант); `DoneCommandSpec` падает: в последовательности нет `git.root()` (`verifySequence` не совпадает)
- [ ] 3. Минимальный код в `DoneCommand.kt`:
  - KDoc класса: после «задача → Done.» добавить «Своя копия `.claude/worktrees/<slug>` удаляется последним шагом — после неё текущей папки нет; копия long-ветки на ветке блока остаётся (#52).»
  - в `run` заменить две последние строки на
    ```kotlin
          val worktree = leaveBranch(name = name, console = console)
          finishIssue(issue = branch.issue, console = console)
          if (worktree) {
              removeOwnCopy(branch = branch, console = console)
          }
    ```
  - `leaveBranch` — возвращает `Boolean`:
    ```kotlin
      private fun leaveBranch(
          name: String,
          console: Console,
      ): Boolean {
          git.fetch()
          val worktree = git.isWorktree()
          if (worktree) {
              git.detach(ref = BASE)
              console.line(text = "worktree отсоединён на $BASE, dev не тронут")
          } else {
              git.updateDev()
              console.line(text = "dev обновлён до $BASE")
          }
          git.deleteBranch(name = name)
          console.line(text = "ветка $name удалена")
          return worktree
      }
    ```
  - после `finishIssue`:
    ```kotlin
      /**
       * Своя копия — `.claude/worktrees/<slug>` этой ветки (#52): удаляется изнутри, без `git -C` — защита сессии в ворктри
       * не пускает `git` в основную папку. Копия long-ветки, где слита ветка блока, остаётся.
       */
      private fun removeOwnCopy(
          branch: BranchName,
          console: Console,
      ) {
          val root = git.root()
          if (File(root).canonicalPath != File(git.copyPath(slug = branch.slug)).canonicalPath) {
              return
          }
          git.removeWorktree(path = root)
          console.line(text = "копия $root удалена — текущей папки больше нет")
      }
    ```
  - импорт `java.io.File`
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*DoneCommandSpec*'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Что даёт этот PR — проверено: `./gradlew :scripts:test --tests '*DoneCommandSpec*'` зелёный
- [ ] Крайние случаи обработаны: своя копия удаляется, копия long-ветки на ветке блока остаётся, основная папка — как раньше
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам (живой прогон — в «После завершения»)

## Открыто
- Блокирует ли защита сессии в ворктри `scripts/task done` (опыт был на прямом `git`) — ассистент — живой прогон после завершения; блокирует — запасной вариант.
- Куда записать «касается #48» (шаг 2 `execute`, черновик `worktree`) — пользователь — после завершения.
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка: в основной папке `scripts/task start 52 <slug> --worktree` на тестовой задаче → последняя строка — путь копии; `EnterWorktree path` → `./gradlew :scripts:test` в копии проходит (есть `local.properties`) → после слияния PR `scripts/task done` в копии → «копия … удалена» → `ExitWorktree keep` → сессия в основной папке, `git worktree list` без копии, `git status` чистый.
