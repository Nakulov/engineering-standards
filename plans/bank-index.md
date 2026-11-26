# Скиллы и агенты сами находят нужное в банке знаний

**Цель:** скиллы и агенты, которые пишут или проверяют код, читают нужные разделы банка знаний `docs/`, а что и когда читать, знает только банк.
**Архитектура:** три слоя. Первый — `rules/bank.md` и хук `SessionStart`: банк есть, и вот где (корень плагина). Второй — оглавление `docs/README.md`: «делаешь → читай». Третий — сами разделы банка. Сабагентам `plan-executor` и `standards-reviewer` правило достаётся копией в маркерах `rules:bank`, а корень плагина передаёт `execute`.
**Стек:** Markdown (правила, скиллы, агенты, банк), bash-хук, Kotlin + Kotest/KotestKit (тесты `scripts/`).

---

## Ограничения
- Не трогать: документы проекта — `.claude/project.md`, карту, `.claude/rules/` проекта. Они вынесены из #49, в том числе строка «карта проекта „путь → проверить“» в `agents/plan-executor.md` («Валидация», п. 2) — это про проверки, а не про банк.
- Копии `rules:bank` в агентах совпадают с `rules/bank.md` слово в слово — это проверяет `MarkersSpec`.
- Без «(#N)» в текстах правил, скиллов и агентов. Примеры абстрактные: репозиторий публичный.
- Kotlin — без переносов `ktlint_official`, строка не длиннее 120 символов (`.editorconfig:11`).
- Хук при любой ошибке выходит с кодом 0: сессию не блокирует.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- **Что реализуется:**
  - оглавление банка `docs/README.md`;
  - правило-вход `rules/bank.md` и строка «Корень плагина» из хука;
  - копия правила в двух агентах и передача корня плагина из `execute`;
  - тест оглавления.
- **Какую проблему решает:** стандарт Kotlin (`docs/code-style/kotlin/`, `docs/testing/kotlin/`) есть, но ни один скилл и агент его не читает, а `agents/standards-reviewer.md:46` ищет несуществующую «карту проекта».
- **Как встраивается:**
  - хук уже печатает `rules/*.md` в контекст сессии (`hooks/session-start:28`), и новое правило подхватывается само;
  - агенты получают правило копией, как `rules:bash-commands`.
- **Критерии цели:**
  1. В скиллах, агентах и правилах нет строк «вид файла → раздел банка». Проверка: `grep -rnE 'code-style/kotlin|testing/kotlin|\*\.kts?' skills agents rules` — пусто.
  2. Растёт банк, а не скиллы. Новый раздел `docs/<область>/<язык>/` с `README.md` требует только строки в `docs/README.md`, иначе `BankIndexSpec` падает.
  3. Вход в банк всегда в контексте:
     - основной сессии — правило `rules/bank.md` и «Корень плагина» от хука;
     - `plan-executor` и `standards-reviewer` — копия `rules:bank` и аргумент «Корень плагина» от `execute`.
  4. Расход токенов не растёт в разы. Правило — несколько строк на сессию; читаются только строки своей работы; при переносе готового кода плана — только оглавление, без разделов стиля и тестов.

## Контекст
- Задействованные файлы:
  - `hooks/session-start` — PATH, запись корня для обёртки, печать `rules/*.md`. Корень печатается только без `task` в PATH (`:16`).
  - `rules/bash-commands.md` — образец правила с маркерами `rules:<имя>`.
  - `agents/plan-executor.md`, `agents/standards-reviewer.md` — копии `rules:bash-commands`, раздел «Вход».
  - `agents/standards-reviewer.md:46` — «по карте проекта … Пример: `*.kt` → стиль Kotlin».
  - `skills/execute/SKILL.md:62` — аргументы `plan-executor`.
  - `skills/execute/review.md:12` — аргументы ревьюеров. Файл читается через Read: `${CLAUDE_PLUGIN_ROOT}` в нём не подставляется.
  - `skills/decompose/SKILL.md:316` — пункт «Документация» в шаблоне плана, вне маркеров `plan:*`.
  - `docs/code-style/kotlin/README.md`, `docs/testing/kotlin/README.md`, `docs/commands/README.md` — разделы банка с оглавлениями файлов.
  - `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpec.kt`, `MarkersSpecData.kt` — проверка копий маркеров.
  - `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/SessionStartHookSpec.kt`, `SessionStartHookSpecData.kt` — тесты хука.
- Паттерны:
  - спека «вне стандарта» проверяет файлы репозитория: путь от `System.getProperty("task.repoRoot")` (`scripts/build.gradle.kts:43`), разбор — в `…SpecData.kt` (`TemplatesSpec`, `MarkersSpec`);
  - правило плагина с копиями в агентах: `rules/bash-commands.md` → 4 агента.
- Зависимости: PR 2 опирается на `rules/bank.md` из PR 1.

## Структура файлов
PR 1:
- Создать: `docs/README.md` — оглавление банка «делаешь → читай».
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/BankIndexSpec.kt` — каждая ссылка оглавления ведёт на файл, каждый раздел банка есть в оглавлении.
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/BankIndexSpecData.kt` — разбор ссылок оглавления и поиск разделов; вход: `findBrokenIndexLinks(): List<String>`, `findSectionsMissingFromIndex(): List<String>`, `findBankSections(): List<String>`.
- Создать: `rules/bank.md` — вход в банк: где оглавление, что делать без него.
- Изменить: `hooks/session-start:28` — строка «Корень плагина» печатается всегда.
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/SessionStartHookSpec.kt`, `SessionStartHookSpecData.kt` — ожидаемый вывод со строкой корня; вход: `makeRootLine(plugin: File): String`.
- Изменить: `skills/decompose/SKILL.md:316` — убрать «файл проекта и карта».

PR 2:
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpecData.kt:5-15`, `:38` — у каждого правила плагина свой источник и свои агенты; блок сравнивается по имени.
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpec.kt` — `rules:bank` среди источников.
- Изменить: `agents/plan-executor.md` — копия `rules:bank`, вход «Корень плагина».
- Изменить: `agents/standards-reviewer.md` — копия `rules:bank`, вход «Корень плагина», `:46` — по оглавлению банка.
- Изменить: `skills/execute/SKILL.md:62` — аргумент «корень плагина» для `plan-executor`.
- Изменить: `skills/execute/review.md:12` — аргумент «корень плагина» для `standards-reviewer`.

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- **Подход:** из груминга — три слоя «правило с хуком → оглавление → разделы». Знание «что и когда» живёт только в `docs/README.md`, по виду работы, а не по имени скилла.
- **Решения:**
  - Ссылки оглавления ведут на `README.md` раздела, не на папку. Агент читает файл, а тест сверяет ссылку с разделом однозначно.
  - Раздел банка — папка `docs/` со своим `README.md`. Сейчас это `code-style/kotlin`, `commands`, `testing/kotlin`. Python (`docs/code-style/python/README.md`) станет разделом сам, без правки теста.
  - Путь к корню — отдельной строкой («Корень плагина» от хука, аргумент от `execute`), а не подстановкой в текст правила. Так копии в агентах совпадают с источником слово в слово.
  - `review.md` ссылается на корень из шага 4.4 `execute`. В нём `${CLAUDE_PLUGIN_ROOT}` не подставляется: файл читается через Read.
  - `MarkersSpecData`: источник правил теперь у каждого правила свой. Блоки сравниваются по имени `rules:<правило>`. Причина: с одним префиксом `rules` правило `rules/bank.md` дало бы ложное расхождение `rules:bash-commands`, которое есть в агенте, но не в этом источнике. Предположение груминга «новый маркер подхватится сам» не подтвердилось (`MarkersSpecData.kt:12`).
  - Два блока PR вместо одного — изменено при плане: на груминге тесты не считали, всего ≈ 14 файлов при лимите 10.
- **Как вписывается:** основная сессия получает правило от хука. `plan-executor` и `standards-reviewer` получают копию и корень от `execute`. `implementation-reviewer`, `test-reviewer` и `plan-reviewer` банк не читают, копии у них нет.
- **Обратная совместимость:** не нужна. Новых входов в команды нет, у хука прибавляется строка вывода.
- **Запасной:** скилл-вход `standards` — переходим, если строка правила от хука не держит вход на деле: на ручной проверке `decompose` по задаче с Kotlin не открывает `docs/README.md`.
- **Отклонено:**
  - пути в каждом скилле и агенте — новый язык потребовал бы трёх копий;
  - скилл-вход `standards` — вызов по описанию не гарантирован, пропуск незаметен;
  - хук на правку `*.kt` — срабатывал бы на каждую правку, а в сабагентах не проверен.

## Технические детали
- **Строка хука:** `Корень плагина: \`<root>\`` — после записи корня для обёртки, перед правилами.
- **Формат оглавления:** таблица «Делаешь | Читай». Ссылки markdown `[текст](путь от docs/)`. Внешние ссылки (`http…`) тест не проверяет.
- **Поток обработки:** сессия читает правило и строку корня → по правилу открывает `<корень>/docs/README.md` → находит строку своей работы → читает только её разделы.
- **Ошибки:**
  - Нет корня или в оглавлении нет строки работы: «Банк знаний не найден — проверено только по образцам существующего кода», дальше работа по образцам. Путь в кеше плагинов агент сам не ищет.
  - Битая ссылка оглавления: агент сообщает о ней в итоге и продолжает по образцам.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: оглавление банка и вход в него в основной сессии
  - [x] Задача 1: Оглавление банка под тестом ссылок и разделов
  - [x] Задача 2: Правило-вход и строка «Корень плагина» из хука
  - [ ] Задача 3: Шаблон плана без «файла проекта и карты»
  - [ ] Приёмка
- PR 2: сабагенты получают вход в банк
  - [ ] Задача 4: Копии правила банка в агентах под проверкой маркеров
  - [ ] Задача 5: Корень плагина — вход агентов от `execute`
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Ручная проверка на живой сессии — сценарии в «После завершения».

## PR 1: оглавление банка и вход в него в основной сессии
**Объём:** `docs/README.md`, `BankIndexSpec.kt`, `BankIndexSpecData.kt`, `rules/bank.md`, `hooks/session-start`, `SessionStartHookSpec.kt`, `SessionStartHookSpecData.kt`, `skills/decompose/SKILL.md` — ≈ 8 файлов.

### Задача 1: Оглавление банка под тестом ссылок и разделов
- Создать: `docs/README.md` — оглавление «делаешь → читай».
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/BankIndexSpec.kt` — ссылки и разделы.
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/BankIndexSpecData.kt` — разбор оглавления и поиск разделов.

- [x] 1. Падающий тест: ссылки оглавления ведут на файлы, каждый раздел есть в оглавлении, разделы найдены.
  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.kotestkit.TestOptions
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.kotest.core.spec.style.BehaviorSpec

  // Вне стандарта: спека проверяет не класс, а оглавление банка `docs/README.md` — единственный вход скиллов и агентов
  // в банк: битая ссылка или раздел без строки тихо отключают раздел. Разбор — в BankIndexSpecData.
  class BankIndexSpec :
      BehaviorSpec({
          shouldReturnTest(emptyList<String>(), options = TestOptions(name = "каждая ссылка оглавления ведёт на файл")) {
              findBrokenIndexLinks()
          }

          shouldReturnTest(emptyList<String>(), options = TestOptions(name = "каждый раздел банка — в оглавлении")) {
              findSectionsMissingFromIndex()
          }

          shouldReturnTest(true, options = TestOptions(name = "разделы банка найдены")) {
              findBankSections().isNotEmpty()
          }
      })
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — падает: `Unresolved reference: findBrokenIndexLinks`.
- [x] 3. Минимальный код: разбор оглавления и само оглавление.
  `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/BankIndexSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  import java.io.File

  // Ссылка markdown `](путь)` или `](путь#якорь)`: группа 1 — путь без якоря.
  private val LINK = Regex(pattern = "\\]\\(([^)#\\s]+)[^)]*\\)")

  private fun docs(): File = File(System.getProperty("task.repoRoot"), "docs")

  /** Ссылки оглавления `docs/README.md` — пути от `docs/`, без якорей; внешние (`http…`) не берём. */
  private fun indexLinks(): List<String> =
      LINK.findAll(input = File(docs(), "README.md").readText())
          .map { match -> match.groupValues[1] }
          .filterNot { link -> link.startsWith(prefix = "http") }
          .toList()

  /** Ссылки оглавления на несуществующие файлы. */
  fun findBrokenIndexLinks(): List<String> = indexLinks().filterNot { link -> File(docs(), link).exists() }

  /** Разделы банка — папки `docs/` со своим `README.md`, кроме самого `docs/`; пути от `docs/`, по алфавиту. */
  fun findBankSections(): List<String> =
      docs().walkTopDown()
          .filter { dir -> dir != docs() && File(dir, "README.md").isFile }
          .map { dir -> dir.relativeTo(docs()).path }
          .sorted()
          .toList()

  /** Разделы банка, на чей `README.md` оглавление не ссылается. */
  fun findSectionsMissingFromIndex(): List<String> {
      val links = indexLinks().toSet()
      return findBankSections().filterNot { section -> "$section/README.md" in links }
  }
  ```
  `docs/README.md`:
  ```markdown
  # Банк знаний

  Стандарты наших проектов: стиль кода, тесты, команды процесса. Это оглавление — единственный вход в банк для скиллов и агентов: найди строку своей работы и читай только её разделы, не весь банк.

  ## Что читать

  | Делаешь | Читай |
  |---------|-------|
  | пишешь или правишь код Kotlin (`*.kt`, `*.kts`) — в плане или в файлах | [code-style/kotlin/README.md](code-style/kotlin/README.md), затем файлы из его таблицы |
  | пишешь или правишь тест Kotlin (`src/test/`) | строку выше и ещё [testing/kotlin/README.md](testing/kotlin/README.md) |
  | переносишь в файлы готовый код из плана | ничего: код плана уже по стандарту. Строки выше — только когда пишешь код не из плана: чинишь упавший тест или сборку |
  | проверяешь дифф | строки выше — по видам изменённых файлов |
  | запускаешь `task <команда>` или `setup` | файл команды — из таблицы [commands/README.md](commands/README.md) |

  Строки для твоей работы нет — банка для неё нет: работай по образцам существующего кода, правил не придумывай.

  ## Новый раздел

  Раздел банка — папка `docs/<область>/<язык>/` со своим `README.md`. Добавил раздел — добавь строку в «Что читать»: без неё раздел никто не прочитает. Раздел без строки ловит тест `BankIndexSpec`.
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — `BUILD SUCCESSFUL`.
- [x] 5. Коммит — тест и код вместе.

### Задача 2: Правило-вход и строка «Корень плагина» из хука
- Создать: `rules/bank.md` — вход в банк.
- Изменить: `hooks/session-start:2`, `:28` — строка корня всегда.
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/SessionStartHookSpec.kt`, `SessionStartHookSpecData.kt` — ожидаемый вывод.

- [x] 1. Падающий тест: вывод хука содержит строку «Корень плагина» во всех запусках, даже без правил и с `task` в PATH.
  `SessionStartHookSpecData.kt` — заменить `makeRulesRun`, `makeNoEnvFileRun`, `makeNoCacheRun` и добавить `makeRootLine`:
  ```kotlin
  /** Строка корня плагина — хук печатает её всегда: по ней сессия находит банк знаний. */
  fun makeRootLine(plugin: File): String {
      return "Корень плагина: `${plugin.path}`\n"
  }

  fun makeRulesRun(plugin: File): LauncherRun {
      return LauncherRun(code = 0, out = makeRootLine(plugin = plugin) + "Правило А\n\nПравило Б\n\n", err = "")
  }

  fun makeNoEnvFileRun(plugin: File): LauncherRun {
      val out = "`task` не в PATH — вызывай ${plugin.path}/scripts/task\n" + makeRootLine(plugin = plugin)
      return LauncherRun(code = 0, out = out, err = "")
  }

  fun makeNoCacheRun(
      cache: File,
      plugin: File,
  ): LauncherRun {
      val err = "engineering-standards: корень плагина не записан в ${cache.path}/engineering-standards/roots\n"
      return LauncherRun(code = 0, out = makeRulesRun(plugin = plugin).out, err = err)
  }
  ```
  `SessionStartHookSpec.kt` — три вызова:
  ```kotlin
          shouldReturnTest(makeRulesRun(plugin = plugin)) {
  ```
  ```kotlin
          shouldReturnTest(makeNoCacheRun(cache = blocked, plugin = plugin)) {
  ```
  ```kotlin
          shouldReturnTest(LauncherRun(code = 0, out = makeRootLine(plugin = bare), err = "")) {
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*SessionStartHookSpec*'` — падают 4 теста: `expected:<…Корень плагина: `…` …> but was:<…>` (в выводе хука строки корня нет).
- [x] 3. Минимальный код: строка в хуке и правило.
  `hooks/session-start`, строки 2–3 (шапка):
  ```bash
  # Хук SessionStart плагина engineering-standards (#43, решение 9): PATH к scripts/ плагина, корень плагина для обёртки
  # ~/.local/bin/task, корень плагина и rules/ в контекст сессии (вход в банк знаний — rules/bank.md).
  # Любая ошибка — код 0 и stderr: хук не блокирует сессию.
  ```
  `hooks/session-start`, перед `for rule in "$root"/rules/*.md; do`:
  ```bash
  # Корень плагина — всегда: по нему правило rules/bank.md находит оглавление банка docs/README.md.
  echo "Корень плагина: \`$root\`"

  ```
  `rules/bank.md`:
  ```markdown
  <!-- >>> rules:bank -->
  Банк знаний — стандарты наших проектов в `docs/` корня плагина. Пишешь код в план или в файлы, правишь или проверяешь код, запускаешь `task` — сначала оглавление `<корень плагина>/docs/README.md`; читай только разделы из строки своей работы, не весь банк.
  Корень плагина — строка «Корень плагина» в начале сессии или вход агента «Корень плагина». Корня нет или в оглавлении нет строки твоей работы — первой строкой итога «Банк знаний не найден — проверено только по образцам существующего кода» и работай по образцам; путь в кеше плагинов сам не ищи — найдёшь не ту версию. Ссылка оглавления битая — сообщи в итоге и продолжай по образцам.
  Причина: что и когда читать, знает банк, а не скилл или агент — новый раздел меняет только банк.
  <!-- <<< rules:bank -->
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*SessionStartHookSpec*'` — `BUILD SUCCESSFUL`.
- [x] 5. Коммит — тест и код вместе.

### Задача 3: Шаблон плана без «файла проекта и карты»
- Изменить: `skills/decompose/SKILL.md:316` — пункт «Документация» в «Приёмке» шаблона.

- [ ] 1. Тест: не применим — текст скилла вне маркеров `plan:*`; проверка — `grep` в шаге 4.
- [ ] 2. Запустить `grep -n "файл проекта и карта" skills/decompose/SKILL.md` — находит `316:`.
- [ ] 3. Заменить строку 316:
  было:
  ```markdown
  - [ ] Документация: <что обновить — README, `docs/`, файл проекта и карта> — пункт только если есть что обновлять
  ```
  стало:
  ```markdown
  - [ ] Документация: <что обновить — README, `docs/`> — пункт только если есть что обновлять
  ```
- [ ] 4. Запустить `grep -n "файл проекта и карта" skills/decompose/SKILL.md` — пусто; `./gradlew :scripts:test --tests '*MarkersSpec*'` — `BUILD SUCCESSFUL` (блоки `plan:*` не задеты).
- [ ] 5. Коммит.

### Приёмка
- [ ] Что даёт этот PR — проверено: `hooks/session-start` из корня репозитория печатает «Корень плагина» и текст `rules/bank.md`; ссылки в `docs/README.md` открываются.
- [ ] Крайние случаи обработаны: хук без правил и с `task` в PATH печатает строку корня (тест), раздел без строки в оглавлении ловит `BankIndexSpec`.
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` — `BUILD SUCCESSFUL`.

## PR 2: сабагенты получают вход в банк
**Объём:** `MarkersSpec.kt`, `MarkersSpecData.kt`, `agents/plan-executor.md`, `agents/standards-reviewer.md`, `skills/execute/SKILL.md`, `skills/execute/review.md` — ≈ 6 файлов.

### Задача 4: Копии правила банка в агентах под проверкой маркеров
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpecData.kt:5-15`, `:38` — правило → агенты, сравнение по имени блока.
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/MarkersSpec.kt:7-35` — шапка и список источников.
- Изменить: `agents/plan-executor.md`, `agents/standards-reviewer.md` — копия `rules:bank` после блока `rules:bash-commands`.

- [ ] 1. Падающий тест: `rules:bank` есть среди источников и скопирован в `plan-executor` и `standards-reviewer`.
  `MarkersSpecData.kt` — заменить `Copies` и `COPIES` (строки 5–15):
  ```kotlin
  /** Источник и копия, у которых блоки [prefix] — копии 1 в 1: [prefix] — имя блока или его часть до `:`. */
  private class Copies(val source: String, val copy: String, val prefix: String)

  /** Правило плагина → агенты с его копией: правила плагина грузятся только основной сессии. */
  private val RULE_COPIES = mapOf(
      "bash-commands" to listOf("plan-executor", "implementation-reviewer", "test-reviewer", "standards-reviewer"),
      "bank" to listOf("plan-executor", "standards-reviewer"),
  )

  private val COPIES = listOf(
      Copies(source = "skills/decompose/SKILL.md", copy = "agents/plan-reviewer.md", prefix = "plan"),
      Copies(source = "skills/execute/SKILL.md", copy = "agents/plan-executor.md", prefix = "execute"),
  ) + RULE_COPIES.flatMap { (rule, agents) ->
      agents.map { agent -> Copies(source = "rules/$rule.md", copy = "agents/$agent.md", prefix = "rules:$rule") }
  } + listOf("implementation-reviewer", "test-reviewer", "standards-reviewer").map { reviewer ->
      Copies(source = "agents/plan-executor.md", copy = "agents/$reviewer.md", prefix = "agents")
  }
  ```
  `MarkersSpecData.kt`, функция `blocks` — заменить фильтр:
  ```kotlin
          .filterKeys { name -> name == prefix || name.startsWith(prefix = "$prefix:") }
  ```
  `MarkersSpec.kt` — строку шапки «// `rules:*` — `rules/bash-commands.md` и 4 агента с Bash (#98): …» заменить двумя:
  ```kotlin
  // `rules:<правило>` — `rules/<правило>.md` и агенты, которым оно нужно (#98, #49): правила плагина грузятся только
  // основной сессии,
  ```
  `MarkersSpec.kt` — список источников второго теста:
  ```kotlin
              listOf(
                  "agents:working-folder",
                  "execute:unit",
                  "plan:coverage",
                  "plan:decomposition",
                  "plan:granularity",
                  "plan:no-stubs",
                  "plan:types",
                  "rules:bank",
                  "rules:bash-commands",
              ),
  ```
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — падает первый тест: `expected:<[]> but was:<[rules:bank, rules:bank]>` (в агентах копий нет); второй — зелёный.
- [ ] 3. Минимальный код: в `agents/plan-executor.md` и `agents/standards-reviewer.md` сразу после строки `<!-- <<< rules:bash-commands -->` — пустая строка и блок, слово в слово как `rules/bank.md`:
  ```markdown

  <!-- >>> rules:bank -->
  Банк знаний — стандарты наших проектов в `docs/` корня плагина. Пишешь код в план или в файлы, правишь или проверяешь код, запускаешь `task` — сначала оглавление `<корень плагина>/docs/README.md`; читай только разделы из строки своей работы, не весь банк.
  Корень плагина — строка «Корень плагина» в начале сессии или вход агента «Корень плагина». Корня нет или в оглавлении нет строки твоей работы — первой строкой итога «Банк знаний не найден — проверено только по образцам существующего кода» и работай по образцам; путь в кеше плагинов сам не ищи — найдёшь не ту версию. Ссылка оглавления битая — сообщи в итоге и продолжай по образцам.
  Причина: что и когда читать, знает банк, а не скилл или агент — новый раздел меняет только банк.
  <!-- <<< rules:bank -->
  ```
- [ ] 4. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — `BUILD SUCCESSFUL`.
- [ ] 5. Коммит — тест и копии вместе.

### Задача 5: Корень плагина — вход агентов от `execute`
- Изменить: `agents/plan-executor.md` — раздел «Вход».
- Изменить: `agents/standards-reviewer.md` — раздел «Вход» и «Как проверяешь», п. 1 (`:46`).
- Изменить: `skills/execute/SKILL.md:62` — аргумент `plan-executor`.
- Изменить: `skills/execute/review.md:12` — аргумент `standards-reviewer`.

- [ ] 1. Тест: не применим — текст агентов и скилла; проверка — `grep` в шаге 4 (критерий цели 1) и `MarkersSpec` (копии не задеты).
- [ ] 2. Запустить `grep -rnE 'code-style/kotlin|testing/kotlin|\*\.kts?' skills agents rules` — находит `agents/standards-reviewer.md:46`.
- [ ] 3. Правки:
  `agents/plan-executor.md`, «Вход» — после строки `- **Рабочая папка** — …`:
  ```markdown
  - **Корень плагина** — абсолютный путь к корню плагина: оглавление банка знаний — `<корень плагина>/docs/README.md` (правило «Банк знаний» выше).
  ```
  `agents/standards-reviewer.md`, «Вход» — после строки `- **Рабочая папка** — …`, та же строка.
  `agents/standards-reviewer.md:46`:
  было:
  ```markdown
  1. Прочитай банк знаний — не весь, а правила для изменённых путей: файлы из диффа → разделы банка по карте проекта «путь → прочитать». Пример: `*.kt` → стиль Kotlin.
  ```
  стало:
  ```markdown
  1. Прочитай банк знаний — не весь: по оглавлению банка (правило «Банк знаний» выше) — разделы по видам изменённых файлов.
  ```
  `skills/execute/SKILL.md:62`:
  было:
  ```markdown
     4. Запусти нового агента `plan-executor` по имени, без истории сессии. Аргументы: абсолютный путь к плану; рабочая папка; блок — заголовок `## PR k` как есть; задача — заголовок секции как есть; номер попытки; журнал; при повторе — итог прошлой попытки (его запись из журнала).
  ```
  стало:
  ```markdown
     4. Запусти нового агента `plan-executor` по имени, без истории сессии. Аргументы: абсолютный путь к плану; рабочая папка; корень плагина — `${CLAUDE_PLUGIN_ROOT}`; блок — заголовок `## PR k` как есть; задача — заголовок секции как есть; номер попытки; журнал; при повторе — итог прошлой попытки (его запись из журнала).
  ```
  `skills/execute/review.md:12`:
  было:
  ```markdown
  Каждому: путь к плану, рабочая папка, база — `git -C <рабочая папка> merge-base origin/dev HEAD` (начало ветки плана; одинаково для одного блока и нескольких); `implementation-reviewer` и `test-reviewer` — ещё журнал.
  ```
  стало:
  ```markdown
  Каждому: путь к плану, рабочая папка, база — `git -C <рабочая папка> merge-base origin/dev HEAD` (начало ветки плана; одинаково для одного блока и нескольких); `implementation-reviewer` и `test-reviewer` — ещё журнал; `standards-reviewer` — ещё корень плагина, тот же, что `plan-executor` (шаг 4.4 скилла).
  ```
- [ ] 4. Запустить `grep -rnE 'code-style/kotlin|testing/kotlin|\*\.kts?' skills agents rules` — пусто; `./gradlew :scripts:test --tests '*MarkersSpec*'` — `BUILD SUCCESSFUL`.
- [ ] 5. Коммит.

### Приёмка
- [ ] Что даёт этот PR — проверено: в `plan-executor` и `standards-reviewer` есть копия `rules:bank` и вход «Корень плагина»; `execute` передаёт корень обоим.
- [ ] Крайние случаи обработаны: корень не передан — правило велит «Банк знаний не найден — проверено только по образцам существующего кода» (`rules/bank.md`); текст совпадает с шаблоном `agents/standards-reviewer.md:86`.
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` — `BUILD SUCCESSFUL`.
- [ ] Критерии цели из «Обзора» — по пунктам: 1 — `grep` из задачи 5 пуст; 2 — `BankIndexSpec` зелёный; 3 — правило в `rules/`, копии в двух агентах, корень от хука и `execute`; 4 — правило ≈ 3 строки, оглавление велит читать только свои строки.

## Открыто
- Нет.
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка на живой сессии после обновления плагина:
  1. Новая сессия: в начале есть «Корень плагина: `<путь>`» и текст правила «Банк знаний».
  2. `/decompose` по задаче с кодом Kotlin: сессия читает `docs/README.md`, затем `code-style/kotlin/` (и `testing/kotlin/` для теста) — не весь `docs/`.
  3. `execute` с ревью: ответ `standards-reviewer` — «Проверено: <файлы `code-style/kotlin/`>», без «Банк знаний не найден».
  4. `plan-executor` с готовым кодом из плана читает `docs/README.md`, но не `code-style/kotlin/` и `testing/kotlin/`.
  5. Критерий 4: объём чтения банка в шагах 2–4 — только разделы из строки работы.
