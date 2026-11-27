# Памятка процесса

**Цель:** памятка `docs/workflow.md` для человека: цикл работы из команд скиллов и тест, который держит её в согласии со `skills/`.
**Архитектура:** памятка — markdown в `docs/` без строки BANK-N; на неё ссылаются `README.md` и `docs/README.md`. Тест: чистая функция сверяет команды `/<имя>` памятки с именами скиллов в обе стороны; вторая функция читает файлы репозитория и вызывает первую.
**Стек:** Markdown; Kotlin, Kotest `BehaviorSpec`, KotestKit `shouldReturnTest` — модуль `scripts`.

---

## Ограничения
- Не трогать: `docs/commands/` — на памятку не ссылается (#34).
- У памятки нет строки BANK-N в оглавлении `docs/README.md`: Claude её не читает (груминг 1/5).
- Коммиты и размер PR в памятку не входят: применяет Claude или механика (груминг 4/5).
- Примеры — абстрактные, без названий продуктов: репозиторий публичный.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: `docs/workflow.md` — вступление, «Цикл работы» из четырёх шагов, «Как начать», «Где подробнее»; ссылки на неё из `README.md` и `docs/README.md`; тест `FindWorkflowErrorsSpec`.
- Какую проблему решает, что даёт: памятка не пишется в каждом проекте заново и не пересказывает скиллы; человек видит, что сказать, что получится и что делает он сам.
- Как встраивается: README стандартов («Состав», «Подключение») → памятка → `docs/commands/README.md` и README проекта.
- Критерии цели:
  - каждая `/<имя>` памятки — существующий скилл `skills/<имя>/SKILL.md`, и каждый скилл из `skills/` упомянут в памятке — тест `FindWorkflowErrorsSpec` зелёный;
  - у каждого шага цикла одна строка: команда, что получится, «Ты: …»;
  - `README.md` и `docs/README.md` ссылаются на памятку, `docs/README.md` — без строки BANK-N; ссылки целые — `BankIndexSpec` зелёный;
  - памятка — один экран (проверка глазами в PR).

## Контекст
- Задействованные файлы:
  - `docs/README.md:3` — вступление оглавления, написано под агентов; `:15` — «строки нет — банка нет»
  - `README.md:21` — «Состав», упоминает закрытый оркестратор (#50); `README.md:39–60` — «Подключение»
  - `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindBankIndexErrors.kt:12` — образец: функция возвращает список ошибок, регулярка в приватной константе
  - `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindBankSections.kt:8` — образец чтения файлов репозитория через `task.repoRoot`, комментарий «Вне стандарта»
  - `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/BankIndexSpec.kt:9` — образец спеки: `shouldReturnTest(emptyList<String>(), …)`
  - `skills/{groom,decompose,execute,worktree}/SKILL.md` — четыре скилла
  - `docs/commands/README.md:14`, `docs/commands/pr.md:14` — `task done`, `task pr --ready`
- Паттерны: проверка файлов репозитория — функция в `src/test` возвращает список строк-ошибок, спека ждёт пустой.
- Зависимости: `task.repoRoot` — `scripts/build.gradle.kts:43`.

## Структура файлов
- Создать: `docs/workflow.md` — памятка процесса для человека
- Изменить: `docs/README.md:3` — вступление для разработчика и агента; после `:15` — строка про `workflow.md` без BANK-N
- Изменить: `README.md:21` — «Состав»: цикл работы вместо оркестратора; после `:60` — ссылка на памятку в конце «Подключения»
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrors.kt` — сверка памятки со скиллами; вход: `findWorkflowErrors(workflow: String, skills: List<String>): List<String>`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindRepoWorkflowErrors.kt` — ошибки памятки репозитория; вход: `findRepoWorkflowErrors(): List<String>`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrorsSpec.kt` — случаи сверки и памятка репозитория
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrorsSpecData.kt` — подставные памятки и ожидаемые ошибки

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: из груминга — `docs/workflow.md` без строки в оглавлении банка; «Цикл работы» через команды скиллов; тест в обе стороны.
- Подходы: из груминга — `docs/workflow.md` без строки BANK-N.
- Решения:
  - команда — `/<имя>` в начале кода в обратных кавычках (груминг, «Дизайн 2»): `` `task done` `` и пути не считаются;
  - сверка — чистая функция на тексте и списке имён: случаи проверяются на подставных данных без файлов (KT-TEST-6);
  - чтение репозитория — отдельная функция по образцу `FindBankSections.kt`, с комментарием «Вне стандарта»;
  - спека названа по файлу сверки (KT-TEST-1); тест памятки репозитория в ней — с комментарием «Вне стандарта», как `BankIndexSpec.kt:7`: отдельная спека по файлу `FindRepoWorkflowErrors.kt` дала бы восьмой файл ради одного теста;
  - `worktree` — пометкой к шагу 3, без исключений в тесте (груминг 2/5).
- Как вписывается в систему: рядом с `BankIndexSpec`; ссылку `docs/README.md` → `workflow.md` проверяет `BankIndexSpec` (битая ссылка).
- Обратная совместимость: не нужна — новый документ и новый тест.
- Запасной: нет — подход из груминга, альтернатив в плане нет.
- Отклонено: `WORKFLOW.md` в корне — меняет согласованную структуру #3; таблица «что сказать → что получится → что делаешь ты» и словарь фраз — оркестратор закрыт (#50), шаги — команды скиллов (груминг 2/5); проверка «один экран» тестом — лимит строк был бы с потолка (груминг, «Дизайн 2»).

## Технические детали
- Структуры данных и изменения: нет новых — списки строк.
- Параметры и форматы:
  - команда — регулярка `` `/([a-z][a-z-]*) `` — группа 1 — имя;
  - скилл — папка `skills/<имя>/` с файлом `SKILL.md`;
  - ошибки: «`` `/<имя>` в docs/workflow.md — нет skills/<имя>/SKILL.md ``» и «скилл <имя> не упомянут в docs/workflow.md»; сначала команды без скилла в порядке появления, затем скиллы без команды в порядке списка.
- Поток обработки: `findRepoWorkflowErrors()` — читает `docs/workflow.md` и имена скиллов → `findWorkflowErrors(workflow, skills)` → список ошибок.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: памятка процесса, сверенная со скиллами
  - [x] Задача 1: сверка памятки со скиллами
  - [x] Задача 2: памятка `docs/workflow.md` и проверка репозитория
  - [x] Задача 3: ссылки на памятку из README
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- открыть `docs/workflow.md` на GitHub: один экран, ссылки «Подключение» и `docs/commands/README.md` открываются.

## PR 1: памятка процесса, сверенная со скиллами
**Объём:** `docs/workflow.md`, `docs/README.md`, `README.md`, `FindWorkflowErrors.kt`, `FindRepoWorkflowErrors.kt`, `FindWorkflowErrorsSpec.kt`, `FindWorkflowErrorsSpecData.kt`; ≈ 7 файлов

### Задача 1: сверка памятки со скиллами — команда без скилла и скилл без команды находятся
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrors.kt` — сверка текста памятки с именами скиллов
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrorsSpec.kt` — случаи сверки
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrorsSpecData.kt` — подставные памятки и ожидаемые ошибки

- [x] 1. Падающий тест: все скиллы упомянуты; команда без скилла; скилл без команды; повтор команды — одна ошибка; `task …` и пути — не команды.
  `FindWorkflowErrorsSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  const val WORKFLOW_WITH_ALL_SKILLS = "1. `/groom <N>` — груминг. 2. `/decompose <N>` — план."
  const val WORKFLOW_WITH_UNKNOWN_COMMAND = "1. `/groom <N>`. 2. `/decompose <N>`. 3. `/plan <N>`."
  const val WORKFLOW_WITHOUT_DECOMPOSE = "1. `/groom <N>` — груминг."
  const val WORKFLOW_WITH_REPEATED_UNKNOWN_COMMAND = "`/groom <N>`, `/decompose <N>`, `/plan <N>` и снова `/plan <тема>`."
  const val WORKFLOW_WITH_TASK_COMMAND = "`/groom <N>`, `/decompose <N>`, затем `task -C .worktrees/<slug> done`."
  const val UNKNOWN_COMMAND_ERROR = "`/plan` в docs/workflow.md — нет skills/plan/SKILL.md"
  const val MISSING_DECOMPOSE_ERROR = "скилл decompose не упомянут в docs/workflow.md"

  fun makeSkills(): List<String> {
      return listOf("decompose", "groom")
  }

  fun makeUnknownCommandErrors(): List<String> {
      return listOf(UNKNOWN_COMMAND_ERROR)
  }

  fun makeMissingDecomposeErrors(): List<String> {
      return listOf(MISSING_DECOMPOSE_ERROR)
  }
  ```
  `FindWorkflowErrorsSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.kotestkit.TestOptions
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.kotest.core.spec.style.BehaviorSpec

  class FindWorkflowErrorsSpec : BehaviorSpec({
      shouldReturnTest(emptyList<String>(), options = TestOptions(name = "все скиллы упомянуты")) {
          findWorkflowErrors(workflow = WORKFLOW_WITH_ALL_SKILLS, skills = makeSkills())
      }

      shouldReturnTest(makeUnknownCommandErrors(), options = TestOptions(name = "команда без скилла")) {
          findWorkflowErrors(workflow = WORKFLOW_WITH_UNKNOWN_COMMAND, skills = makeSkills())
      }

      shouldReturnTest(makeMissingDecomposeErrors(), options = TestOptions(name = "скилл без команды")) {
          findWorkflowErrors(workflow = WORKFLOW_WITHOUT_DECOMPOSE, skills = makeSkills())
      }

      shouldReturnTest(makeUnknownCommandErrors(), options = TestOptions(name = "повтор команды — одна ошибка")) {
          findWorkflowErrors(workflow = WORKFLOW_WITH_REPEATED_UNKNOWN_COMMAND, skills = makeSkills())
      }

      shouldReturnTest(emptyList<String>(), options = TestOptions(name = "команда task — не скилл")) {
          findWorkflowErrors(workflow = WORKFLOW_WITH_TASK_COMMAND, skills = makeSkills())
      }
  })
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*FindWorkflowErrorsSpec*'` — падает: `Unresolved reference 'findWorkflowErrors'`
- [x] 3. Минимальный код: `FindWorkflowErrors.kt`
  ```kotlin
  package io.github.nakulov.standards.plugin

  // Команда скилла — `/<имя>` в начале кода в обратных кавычках: группа 1 — имя.
  private const val COMMAND_PATTERN = "`/([a-z][a-z-]*)"

  // Ошибки памятки `docs/workflow.md` против имён скиллов: команда без скилла и скилл, не упомянутый командой.
  // Порядок: команды — по первому появлению в тексте, скиллы — по списку.
  fun findWorkflowErrors(
      workflow: String,
      skills: List<String>,
  ): List<String> {
      val commands = Regex(pattern = COMMAND_PATTERN).findAll(input = workflow)
          .map { match -> match.groupValues[1] }
          .distinct()
          .toList()
      val unknownCommands = commands.filterNot { command -> command in skills }
          .map { command -> "`/$command` в docs/workflow.md — нет skills/$command/SKILL.md" }
      val missingSkills = skills.filterNot { skill -> skill in commands }
          .map { skill -> "скилл $skill не упомянут в docs/workflow.md" }
      return unknownCommands + missingSkills
  }
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*FindWorkflowErrorsSpec*'` — `BUILD SUCCESSFUL`, 5 тестов
- [x] 5. Коммит — тест и код вместе: «Сверка памятки процесса со скиллами»

### Задача 2: памятка `docs/workflow.md` и проверка репозитория — памятка согласована со `skills/`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindRepoWorkflowErrors.kt` — ошибки памятки репозитория
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindWorkflowErrorsSpec.kt` — тест памятки репозитория
- Создать: `docs/workflow.md` — памятка процесса

- [x] 1. Падающий тест: в конец `FindWorkflowErrorsSpec` (перед `})`) добавить
  ```kotlin

      // Вне стандарта: тест проверяет не функцию, а файлы репозитория — памятку `docs/workflow.md` и папку `skills/`:
      // переименованный или новый скилл без строки в памятке иначе молча расходится с ней.
      shouldReturnTest(emptyList<String>(), options = TestOptions(name = "памятка репозитория без ошибок")) {
          findRepoWorkflowErrors()
      }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*FindWorkflowErrorsSpec*'` — падает: `Unresolved reference 'findRepoWorkflowErrors'`
- [x] 3. Минимальный код: `FindRepoWorkflowErrors.kt`
  ```kotlin
  package io.github.nakulov.standards.plugin

  import java.io.File

  // Вне стандарта: функция читает файлы репозитория прямо в тесте, без интерфейса —
  // проверяются файлы, а не код.
  // Ошибки памятки `docs/workflow.md` репозитория; скиллы — папки `skills/` со своим `SKILL.md`, по алфавиту.
  fun findRepoWorkflowErrors(): List<String> {
      val root = File(System.getProperty(/* key = */ "task.repoRoot"))
      val skills = File(root, /* child = */ "skills").listFiles()
          .orEmpty()
          .filter { dir -> File(dir, /* child = */ "SKILL.md").isFile }
          .map { dir -> dir.name }
          .sorted()
      val workflow = File(root, /* child = */ "docs/workflow.md").readText()
      return findWorkflowErrors(workflow, skills)
  }
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*FindWorkflowErrorsSpec*'` — падает: `FileNotFoundException: …/docs/workflow.md` (памятки ещё нет)
- [x] 5. Памятка `docs/workflow.md`:
  ```markdown
  # Как мы работаем

  Задача → обсуждение → план → блоки PR → слияние. Каждый шаг — новая сессия Claude Code в папке проекта; команду следующего шага печатает предыдущий.

  ## Цикл работы

  1. `/groom <N>` — груминг: проектные решения в задаче #N, задача → Groomed. Ты: выбираешь варианты; в конце — «план» или «сразу в работу» (сразу в работу — Claude делает всё в той же сессии, дальше шаг 4).
  2. `/decompose <N>` — план по блокам PR в ветке задачи. Ты: отвечаешь на вопросы, решаешь про ревью плана агентом, утверждаешь план.
  3. `/execute <N> <slug>` — блок плана: код, тесты, черновик PR. Копия для параллельной работы — `/worktree` или «Ворктри» в `execute`. Ты: делаешь ручные шаги ✋, смотришь PR, переводишь к ревью — `task pr --ready`, сливаешь (squash: заголовок и описание PR станут коммитом, сообщение не правь). Блоков несколько — `/execute` на каждый, после последнего ещё раз — итоговый PR.
  4. `task done` — после слияния: свежий `dev`, ветка удалена, задача → Done. Ты: запускаешь в терминале.

  ## Как начать

  1. Подключи проект — README стандартов, [«Подключение»](../README.md#подключение).
  2. Заведи задачу — `task new` — или начни с темы: `/groom <тема>`.
  3. Дальше — цикл с шага 1.

  ## Где подробнее

  - Команды `task` — [docs/commands/README.md](commands/README.md).
  - Как проверить результат — README проекта.
  ```
- [x] 6. Запустить `./gradlew :scripts:test --tests '*FindWorkflowErrorsSpec*'` — `BUILD SUCCESSFUL`, 6 тестов
- [x] 7. Коммит — тест, код и памятка вместе: «Памятка процесса для человека»

### Задача 3: ссылки на памятку из README — человек находит памятку
- Изменить: `docs/README.md:3` — вступление; после `:15` — строка про памятку
- Изменить: `README.md:21` — «Состав»; после `:60` — конец «Подключения»

Тест: не применим — текст документов; ссылку из `docs/README.md` проверяет `BankIndexSpec` (битая ссылка).

- [x] 1. `docs/README.md:3` — было:
  ```markdown
  Стандарты наших проектов: стиль кода, тесты, команды процесса. Это оглавление — единственный вход в банк для скиллов и агентов: найди строку своей работы и прочитай целиком все её файлы; другие строки не читай.
  ```
  стало:
  ```markdown
  Стандарты наших проектов: стиль кода, тесты, команды процесса. Это оглавление для разработчика и агента: найди строку своей работы и прочитай целиком все её файлы; другие строки не читай. Агент входит в банк только через оглавление.
  ```
- [x] 2. `docs/README.md` — после строки `:15` («Строки для твоей работы нет — …») добавить пустую строку и:
  ```markdown
  Процесс для человека — [workflow.md](workflow.md): цикл работы от задачи до слияния. Строки BANK-N у памятки нет — Claude её не читает.
  ```
- [x] 3. `README.md:21` — было:
  ```markdown
  - **Процесс:** workflow и скиллы, в том числе оркестратор, который ведёт сессию от груминга до выполнения задачи.
  ```
  стало:
  ```markdown
  - **Процесс:** цикл работы ([docs/workflow.md](docs/workflow.md)) и скиллы этапов.
  ```
- [x] 4. `README.md` — после строки `:60` («папки копии.») добавить пустую строку и:
  ```markdown
  Как работать дальше — [docs/workflow.md](docs/workflow.md).
  ```
- [x] 5. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — `BUILD SUCCESSFUL`
- [x] 6. Коммит: «Ссылки на памятку процесса из README»

### Приёмка
- [ ] Что даёт этот PR — проверено: `docs/workflow.md` открывается из `README.md` и `docs/README.md`, все `/<имя>` в ней — скиллы
- [ ] Крайние случаи обработаны: повтор команды, `task …` и пути в коде — покрыты случаями задачи 1
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` — `BUILD SUCCESSFUL`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам; в описании PR — строка «памятка — один экран, проверить глазами»

## Открыто
- нет
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка: открыть `docs/workflow.md` на GitHub — помещается на один экран; ссылка «Подключение» ведёт к разделу README; ссылка на `docs/commands/README.md` открывается.
