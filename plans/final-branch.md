# Итоговый PR из ветки final

**Цель:** `task final <N>` открывает итоговый PR из ветки `task/<N>-final`, а не из защищённой long-ветки; `task done` после его слияния удаляет и final, и long.
**Архитектура:** `FinalCommand` готовит ветку `task/<N>-final` от `origin/long/<N>-…` (повтор — вливает в неё свежую long), переключается на неё и отдаёт `Publisher` черновик с `head = task/<N>-final` — архив плана и push идут на final. `DoneCommand` на `task/<N>-final` находит long по номеру задачи (`Git.longBranch`), удаляет её локально и в `origin`, копию-ворктри определяет по slug long.
**Стек:** Kotlin, Kotest BehaviorSpec + MockK, kotestkit (`shouldReturnTest`, `shouldThrowWithMessageTest`), настоящий `git` в `GitSpec`.

---

## Ограничения
- Не трогать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/Rulesets.kt` — ruleset `standards/long` (`:239-240`, запрет push и перезаписи) не ослабляем; обход отклонён в #66.
- Не трогать: `task/command/Publisher.kt`, `task/command/PlanArchive.kt` — общие с `task pr`; план убирается на текущей ветке, поэтому `task final` сначала переключается на final.
- Работа и в основной папке, и в копии-ворктри `.claude/worktrees/<slug>` (#52).
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: итоговый PR `task/<N>-final` → `dev`; повтор `task final` вливает свежую long в final; `task done` из final удаляет long локально и в `origin`.
- Какую проблему решает: `task final` пушил коммит «Убрать план» прямо в `long/<N>-…` — ruleset `standards/long` отклонял push (GH013), PR не создавался (#66, воспроизведено на #52).
- Как встраивается: `FinalCommand` → `Publisher` (без изменений) → `PlanArchive` на ветке final; `DoneCommand` — новый шаг после ухода с ветки.
- Критерии цели:
  - Тест, который воспроизводит баг, падает до исправления и проходит после (`FinalCommandSpec`: `head` черновика = `task/13-final`).
  - `task final` на плане из нескольких блоков при включённом `standards/long` открывает PR в `dev`, план в него не попадает.
  - После слияния итогового PR `task done` удаляет `long/<N>-…` и `task/<N>-final`.

## Контекст
- Задействованные файлы (корень кода — `scripts/src/main/kotlin/io/github/nakulov/standards/task/`, тестов — `scripts/src/test/kotlin/io/github/nakulov/standards/task/`):
  - `command/FinalCommand.kt:29-42` — сейчас head PR = текущая long-ветка
  - `command/Publisher.kt:28-36` — PR в `dev` с `Closes` → `PlanArchive.run` на текущей ветке → `git.push(draft.head)`
  - `command/DoneCommand.kt:45-49, 65, 89-99` — удаляет только текущую ветку; копию ищет по slug текущей ветки
  - `git/Git.kt:38-44` `hasLocalBranch`/`hasRemoteBranch`, `:47` `createBranch`, `:55` `switchTo`, `:79` `deleteBranch`, `:84` `push`, `:96-100` `base`, `:183` `openedAt`, приватные `read`/`run`
  - `git/BranchName.kt:9-17` — `BranchName(issue, slug, long).toString()` → `task/<N>-<slug>`
- Паттерны: команды получают `Git`/`Issues`/`Board` в конструкторе; спеки команд — MockK + `verifySequence`; `GitSpec` — настоящий `git` во временных репозиториях, подготовка — в `GitSpecData`.
- Зависимости: `Publisher` и `PlanArchive` не меняются; справка `task --help` сверяется в `cli/TaskLauncherSpecData.kt:21`.

## Подход к разработке
- Завершай задачу целиком до следующей.
- Небольшие сфокусированные изменения.
- **ВАЖНО:** каждая задача — с новыми или обновлёнными тестами для её кода.
- **ВАЖНО:** все тесты зелёные до начала следующей задачи.
- **ВАЖНО:** изменение объёма — в этот план, не молча.
- После каждого изменения — конкретный тест; полный набор — в конце задачи.
- Обратная совместимость — не нужна: `task final` из long продолжает работать, меняется только ветка PR (этап 3).

## Структура файлов
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt:101` — после `base`: `longBranch(issue)`, `merge(ref)`, `deleteRemoteBranch(name)`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt` — случаи трёх новых методов
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpecData.kt` — репозитории с long-веткой и веткой final
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/FinalCommand.kt` — PR из `task/<N>-final`, повтор
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/FinalCommandSpec.kt`, `FinalCommandSpecData.kt` — первый запуск, повторы, отказы
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt:21` — новая строка usage в справке
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/DoneCommand.kt:45-99` — удаление long после final
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/DoneCommandSpec.kt`, `DoneCommandSpecData.kt` — final в копии, long не найдена

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: итоговый PR — из отдельной ветки `task/<N>-final` от long (груминг #66, «Согласовано»); защита long остаётся полной, меняется только скрипт.
- Решения:
  - После `task final` пользователь остаётся на `task/<N>-final` — как после `task pr`: правки по ревью и `task done` — оттуда (этап 3, деталь 1).
  - Повтор — `git merge` свежей `origin/long/<N>-…` в final, без перезаписи истории; конфликт — отказ «разрешите, закоммитьте, повторите» (этап 3, деталь 2).
  - `task done` из final удаляет long и локально, и в `origin` — иначе long, которая больше не бывает веткой PR, оставалась бы на `origin` навсегда; ruleset удаление не запрещает (этап 3, деталь 3).
  - `task done` до удаления final проверяет, что long не открыта в другой папке (`Git.openedAt`, `Git.kt:183`): иначе final уже удалена, а long удалить нельзя — повторить `task done` не из чего (ревью плана, М1).
  - Отказ слияния называет обе причины — конфликт или нет `origin/long/…`: `Git.merge` их не различает (ревью плана, М2).
  - Long ищется по номеру задачи (`long/<N>-*`, локально, затем в `origin`), а не через `Git.setBase`: база ветки читается `task pr` как цель PR (`Git.kt:88`, `base` — `:96-100`), и `task pr` на final открыл бы PR в long (этап 3, поправка к детали 1).
- Как вписывается в систему: `Publisher` получает `head = task/<N>-final` и уже стоит на ней — архив плана, push и поиск PR по ветке работают без изменений.
- Запасной: пересоздавать final от long + force-push в `task/**` — переходим, если слияние long в final окажется неработоспособным на практике (постоянные конфликты по файлу плана).
- Отклонено:
  - Убирать план в PR последнего блока — нужен признак «последний блок» в `task pr` и правка `execute`; плана нет на ветке во время ревью последнего блока (#66).
  - Обход ruleset `standards/long` — ослабляет защиту long-веток ради одного коммита (#66).
  - Пересоздание final с force-push — теряет правки, сделанные прямо на final (этап 3).
  - Повтор только из final — расходится с «обновить её от long-ветки» из #66 (этап 3).
  - Имя long через `Git.setBase` — `task pr` на final открыл бы PR в long (этап 3).

## Технические детали
- Структуры данных и изменения: новых типов нет. `FinalCommand.SLUG = "final"` — общий для `FinalCommand` и `DoneCommand`.
- Параметры и форматы:
  - `Git.longBranch(issue: Int): String?` — `git for-each-ref --format=%(refname) refs/heads/long/<N>-* refs/remotes/origin/long/<N>-*`, префиксы срезаются, первое имя или `null`; `long/7-*` не совпадает с `long/70-…`.
  - `Git.merge(ref: String): Boolean` — `git merge --quiet --no-edit <ref>`, код 0 → `true`; иначе `false`, слияние остаётся пользователю.
  - `Git.deleteRemoteBranch(name: String)` — `git push --quiet origin --delete <name>`.
- Поток обработки `task final <N>`:
  1. Текущая ветка: `long/<N>-…` → она и есть long; `task/<N>-final` → long по `git.longBranch(N)` (нет — отказ); иная — отказ.
  2. Дерево грязное — отказ (до переключения веток).
  3. `git.fetch()`.
  4. Final нет ни локально, ни в `origin` → `createBranch(final, origin/<long>)`; есть → переключиться (если не на ней) и `merge(origin/<long>)`, `false` — отказ.
  5. `Publisher.publish` с `head = task/<N>-final` → «Убрать план» на final, push final, PR создан или обновлён.
  6. Задача → In review.
- Поток `task done` на `task/<N>-final`: проверки как сейчас → `longBranch(N)`; есть и открыта в другой папке (`openedAt`) — отказ, final не тронута → уход с ветки и удаление final → long есть — удалить локально (если есть) и в `origin` (если есть), нет — строка «long-ветки нет» → статус → копия-ворктри по slug long.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: итоговый PR long-задачи открывается без push в long-ветку
  - [ ] Задача 1: Git — поиск long-ветки, слияние, удаление в origin
  - [ ] Задача 2: task final — PR из `task/<N>-final`
  - [ ] Задача 3: task done — удаление long после итогового PR
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Ручная проверка `task final` на настоящей long-задаче при включённом `standards/long` (см. «После завершения»).
- `task done` после слияния итогового PR — обе ветки удалены.

## PR 1: итоговый PR long-задачи открывается без push в long-ветку
**Объём:** `git/Git.kt`, `GitSpec.kt`, `GitSpecData.kt`, `command/FinalCommand.kt`, `FinalCommandSpec.kt`, `FinalCommandSpecData.kt`, `cli/TaskLauncherSpecData.kt`, `command/DoneCommand.kt`, `DoneCommandSpec.kt`, `DoneCommandSpecData.kt`; ≈ 10 файлов

### Задача 1: Git — поиск long-ветки, слияние, удаление в origin
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt` — три метода после `base` (`:96-100`)
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpecData.kt` — три подготовки репозитория
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt` — шесть случаев в конце спеки

- [ ] 1. Падающий тест: в `GitSpecData.kt` перед `private fun makeTempDirectory()`:
  ```kotlin
  /** Клон с long-веткой `long/7-big` — локальной и в `origin`; рядом `long/70-other`: номер с тем же началом. */
  fun makeCloneWithLongBranch(): File {
      val clone = makeClone()
      git(directory = clone, args = listOf("branch", "--quiet", "long/70-other"))
      git(directory = clone, args = listOf("branch", "--quiet", "long/7-big"))
      git(directory = clone, args = listOf("push", "--quiet", "origin", "long/70-other", "long/7-big"))
      return clone
  }

  /** Как [makeCloneWithLongBranch], но локальной `long/7-big` нет — только в `origin`. */
  fun makeCloneWithRemoteLongBranch(): File {
      val clone = makeCloneWithLongBranch()
      git(directory = clone, args = listOf("branch", "--quiet", "-D", "long/7-big"))
      return clone
  }

  /**
   * Клон с личностью на ветке `task/7-final` от `dev`; в `origin/long/7-big` — коммит «блок слит» с `b.txt`.
   * [conflict] — на final свой `b.txt`: слияние long с конфликтом.
   */
  fun makeFinalBranchBehindLong(conflict: Boolean): File {
      val clone = makeIdentityClone()
      git(directory = clone, args = listOf("switch", "--quiet", "--create", "long/7-big"))
      File(clone, "b.txt").writeText("long\n")
      git(directory = clone, args = listOf("add", "b.txt"))
      git(directory = clone, args = listOf("commit", "--quiet", "--message", "блок слит"))
      git(directory = clone, args = listOf("push", "--quiet", "origin", "long/7-big"))
      git(directory = clone, args = listOf("switch", "--quiet", "--create", "task/7-final", "dev"))
      if (conflict) {
          File(clone, "b.txt").writeText("final\n")
          git(directory = clone, args = listOf("add", "b.txt"))
          git(directory = clone, args = listOf("commit", "--quiet", "--message", "правка по ревью"))
      }
      return clone
  }
  ```
  В `GitSpec.kt` перед закрывающим `})`:
  ```kotlin
        shouldReturnTest("long/7-big", options = TestOptions(name = "long-ветка задачи — локальная")) {
            Git(shell = ProcessShell(directory = makeCloneWithLongBranch())).longBranch(issue = 7)
        }

        shouldReturnTest("long/7-big", options = TestOptions(name = "long-ветка задачи — только в origin")) {
            Git(shell = ProcessShell(directory = makeCloneWithRemoteLongBranch())).longBranch(issue = 7)
        }

        shouldReturnTest(null, options = TestOptions(name = "у задачи нет long-ветки")) {
            Git(shell = ProcessShell(directory = makeClone())).longBranch(issue = 7)
        }

        shouldReturnTest(listOf(true, true), options = TestOptions(name = "свежая long влита в final")) {
            val clone = makeFinalBranchBehindLong(conflict = false)
            val merged = Git(shell = ProcessShell(directory = clone)).merge(ref = "origin/long/7-big")
            listOf(merged, File(clone, "b.txt").exists())
        }

        shouldReturnTest(false, options = TestOptions(name = "слияние long в final с конфликтом")) {
            Git(shell = ProcessShell(directory = makeFinalBranchBehindLong(conflict = true)))
                .merge(ref = "origin/long/7-big")
        }

        shouldReturnTest(false, options = TestOptions(name = "long-ветка удалена в origin")) {
            val git = Git(shell = ProcessShell(directory = makeCloneWithLongBranch()))
            git.deleteRemoteBranch(name = "long/7-big")
            git.hasRemoteBranch(name = "long/7-big")
        }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*.GitSpec'` — падает: `Unresolved reference 'longBranch'` (и `merge`, `deleteRemoteBranch`)
- [ ] 3. Минимальный код: в `Git.kt` после `fun base(...)`:
  ```kotlin
    /**
     * Long-ветка задачи [issue] — локальная или только в `origin`; нет — `null`. Нужна `task final` и `task done` из
     * ветки итогового PR `task/<N>-final` (#66).
     */
    fun longBranch(issue: Int): String? {
        val refs = listOf("$HEADS$LONG$issue-*", "$REMOTES$LONG$issue-*")
        return read(args = listOf("for-each-ref", "--format=%(refname)") + refs)
            .lines()
            .filter { line -> line.isNotBlank() }
            .map { line -> line.removePrefix(prefix = HEADS).removePrefix(prefix = REMOTES) }
            .firstOrNull()
    }

    /** Слияние [ref] в текущую ветку; не прошло (конфликт) — `false`, незавершённое слияние остаётся пользователю. */
    fun merge(ref: String): Boolean {
        return run(args = listOf("merge", "--quiet", "--no-edit", ref)).code == 0
    }

    /** Удаление ветки в `origin`: long-ветку после итогового PR из `task/<N>-final` (#66). */
    fun deleteRemoteBranch(name: String) {
        read(args = listOf("push", "--quiet", "origin", "--delete", name))
    }
  ```
  В `companion object` `Git` добавить:
  ```kotlin
        const val HEADS = "refs/heads/"
        const val REMOTES = "refs/remotes/origin/"
        const val LONG = "long/"
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*.GitSpec'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе: «Git ищет long-ветку задачи, вливает ветку и удаляет её в origin»

### Задача 2: task final — PR из `task/<N>-final`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/FinalCommand.kt` — целиком
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/FinalCommandSpec.kt` — целиком
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/FinalCommandSpecData.kt` — целиком
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt:21` — строка `task final`

- [ ] 1. Падающий тест. `FinalCommandSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  fun makeFinalDraft(): PullRequestDraft {
      return PullRequestDraft(
          head = "task/13-final",
          base = "dev",
          link = "Closes #13",
          title = "Заголовок",
          text = makeBodyText(),
          checks = listOf("./gradlew test", "scripts/task --help"),
          draft = false,
      )
  }

  fun makeFinalUsageMessage(): String {
      return "использование: task final <N> --title «…» --body-file <файл> [--check «команда»]… — " +
          "итоговый PR task/<N>-final → dev из long/<N>-…"
  }

  fun makeFinalWrongBranchMessage(): String {
      return "task final 13 запускается из long/13-… или task/13-final, сейчас «task/13-x»"
  }

  fun makeFinalMergeMessage(): String {
      return "слияние origin/long/13-x в task/13-final не прошло — разрешите конфликт и закоммитьте или проверьте, " +
          "что origin/long/13-x существует; затем повторите task final 13"
  }

  fun makeFinalDirtyMessage(): String {
      return "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их"
  }
  ```
  `FinalCommandSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.kotestkit.then
  import io.github.nakulov.kotestkit.`when`
  import io.github.nakulov.standards.task.board.Board
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.Git
  import io.github.nakulov.standards.task.github.Issues
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.mockk.every
  import io.mockk.justRun
  import io.mockk.mockk
  import io.mockk.verifySequence

  class FinalCommandSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val git = mockk<Git>()
          val issues = mockk<Issues>()
          val board = mockk<Board>()
          val publisher = mockk<Publisher>()
          val console = mockk<Console>()
          val command = FinalCommand(git = git, issues = issues, board = board, publisher = publisher)
          justRun { console.line(text = any()) }
          justRun { publisher.publish(draft = any(), console = console) }
          justRun { board.move(item = any(), status = any()) }
          justRun { git.fetch() }
          justRun { git.createBranch(name = any(), base = any()) }
          justRun { git.switchTo(name = any()) }
          every { git.isClean() } returns true
          every { git.currentBranch() } returns "long/13-x"
          every { git.hasLocalBranch(name = "task/13-final") } returns false
          every { git.hasRemoteBranch(name = "task/13-final") } returns false
          every { git.merge(ref = "origin/long/13-x") } returns true
          every { git.longBranch(issue = 13) } returns "long/13-x"
          every { issues.nodeId(number = 13) } returns "I_issue13"
          every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()

          given("первый итоговый PR из long-ветки") {
              `when` {
                  command.run(args = listOf("13") + makeDraftArgs(), console = console)
                  then {
                      verifySequence {
                          git.currentBranch()
                          git.isClean()
                          git.fetch()
                          git.hasLocalBranch(name = "task/13-final")
                          git.hasRemoteBranch(name = "task/13-final")
                          git.createBranch(name = "task/13-final", base = "origin/long/13-x")
                          publisher.publish(draft = makeFinalDraft(), console = console)
                          issues.nodeId(number = 13)
                          board.item(issue = 13, nodeId = "I_issue13")
                          board.move(item = makeInProgressItem(), status = "In review")
                          console.line(text = "#13 → In review")
                      }
                  }
              }
          }

          given("повтор из ветки final") {
              every { git.currentBranch() } returns "task/13-final"
              every { git.hasLocalBranch(name = "task/13-final") } returns true
              `when` {
                  command.run(args = listOf("13") + makeDraftArgs(), console = console)
                  then {
                      verifySequence {
                          git.currentBranch()
                          git.longBranch(issue = 13)
                          git.isClean()
                          git.fetch()
                          git.hasLocalBranch(name = "task/13-final")
                          git.merge(ref = "origin/long/13-x")
                          publisher.publish(draft = makeFinalDraft(), console = console)
                          issues.nodeId(number = 13)
                          board.item(issue = 13, nodeId = "I_issue13")
                          board.move(item = makeInProgressItem(), status = "In review")
                          console.line(text = "#13 → In review")
                      }
                  }
              }
          }

          given("повтор из long-ветки, final только в origin") {
              every { git.hasRemoteBranch(name = "task/13-final") } returns true
              `when` {
                  command.run(args = listOf("13") + makeDraftArgs(), console = console)
                  then {
                      verifySequence {
                          git.currentBranch()
                          git.isClean()
                          git.fetch()
                          git.hasLocalBranch(name = "task/13-final")
                          git.hasRemoteBranch(name = "task/13-final")
                          git.switchTo(name = "task/13-final")
                          git.merge(ref = "origin/long/13-x")
                          publisher.publish(draft = makeFinalDraft(), console = console)
                          issues.nodeId(number = 13)
                          board.item(issue = 13, nodeId = "I_issue13")
                          board.move(item = makeInProgressItem(), status = "In review")
                          console.line(text = "#13 → In review")
                      }
                  }
              }
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(makeFinalMergeMessage()) {
              every { git.hasLocalBranch(name = "task/13-final") } returns true
              every { git.merge(ref = "origin/long/13-x") } returns false
              command.run(args = listOf("13") + makeDraftArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>("у задачи #13 нет long-ветки — итоговый PR не из чего собрать") {
              every { git.currentBranch() } returns "task/13-final"
              every { git.longBranch(issue = 13) } returns null
              command.run(args = listOf("13") + makeDraftArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(makeFinalDirtyMessage()) {
              every { git.isClean() } returns false
              command.run(args = listOf("13") + makeDraftArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(makeFinalWrongBranchMessage()) {
              every { git.currentBranch() } returns "task/13-x"
              command.run(args = listOf("13") + makeDraftArgs(), console = console)
          }

          shouldThrowWithMessageTest<TaskError.Failure>(makeFinalUsageMessage()) {
              command.run(args = makeDraftArgs(), console = console)
          }
      })
  ```
  `TaskLauncherSpecData.kt:21` — заменить строку `task final` на:
  ```kotlin
            "  task final <N> --title «…» --body-file <файл> [--check «команда»]… — итоговый PR " +
            "task/<N>-final → dev из long/<N>-…\n" +
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*.FinalCommandSpec' --tests '*.TaskLauncherSpec'` — падает: `FinalCommandSpec` «первый итоговый PR из long-ветки» — `publish` с `head = long/13-x` вместо `task/13-final` (воспроизведение бага), `TaskLauncherSpec` — другая строка справки
- [ ] 3. Минимальный код — `FinalCommand.kt` целиком:
  ```kotlin
  package io.github.nakulov.standards.task.command

  import io.github.nakulov.standards.task.board.Board
  import io.github.nakulov.standards.task.cli.Command
  import io.github.nakulov.standards.task.cli.Console
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.BranchName
  import io.github.nakulov.standards.task.git.Git
  import io.github.nakulov.standards.task.github.Issues

  /**
   * `task final <N>` из `long/<N>-…` или `task/<N>-final`: итоговый PR `task/<N>-final` → `dev` с `Closes #N`, сразу к
   * ревью (части уже проверены), In review. Final — от `origin/long/<N>-…`, повтор вливает в неё свежую long; в
   * long-ветку ничего не пушится — её защищает ruleset `standards/long` (#66).
   */
  class FinalCommand(
      private val git: Git,
      private val issues: Issues,
      private val board: Board,
      private val publisher: Publisher,
  ) : Command {
      override val usage: String =
          "<N> --title «…» --body-file <файл> [--check «команда»]… — итоговый PR task/<N>-final → dev из long/<N>-…"

      override fun run(
          args: List<String>,
          console: Console,
      ) {
          val issue = args.firstOrNull()?.toIntOrNull()
              ?: throw TaskError.Failure(message = "использование: task final $usage")
          val final = BranchName(issue = issue, slug = SLUG, long = false).toString()
          val current = git.currentBranch()
          val long = longBranch(issue = issue, current = current, final = final)
          if (!git.isClean()) {
              throw TaskError.Refusal(message = "в рабочей папке незакоммиченные изменения — закоммитьте или уберите их")
          }
          git.fetch()
          prepare(issue = issue, final = final, long = "origin/$long", current = current)
          val draft = PullRequestDraft.of(
              args = args.drop(n = 1),
              head = final,
              base = "dev",
              link = "Closes #$issue",
              draft = false,
              usage = "task final $usage",
          )
          publisher.publish(draft = draft, console = console)
          board.move(item = board.item(issue = issue, nodeId = issues.nodeId(number = issue)), status = Board.IN_REVIEW)
          console.line(text = "#$issue → ${Board.IN_REVIEW}")
      }

      /** Long-ветка задачи: текущая или, из ветки final, найденная по номеру задачи. */
      private fun longBranch(
          issue: Int,
          current: String,
          final: String,
      ): String {
          val branch = BranchName.parse(branch = current)
          if (branch != null && branch.long && branch.issue == issue) {
              return current
          }
          if (current == final) {
              return git.longBranch(issue = issue)
                  ?: throw TaskError.Refusal(message = "у задачи #$issue нет long-ветки — итоговый PR не из чего собрать")
          }
          throw TaskError.Refusal(message = "task final $issue запускается из long/$issue-… или $final, сейчас «$current»")
      }

      /** Ветка final — на свежей long: новая от неё или существующая с влитой long (повтор, #66). */
      private fun prepare(
          issue: Int,
          final: String,
          long: String,
          current: String,
      ) {
          if (!git.hasLocalBranch(name = final) && !git.hasRemoteBranch(name = final)) {
              git.createBranch(name = final, base = long)
              return
          }
          if (current != final) {
              git.switchTo(name = final)
          }
          if (!git.merge(ref = long)) {
              throw TaskError.Refusal(
                  message = "слияние $long в $final не прошло — разрешите конфликт и закоммитьте или проверьте, " +
                      "что $long существует; затем повторите task final $issue",
              )
          }
      }

      companion object {
          /** Slug ветки итогового PR `task/<N>-final`; по нему `task done` узнаёт, что удалить и long (#66). */
          const val SLUG = "final"
      }
  }
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*.FinalCommandSpec' --tests '*.TaskLauncherSpec'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе: «task final открывает итоговый PR из ветки task/<N>-final»

### Задача 3: task done — удаление long после итогового PR
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/command/DoneCommand.kt:45-49, 85-99` — шаги `freeLong` и `removeLong`, копия по slug
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/DoneCommandSpec.kt` — три случая перед закрывающим `})`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/command/DoneCommandSpecData.kt` — строки вывода

- [ ] 1. Падающий тест. В `DoneCommandSpecData.kt`:
  ```kotlin
  fun makeLongRemovedMessage(): String {
      return "long-ветка long/13-big удалена — локально и в origin"
  }

  fun makeNoLongMessage(): String {
      return "long-ветки задачи #13 нет — удалена только ветка итогового PR"
  }

  fun makeLongOpenedMessage(): String {
      return "ветка long/13-big открыта в /repo — переключите там ветку и повторите task done"
  }
  ```
  В `DoneCommandSpec.kt` в общую подготовку (после `justRun { git.removeWorktree(path = any()) }`):
  ```kotlin
          justRun { git.deleteRemoteBranch(name = any()) }
  ```
  И случаи перед закрывающим `})`:
  ```kotlin
          given("итоговый PR из task/13-final слит в копии long-ветки .claude/worktrees/big") {
              every { git.currentBranch() } returns "task/13-final"
              every { issues.pullRequest(branch = "task/13-final") } returns PullRequest(number = 50, merged = true)
              every { git.isWorktree() } returns true
              every { git.longBranch(issue = 13) } returns "long/13-big"
              every { git.openedAt(branch = "long/13-big") } returns null
              every { git.hasLocalBranch(name = "long/13-big") } returns true
              every { git.hasRemoteBranch(name = "long/13-big") } returns true
              every { issues.info(number = 13) } returns makeClosedInfo()
              every { board.item(issue = 13, nodeId = "I_issue13") } returns makeInProgressItem()
              every { git.root() } returns "/repo/.claude/worktrees/big"
              every { git.copyPath(slug = "big") } returns "/repo/.claude/worktrees/big"
              `when` {
                  command.run(args = emptyList(), console = console)
                  then {
                      verifySequence {
                          git.currentBranch()
                          git.isClean()
                          issues.pullRequest(branch = "task/13-final")
                          git.longBranch(issue = 13)
                          git.openedAt(branch = "long/13-big")
                          git.fetch()
                          git.isWorktree()
                          git.detach(ref = "origin/dev")
                          console.line(text = "worktree отсоединён на origin/dev, dev не тронут")
                          git.deleteBranch(name = "task/13-final")
                          console.line(text = "ветка task/13-final удалена")
                          git.hasLocalBranch(name = "long/13-big")
                          git.deleteBranch(name = "long/13-big")
                          git.hasRemoteBranch(name = "long/13-big")
                          git.deleteRemoteBranch(name = "long/13-big")
                          console.line(text = makeLongRemovedMessage())
                          issues.info(number = 13)
                          issues.nodeId(number = 13)
                          board.item(issue = 13, nodeId = "I_issue13")
                          board.move(item = makeInProgressItem(), status = "Done")
                          console.line(text = "#13 → Done")
                          git.root()
                          git.copyPath(slug = "big")
                          git.removeWorktree(path = "/repo/.claude/worktrees/big")
                          console.line(text = "копия /repo/.claude/worktrees/big удалена — текущей папки больше нет")
                      }
                  }
              }
          }

          given("итоговый PR из task/13-final слит, long-ветки уже нет") {
              every { git.currentBranch() } returns "task/13-final"
              every { issues.pullRequest(branch = "task/13-final") } returns PullRequest(number = 50, merged = true)
              every { git.isWorktree() } returns false
              every { git.longBranch(issue = 13) } returns null
              every { issues.info(number = 13) } returns makeClosedInfo()
              every { board.item(issue = 13, nodeId = "I_issue13") } returns makeDoneItem()
              `when` {
                  command.run(args = emptyList(), console = console)
                  then {
                      verifySequence {
                          git.currentBranch()
                          git.isClean()
                          issues.pullRequest(branch = "task/13-final")
                          git.longBranch(issue = 13)
                          git.fetch()
                          git.isWorktree()
                          git.updateDev()
                          console.line(text = "dev обновлён до origin/dev")
                          git.deleteBranch(name = "task/13-final")
                          console.line(text = "ветка task/13-final удалена")
                          console.line(text = makeNoLongMessage())
                          issues.info(number = 13)
                          issues.nodeId(number = 13)
                          board.item(issue = 13, nodeId = "I_issue13")
                          console.line(text = "#13 → Done")
                      }
                  }
              }
          }

          shouldThrowWithMessageTest<TaskError.Refusal>(makeLongOpenedMessage()) {
              every { git.currentBranch() } returns "task/13-final"
              every { issues.pullRequest(branch = "task/13-final") } returns PullRequest(number = 50, merged = true)
              every { git.longBranch(issue = 13) } returns "long/13-big"
              every { git.openedAt(branch = "long/13-big") } returns "/repo"
              command.run(args = emptyList(), console = console)
          }
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*.DoneCommandSpec'` — падает: два новых `given` — в последовательности нет `git.longBranch(issue = 13)` (после `pullRequest` сразу `fetch`); отказ — исключение не брошено
- [ ] 3. Минимальный код — в `DoneCommand.kt`:
  в `run` вместо строк `:45-49`:
  ```kotlin
          val final = !branch.long && branch.slug == FinalCommand.SLUG
          val long = if (final) freeLong(issue = branch.issue) else null
          val worktree = leaveBranch(name = name, console = console)
          if (final) {
              removeLong(issue = branch.issue, name = long, console = console)
          }
          finishIssue(issue = branch.issue, console = console)
          if (worktree) {
              val slug = long?.let { found -> BranchName.parse(branch = found)?.slug } ?: branch.slug
              removeOwnCopy(slug = slug, console = console)
          }
  ```
  новые методы после `leaveBranch`:
  ```kotlin
      /**
       * Long-ветка задачи для удаления после итогового PR из `task/<N>-final` (#66); нет — `null`. Открыта в другой
       * папке — отказ до удаления final: иначе `task done` уже не повторить.
       */
      private fun freeLong(issue: Int): String? {
          val name = git.longBranch(issue = issue) ?: return null
          val folder = git.openedAt(branch = name)
          if (folder != null) {
              throw TaskError.Refusal(message = "ветка $name открыта в $folder — переключите там ветку и повторите task done")
          }
          return name
      }

      /** Long-ветка [name] после итогового PR (#66) — удаляется локально и в `origin`, где есть. */
      private fun removeLong(
          issue: Int,
          name: String?,
          console: Console,
      ) {
          if (name == null) {
              console.line(text = "long-ветки задачи #$issue нет — удалена только ветка итогового PR")
              return
          }
          if (git.hasLocalBranch(name = name)) {
              git.deleteBranch(name = name)
          }
          if (git.hasRemoteBranch(name = name)) {
              git.deleteRemoteBranch(name = name)
          }
          console.line(text = "long-ветка $name удалена — локально и в origin")
      }
  ```
  `removeOwnCopy` — параметр `branch: BranchName` → `slug: String`; KDoc дополнить: «Копия ветки final — копия её long-ветки (#66)»; строка `:94`:
  ```kotlin
          if (File(root).canonicalPath != File(git.copyPath(slug = slug)).canonicalPath) {
  ```
  KDoc класса `:12-17` — дописать: «Из `task/<N>-final` удаляется и long-ветка задачи — локально и в `origin` (#66).»
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*.DoneCommandSpec'` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — тест и код вместе: «task done после итогового PR удаляет и long-ветку задачи»

### Приёмка
- [ ] Что даёт этот PR — проверено: `FinalCommandSpec` «первый итоговый PR из long-ветки» — `head = task/13-final`, push в long нет
- [ ] Крайние случаи обработаны: повтор из final и из long, final только в `origin`, конфликт слияния, long не найдена, long открыта в другой папке, грязное дерево, `long/7-*` против `long/70-…`, копия-ворктри long
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`; `scripts/task --help` — строка `task final` новая
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам (автоматический — тестом; два ручных — в «После завершения»)
- [ ] Покрытие требований:
  - тест воспроизводит баг → задача 2 («первый итоговый PR из long-ветки»)
  - PR `task/<N>-final` → `dev`, в long ничего не пушится → задача 2
  - повтор: свежая long вливается в final, PR не дублируется → задача 1 (`merge`), задача 2
  - после `task final` остаёмся на final → задача 2 (обратного переключения нет)
  - `task done` удаляет final и long, long — и в `origin` → задача 1 (`deleteRemoteBranch`), задача 3
  - long — по номеру задачи, не через `setBase` → задача 1 (`longBranch`), задачи 2–3
  - long открыта в другой папке — отказ до удаления final → задача 3
  - копия-ворктри long удаляется → задача 3

## Открыто
- `skills/execute/SKILL.md:137` (шаг 8) описывает `task final` как «long-ветка → `dev`» и не говорит, что `task done` — из `task/<N>-final`: отдельная задача #77 (Backlog, эпик #4, заблокирована #66) — в этот PR не влезает, 11-й файл.
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка: на настоящей long-задаче (план из ≥ 2 блоков, все PR блоков слиты, `standards/long` включён) из `long/<N>-…` — `scripts/task final <N> --title «…» --body-file <файл>` → создан `task/<N>-final`, PR `task/<N>-final` → `dev` с `Closes #N` открыт, в PR нет `plans/<slug>.md`, `origin/long/<N>-…` не изменилась.
- После слияния итогового PR — `scripts/task done` из `task/<N>-final` → нет ни `task/<N>-final`, ни `long/<N>-…` локально и в `origin`; задача — Done.
