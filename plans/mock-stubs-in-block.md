# Заглушки мока — в блоке при создании

**Цель:** правило KT-KIT-13 «заглушки мока верхнего уровня спеки — только в блоке `mockk<T> { … }` при создании мока» в банке, его проверка своим правилом detekt и существующие нарушения `scripts/` в бейзлайне.
**Архитектура:** своё правило detekt `StubsInMockBlock` в `gradle-plugin/detekt-rules`: прямые выражения лямбды `BehaviorSpec({ … })`, самый левый вызов инфиксной цепочки с именем `every`/`coEvery`/`justRun`/`coJustRun` — находка. Правило регистрируется в `StandardsProvider`, включается в `detekt.yml` только для тестов; 127 существующих заглушек `scripts/` уходят в `scripts/detekt-baseline-test.xml`.
**Стек:** Kotlin, detekt 2 API (`dev.detekt.api`), PSI Kotlin, Kotest + KotestKit, Gradle (included build `gradle-plugin`).

---

## Ограничения
- Не трогать: `scripts/src/test/**` — перевод заглушек 26 спек в блок мока — задача #107; эта задача только пополняет бейзлайн.
- Правило — только для тестов: `includes: ['**/test/**']` в `detekt.yml`, как у `BehaviorSpecOnly`.
- Без разрешения типов: вызов определяется по имени; ложное срабатывание на свою функцию `every` — `@Suppress` по KT-DOC-7.
- Пополнение `scripts/detekt-baseline-test.xml` записями `StubsInMockBlock:` разрешено (решение груминга #154, пункт 3): нарушения существовали до подключения правила — это бейзлайн при подключении проверки, а не подавление нового кода (`docs/code-style/kotlin/README.md:48`). Разрешение — только для этих записей в задаче 3; другие правила и другие записи бейзлайна не трогаются.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: правило KT-KIT-13 в `docs/testing/kotlin/kotest-kit.md`, правило detekt `StubsInMockBlock`, его регистрация и включение, бейзлайн `scripts/`.
- Какую проблему решает, что даёт: заглушки общего мока спеки пишутся двумя способами, и банк разрешает оба (`kotest-kit.md:92-102`); отдельные `every` на уровне спеки оторваны от мока — читатель собирает мок по кускам (`GroomedCommandSpec.kt:25-32`). Правило даёт один способ, detekt не пропускает новое нарушение.
- Как встраивается: шестое правило набора `standards`; KT-KIT-3 остаётся правилом про инструменты мока, его пример «заглушка вызова» — внутри `given` со ссылкой на KT-KIT-13.
- Критерии цели:
  - `./gradlew -p gradle-plugin :detekt-rules:check` — зелёный, спека `StubsInMockBlockSpec` покрывает случаи груминга;
  - `./gradlew :scripts:check` — зелёный с правилом, включённым в `detekt.yml`;
  - в `scripts/detekt-baseline-test.xml` добавлены только записи `StubsInMockBlock:`, прежние не изменены;
  - KT-KIT-13 есть в `kotest-kit.md` с «Проверка: detekt `StubsInMockBlock`»; пример «заглушка вызова» KT-KIT-3 ссылается на KT-KIT-13.

## Контекст
- Задействованные файлы:
  - `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/BehaviorSpecOnly.kt` — образец правила по классу спеки: `RULE_ID`, `findSpecSuperType()`, `Finding` + `report`.
  - `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/FindSpecSuperType.kt:6` — `KtClass.findSpecSuperType(): String?`, короткое имя супертипа спеки.
  - `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StandardsProvider.kt:15-21` — список правил набора.
  - `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/FindLintLinesInFile.kt:9` — `Rule.findLintLinesInFile(code, filename): List<String>`, находки строками «строка:столбец сообщение».
  - `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StandardsProviderSpecData.kt:10-17` — ожидаемые имена правил.
  - `gradle-plugin/conventions/src/main/resources/standards/detekt.yml:80-94` — набор `standards`.
  - `gradle-plugin/conventions/src/main/kotlin/io/github/nakulov/standards/gradle/StandardsKotlinPlugin.kt:74-77` — бейзлайн проекта `detekt-baseline.xml` → detekt берёт `-main.xml` / `-test.xml`.
  - `scripts/detekt-baseline-test.xml` — 543 записи `<ID>`, формат `<Правило>:<файл>:<сигнатура>`.
  - `docs/testing/kotlin/kotest-kit.md:85-118` — KT-KIT-3; новое правило — после KT-KIT-12 (`:464-492`).
- Паттерны: спека правила — `withGivens` + `shouldReturnTest` + `findLintLinesInFile`, код примеров — константы в `<Правило>SpecData.kt` (`BehaviorSpecOnlySpec.kt`, `BehaviorSpecOnlySpecData.kt`).
- Зависимости: `scripts` получает правила из included build `gradle-plugin` (`settings.gradle.kts:3`, `StandardsKotlinPlugin.kt:78-80`); правило не активно, пока не включено в `detekt.yml`.

## Структура файлов
Отвечает за правило detekt:
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StubsInMockBlock.kt` — находит заглушку верхнего уровня спеки; вход: `StubsInMockBlock(config: Config)`
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StubsInMockBlockSpec.kt` — случаи находки и её отсутствия
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StubsInMockBlockSpecData.kt` — код примеров и ожидаемые находки

Отвечает за набор правил:
- Изменить: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StandardsProvider.kt:20` — регистрация `StubsInMockBlock`
- Изменить: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StandardsProviderSpecData.kt:16` — новое имя в ожидании

Отвечает за включение в проектах:
- Изменить: `gradle-plugin/conventions/src/main/resources/standards/detekt.yml:94` — `StubsInMockBlock` активно, только тесты
- Изменить: `scripts/detekt-baseline-test.xml` — существующие заглушки верхнего уровня `scripts/`

Отвечает за правило банка:
- Изменить: `docs/testing/kotlin/kotest-kit.md:100-106` — примеры «заглушка вызова» и «заглушка вызова с Unit» KT-KIT-3 внутри `given`, со ссылкой на KT-KIT-13
- Изменить: `docs/testing/kotlin/kotest-kit.md:492` — новое правило KT-KIT-13 в конце файла

≈ 8 файлов → 1 PR.

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подходы: из груминга — новое правило KT-KIT-13 + своё правило detekt + бейзлайн `scripts/`.
- Подход и архитектура: правило detekt по классу спеки, как `BehaviorSpecOnly`; смотрит только прямые выражения лямбды `BehaviorSpec({ … })`, вложенные блоки (`given`, `withGivens`, `mockk<T> { … }`) не проверяет.
- Решения:
  - имя правила `StubsInMockBlock` — говорит, где должна быть заглушка; груминг оставил имя плану;
  - граница — все четыре функции заглушки (`every`, `coEvery`, `justRun`, `coJustRun`) — иначе мок с `justRun` остаётся разорван (`GroomedCommandSpec.kt:29-30`);
  - инфиксная цепочка (`returns`, `answers`, `throws`, `andThen`, `coAnswers`, …) разворачивается влево до самого левого вызова — так `every { … } returns a andThen b` даёт одну находку на `every`;
  - находка на вызове `every { … }` (`Entity.from(call)`): строка и столбец указывают на заглушку, сигнатура бейзлайна — текст вызова;
  - вспомогательные функции — приватные функции класса правила, не расширения в том же файле: KT-NAME-3 разрешает рядом с декларацией только расширения этого класса;
  - правило активно только через `detekt.yml` — регистрация в `StandardsProvider` сама проверку в проектах не включает.
- Как вписывается в систему: шестое правило набора `standards`; в проектах на плагине включается обновлением плагина, существующие нарушения проекта — в его бейзлайн.
- Обратная совместимость: не нужна — правило новое; проекты на плагине получат новые находки в тестах и пополнят свой бейзлайн, как `scripts/` здесь.
- Запасной: проверка ревью вместо detekt — переходим, если PSI лямбды `BehaviorSpec({ … })` не даёт прямых выражений надёжно (спека задачи 1 не сходится).
- Отклонено:
  - только `every`/`coEvery` — мок с `justRun` остаётся разорван (`GroomedCommandSpec.kt:29-30`);
  - правило от уровня объявления мока (и внутри `given`) — шире кейса задачи;
  - проверка ревью — нарушение уже прошло ревью в 29 спеках;
  - перевод всех 26 спек `scripts/` в этой задаче — ≈ 32 файла, 2–3 PR, не нужен для решения; перевод — #107;
  - расширить KT-KIT-3 — две проверки в одном правиле, сообщение detekt ведёт к правилу про фейки.

## Технические детали
- Структуры данных и изменения: новых типов нет; правило — `internal class StubsInMockBlock(config: Config) : Rule`.
- Параметры и форматы:
  - сообщение находки: «KT-KIT-13. Заглушка верхнего уровня спеки — в блоке при создании мока `mockk<T> { … }`.»;
  - описание правила: «KT-KIT-13. Заглушки мока верхнего уровня спеки — в блоке при создании мока.»;
  - запись `detekt.yml`: `StubsInMockBlock: # KT-KIT-13, только тесты`, `active: true`, `includes: ['**/test/**']`;
  - запись бейзлайна: `<ID>StubsInMockBlock:<Файл>.kt:<текст вызова every { … }></ID>`; одинаковые вызовы в одном файле дают одну запись.
- Поток обработки:
  1. `visitClass`: `findSpecSuperType()` не `BehaviorSpec` — выход (не спека, или спека другого стиля — KT-KIT-1).
  2. Прямые выражения лямбды: `superTypeListEntries` → `KtSuperTypeCallEntry` → аргументы → `KtLambdaExpression` → `bodyExpression.statements`. Нет лямбды — пустой список, без находки.
  3. У каждого выражения — самый левый вызов: `KtBinaryExpression` → его `left`, рекурсивно; итог `KtCallExpression` или `null` (`val x = …`, `isolationMode = …` — не вызов).
  4. Имя вызова (`calleeExpression.text`) из набора заглушек — находка на вызове.
- Крайние случаи (из груминга):
  - заглушки в блоке `mockk<T> { … }`, в `given`, в `withGivens` — нет находки (вложенные лямбды);
  - мок из функции (`val git = makeGit()` + `every { git.… }` наверху) — находка: правило смотрит на заглушку, не на мок;
  - `verify`/`verifySequence` наверху — нет находки;
  - не `BehaviorSpec` (`FunSpec`) — нет находки;
  - `BehaviorSpec()` без лямбды — нет находки (пустой список выражений);
  - заглушка со значением, объявленным ниже мока, — находка: значение переносится выше мока;
  - `mockkStatic`/`mockkObject` — KT-KIT-5, правило их не смотрит.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: заглушки верхнего уровня спеки ловит detekt
  - [x] Задача 1: правило detekt `StubsInMockBlock`
  - [x] Задача 2: регистрация в `StandardsProvider`
  - [x] Задача 3: включение в `detekt.yml` и бейзлайн `scripts/`
  - [x] Задача 4: правило KT-KIT-13 в банке
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- #107 — перевод заглушек 26 спек `scripts/` и удаление записей `StubsInMockBlock:` из `scripts/detekt-baseline-test.xml`.

## PR 1: заглушки верхнего уровня спеки ловит detekt
**Объём:** правило detekt и его тесты, `StandardsProvider*`, `detekt.yml`, `scripts/detekt-baseline-test.xml`, `kotest-kit.md`; ≈ 8 файлов

### Задача 1: правило detekt `StubsInMockBlock` — находка на заглушке верхнего уровня `BehaviorSpec`
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StubsInMockBlock.kt` — правило
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StubsInMockBlockSpecData.kt` — код примеров, ожидаемая находка
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StubsInMockBlockSpec.kt` — случаи правила

- [x] 1. Падающий тест: четыре случая находки (`every … returns … andThen …`, `justRun`, `coEvery … coAnswers`, мок из `makeX()`) и пять без находки (заглушки в блоке мока, в `given` и `withGivens`, `verifySequence` наверху, `FunSpec`, `BehaviorSpec()` без лямбды). Во всех случаях с находкой заглушка — на строке 4, столбце 5: код начинается с перевода строки, строка 2 — класс, строка 3 — мок.

  `StubsInMockBlockSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.detekt

  const val TOP_LEVEL_STUB_MESSAGE =
      "KT-KIT-13. Заглушка верхнего уровня спеки — в блоке при создании мока `mockk<T> { … }`."

  const val TOP_LEVEL_EVERY_CODE = """
  class ExampleSpec : BehaviorSpec({
      val repository = mockk<Repository>()
      every { repository.root() } returns "/first" andThen "/second"
  })
  """

  const val TOP_LEVEL_JUST_RUN_CODE = """
  class ExampleSpec : BehaviorSpec({
      val console = mockk<Console>()
      justRun { console.line(text = any()) }
  })
  """

  const val TOP_LEVEL_CO_EVERY_CODE = """
  class ExampleSpec : BehaviorSpec({
      val repository = mockk<Repository>()
      coEvery { repository.load() } coAnswers { "loaded" }
  })
  """

  const val FACTORY_MOCK_STUB_CODE = """
  class ExampleSpec : BehaviorSpec({
      val repository = makeRepository()
      every { repository.root() } returns "/repo"
  })
  """

  const val MOCK_BLOCK_STUBS_CODE = """
  class ExampleSpec : BehaviorSpec({
      isolationMode = IsolationMode.InstancePerRoot

      val repository = mockk<Repository> {
          every { root() } returns "/repo"
          justRun { save(any()) }
      }
  })
  """

  const val NESTED_STUBS_CODE = """
  class ExampleSpec : BehaviorSpec({
      val repository = mockk<Repository>()

      given("репозиторий") {
          every { repository.root() } returns "/repo"
      }

      withGivens(
          nameFn = { case -> case.arg1 },
          case(arg1 = "/repo"),
      ) { root ->
          coEvery { repository.load() } returns root
      }
  })
  """

  const val TOP_LEVEL_VERIFY_CODE = """
  class ExampleSpec : BehaviorSpec({
      val repository = mockk<Repository>()
      verifySequence { repository.root() }
  })
  """

  const val FUN_SPEC_STUB_CODE = """
  class ExampleSpec : FunSpec({
      val repository = mockk<Repository>()
      every { repository.root() } returns "/repo"
  })
  """

  const val NO_LAMBDA_SPEC_CODE = """
  class ExampleSpec : BehaviorSpec()
  """

  fun makeTopLevelStubFindings(): List<String> {
      return listOf(
          "4:5 $TOP_LEVEL_STUB_MESSAGE",
      )
  }
  ```

  `StubsInMockBlockSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class StubsInMockBlockSpec : BehaviorSpec({
      withGivens(
          nameFn = { case -> case.arg2 },
          case(arg1 = makeTopLevelStubFindings(), arg2 = TOP_LEVEL_EVERY_CODE),
          case(arg1 = makeTopLevelStubFindings(), arg2 = TOP_LEVEL_JUST_RUN_CODE),
          case(arg1 = makeTopLevelStubFindings(), arg2 = TOP_LEVEL_CO_EVERY_CODE),
          case(arg1 = makeTopLevelStubFindings(), arg2 = FACTORY_MOCK_STUB_CODE),
          case(arg1 = emptyList(), arg2 = MOCK_BLOCK_STUBS_CODE),
          case(arg1 = emptyList(), arg2 = NESTED_STUBS_CODE),
          case(arg1 = emptyList(), arg2 = TOP_LEVEL_VERIFY_CODE),
          case(arg1 = emptyList(), arg2 = FUN_SPEC_STUB_CODE),
          case(arg1 = emptyList(), arg2 = NO_LAMBDA_SPEC_CODE),
      ) { expected, code ->
          shouldReturnTest(expected) {
              StubsInMockBlock(Config.empty).findLintLinesInFile(code, filename = "ExampleSpec.kt")
          }
      }
  })
  ```
- [x] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*StubsInMockBlockSpec*'` — падает: `Unresolved reference 'StubsInMockBlock'`
- [x] 3. Минимальный код: правило по потоку из «Технических деталей».

  `StubsInMockBlock.kt`:
  ```kotlin
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import dev.detekt.api.Entity
  import dev.detekt.api.Finding
  import dev.detekt.api.Rule
  import org.jetbrains.kotlin.psi.KtBinaryExpression
  import org.jetbrains.kotlin.psi.KtCallExpression
  import org.jetbrains.kotlin.psi.KtClass
  import org.jetbrains.kotlin.psi.KtExpression
  import org.jetbrains.kotlin.psi.KtLambdaExpression
  import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

  private const val RULE_ID = "KT-KIT-13"
  private const val BEHAVIOR_SPEC = "BehaviorSpec"
  private const val MESSAGE =
      "$RULE_ID. Заглушка верхнего уровня спеки — в блоке при создании мока `mockk<T> { … }`."
  private val STUB_CALLS = setOf("every", "coEvery", "justRun", "coJustRun")

  internal class StubsInMockBlock(
      config: Config,
  ) : Rule(
      config,
      description = "$RULE_ID. Заглушки мока верхнего уровня спеки — в блоке при создании мока.",
  ) {

      override fun visitClass(klass: KtClass) {
          super.visitClass(klass)
          if (klass.findSpecSuperType() != BEHAVIOR_SPEC) return
          findSpecStatements(klass)
              .mapNotNull { statement -> findLeftmostCall(statement) }
              .filter { call -> isStubCall(call) }
              .forEach { call -> reportStub(call) }
      }

      // Только прямые выражения лямбды `BehaviorSpec({ … })`: заглушки во вложенных блоках разрешены.
      private fun findSpecStatements(klass: KtClass): List<KtExpression> {
          return klass.superTypeListEntries
              .filterIsInstance<KtSuperTypeCallEntry>()
              .flatMap { entry -> entry.valueArguments }
              .mapNotNull { argument -> argument.getArgumentExpression() as? KtLambdaExpression }
              .flatMap { lambda ->
                  lambda.bodyExpression
                      ?.statements
                      .orEmpty()
              }
      }

      // Инфиксная цепочка `every { … } returns … andThen …` — дерево бинарных выражений, заглушка в самом левом.
      private fun findLeftmostCall(expression: KtExpression?): KtCallExpression? {
          if (expression is KtBinaryExpression) return findLeftmostCall(expression.left)
          return expression as? KtCallExpression
      }

      private fun isStubCall(call: KtCallExpression): Boolean {
          val name = call.calleeExpression
              ?.text
          return name in STUB_CALLS
      }

      private fun reportStub(call: KtCallExpression) {
          val entity = Entity.from(call)
          val finding = Finding(entity, MESSAGE)
          report(finding)
      }
  }
  ```
- [x] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*StubsInMockBlockSpec*'` — зелёный, 9 случаев
- [x] 5. Запустить `./gradlew -p gradle-plugin :detekt-rules:check` — `BUILD SUCCESSFUL`; падение — имя правила detekt (например `FunctionSignature`) или порог покрытия
- [x] 6. Коммит — тест и код вместе

### Задача 2: регистрация `StubsInMockBlock` в наборе `standards`
- Изменить: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StandardsProviderSpecData.kt:16` — новое имя в ожидании
- Изменить: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StandardsProvider.kt:20` — регистрация

- [x] 1. Падающий тест: в `makeStandardsRuleNames()` после `RuleName(value = "SpecDataContent"),` добавить строку
  ```kotlin
          RuleName(value = "StubsInMockBlock"),
  ```
- [x] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*StandardsProviderSpec*'` — падает: ожидаемый набор содержит `StubsInMockBlock`, фактический — нет
- [x] 3. Минимальный код: в `StandardsProvider.instance()` после `{ config -> SpecDataContent(config) },` добавить строку
  ```kotlin
                  { config -> StubsInMockBlock(config) },
  ```
- [x] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*StandardsProviderSpec*'` — зелёный
- [x] 5. Запустить `./gradlew -p gradle-plugin :detekt-rules:check` — `BUILD SUCCESSFUL`
- [x] 6. Коммит — тест и код вместе

### Задача 3: включение `StubsInMockBlock` в `detekt.yml` и бейзлайн `scripts/`
- Изменить: `gradle-plugin/conventions/src/main/resources/standards/detekt.yml:94` — правило активно, только тесты
- Изменить: `scripts/detekt-baseline-test.xml` — записи существующих заглушек верхнего уровня

Тест: проверка формата — сам detekt `scripts/`: до бейзлайна падает на `StubsInMockBlock`, после — зелёный.

- [x] 1. Включить правило: в `detekt.yml` после записи `SpecDataContent` (после строки 94 `    includes: ['**/test/**']`) добавить
  ```yaml
    StubsInMockBlock: # KT-KIT-13, только тесты
      active: true
      includes: ['**/test/**']
  ```
- [x] 2. Запустить `./gradlew :scripts:check` — падает: находки `StubsInMockBlock` в спеках `scripts/src/test` (≈ 127, например `GroomedCommandSpec.kt:29`)
- [x] 3. Найти задачу бейзлайна тестов: `./gradlew :scripts:tasks --all` → строка `detektBaselineTest` (предполагаю имя по плагину detekt 2: бейзлайн по набору исходников). Нет такой задачи — ⚠️, остановиться и сообщить.
- [x] 4. Пополнить бейзлайн: `./gradlew :scripts:detektBaselineTest` — `BUILD SUCCESSFUL`. Право на пополнение — «Ограничения», пункт о бейзлайне.
- [x] 5. Проверить дифф бейзлайна: `git diff --numstat scripts/detekt-baseline-test.xml` — удалённых строк 0; `git diff scripts/detekt-baseline-test.xml` — добавлены только строки `<ID>StubsInMockBlock:…</ID>`.
  Задача пересоздала файл и задела прежние записи (удалены, изменены, переставлены) — запасной путь: сохранить добавленные строки `StubsInMockBlock:` из диффа, вернуть файл `git checkout -- scripts/detekt-baseline-test.xml` и вставить эти строки через Edit перед `  </CurrentIssues>` в порядке сортировки по ID. Затем повторить проверку диффа: удалённых строк 0.
- [x] 6. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [x] 7. Запустить `./gradlew -p gradle-plugin :conventions:check` — `BUILD SUCCESSFUL`
- [x] 8. Коммит — `detekt.yml` и бейзлайн вместе

### Задача 4: правило KT-KIT-13 в банке
- Изменить: `docs/testing/kotlin/kotest-kit.md:100-106` — примеры «заглушка вызова» и «заглушка вызова с Unit» KT-KIT-3 внутри `given`, со ссылкой на KT-KIT-13
- Изменить: `docs/testing/kotlin/kotest-kit.md:492` — новое правило в конце файла

Тест: не применим — текст банка без логики; проверки модуля — тоже не применимы.

- [x] 1. В KT-KIT-3 заменить строки 100-106
  ```kotlin
  // Разрешено — заглушка вызова
  every { gitHub.read(makeAdminArgs()) } returns "true"
  coEvery { gitHub.load(makeAdminArgs()) } returns "true"

  // Разрешено — заглушка вызова с Unit
  justRun { pause.invoke(any()) }
  coJustRun { pause.invoke(any()) }
  ```
  на
  ```kotlin
  // Разрешено — заглушка вызова на один случай; общие заглушки уровня спеки — KT-KIT-13
  given(name = "администратор") {
      every { gitHub.read(makeAdminArgs()) } returns "true"
      coEvery { gitHub.load(makeAdminArgs()) } returns "true"
      // …
  }

  // Разрешено — заглушка вызова с Unit на один случай
  given(name = "пауза") {
      justRun { pause.invoke(any()) }
      coJustRun { pause.invoke(any()) }
      // …
  }
  ```
  Пример `:116-117` («Запрещено — every … just Runs») не меняется: он уже запрещён и показывает форму заглушки, а не её место.
- [x] 2. В конец файла, после блока KT-KIT-12, через пустую строку добавить
  ````markdown
  ### KT-KIT-13. Заглушки мока верхнего уровня спеки — в блоке при создании мока
  Почему: мок и его поведение читаются в одном месте; отдельные `every` ниже объявления разрывают мок — читатель собирает его по кускам среди других объявлений.
  Проверка: detekt `StubsInMockBlock` (своё правило из engineering-standards)
  - Верхний уровень спеки — прямые выражения лямбды `BehaviorSpec({ … })`. Заглушки там — `every`, `coEvery`, `justRun`, `coJustRun` — только в блоке `mockk<T> { … }`.
  - Заглушка на один случай — отдельной строкой внутри `given`, `when` или `withGivens`; мок, объявленный внутри `given`, правило не затрагивает.
  - Значение для заглушки объявляется выше мока.
  - Мок из функции (`val repository = makeRepository()`) с заглушкой на верхнем уровне — нарушение: заглушки — в блоке при создании мока внутри функции или в `given`.

  ```kotlin
  // Разрешено
  class RepositoryServiceSpec : BehaviorSpec({
      isolationMode = IsolationMode.InstancePerRoot

      val root = makeRoot()
      val repository = mockk<Repository> {
          every { root() } returns root.path
          justRun { save(any()) }
      }
      val service = RepositoryService(repository = repository)

      given(name = "пустой репозиторий") {
          every { repository.files() } returns emptyList()
          // …
      }
  })

  // Запрещено — заглушки мока отдельными строками на верхнем уровне спеки
  class RepositoryServiceSpec : BehaviorSpec({
      isolationMode = IsolationMode.InstancePerRoot

      val repository = mockk<Repository>()
      val service = RepositoryService(repository = repository)
      val root = makeRoot()
      every { repository.root() } returns root.path
      justRun { repository.save(any()) }
      // …
  })
  ```
  ````
- [x] 3. Проверить: `grep -n "KT-KIT-13" docs/testing/kotlin/kotest-kit.md` — строка заголовка правила и строка комментария в примере KT-KIT-3
- [x] 4. Коммит

### Приёмка
- [ ] Что даёт этот PR — проверено: с включённым правилом и без бейзлайна `./gradlew :scripts:check` падал на `StubsInMockBlock` (задача 3, шаг 2), с бейзлайном — зелёный (задача 3, шаг 6)
- [ ] Крайние случаи обработаны: случаи «Технических деталей» — в `StubsInMockBlockSpec`
- [ ] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check` и `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам
- [ ] Документация: `docs/testing/kotlin/kotest-kit.md` — KT-KIT-13 и пример KT-KIT-3 (задача 4)

## Открыто
- нет

## После завершения
- Ручная проверка: открыть `docs/testing/kotlin/kotest-kit.md` — KT-KIT-13 после KT-KIT-12, «Проверка: detekt `StubsInMockBlock`»; пример «заглушка вызова» KT-KIT-3 внутри `given` и ссылается на KT-KIT-13.
