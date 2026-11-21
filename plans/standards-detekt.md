# Общие правила detekt

**Цель:** правила кодстайла и тестов Kotlin проверяются detekt и Kover из общей сборки `gradle-plugin/` этого репозитория, подключённой к `scripts/` плагином соглашений `standards.kotlin`.
**Архитектура:** отдельная сборка `gradle-plugin/` (не `buildSrc`, чтобы эпик доставки опубликовал её без переписывания) с модулями `detekt-rules` (5 своих правил) и `conventions` (плагин `standards.kotlin`: detekt с конфигом стандарта, свои правила, ktlint, Kover с порогом проекта). Корневой `settings.gradle.kts` подключает её через `pluginManagement { includeBuild("gradle-plugin") }`, `scripts/` применяет плагин; текущие нарушения — в бейзлайне detekt, порог покрытия — текущий замер.
**Стек:** Kotlin 2.4.21, Gradle 9.8.2, detekt 2.0.0-alpha.6 (`dev.detekt`, `detekt-test`, `detekt-rules-ktlint-wrapper`), Kover 0.9.11, Gradle TestKit, KotestKit 2.2.0.

---

## Ограничения
- Не трогать: `scripts/task`, `scripts/setup` — сборка через `kotlinc` человека, Gradle-плагин её не касается.
- Не трогать: версию Kotlin, toolchain и `languageVersion` 2.2 в `scripts/build.gradle.kts` (#13) — плагин Kotlin не применяет.
- Не трогать: `../ui-design-system-generator/` — образец, правила #252, #253, #254, #263 остаются там.
- Код `gradle-plugin/` своим detekt не проверяется — пишется по стандарту, проверка ревью.
- Версия detekt — 2.0.0-alpha.6, как в генераторе: правила и тесты переносятся без переписывания API.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: сборка `gradle-plugin/` — 5 своих правил detekt и плагин `standards.kotlin`; подключение к `scripts/` с бейзлайном и порогом; новые правила стандарта KT-FUN-7, KT-FUN-8, KT-CLASS-10, KT-TEST-7 и строки «Проверка:».
- Какую проблему решает, что даёт: кодстайл держится только там, где его проверяет detekt; конфиг и правила — одним артефактом стандарта, а не копией в каждом проекте.
- Как встраивается: проект применяет `id("standards.kotlin")` после `kotlin("jvm")` и задаёт `standards { coverageMinBound = … }`; `check` запускает `detektMain`, `detektTest`, `koverVerify`.
- Критерии цели:
  - `./gradlew :scripts:check` зелёный: detekt с конфигом стандарта, 5 своих правил, ktlint, бейзлайн; Kover с порогом — замер `scripts/`, округлённый вниз;
  - новое нарушение роняет `check` с ID правила: пробное `it` в `scripts/` → KT-FUN-6;
  - покрытие ниже порога роняет `check` (`koverVerify`);
  - плагин без `kotlin("jvm")` и без `coverageMinBound` — ошибка конфигурации с текстом;
  - `./gradlew -p gradle-plugin check` зелёный: тесты правил и плагина;
  - правила стандарта KT-FUN-7, KT-FUN-8, KT-CLASS-10, KT-TEST-7 описаны; «Проверка:» у KT-NAME-3, KT-KIT-1, KT-TEST-2, KT-TEST-3, KT-TEST-5 называет правило detekt;
  - `.editorconfig` (источник `templates/project/editorconfig` и копия в корне) выключает `no-empty-first-line-in-class-body` с KT-FMT-3;
  - README кодстайла: бейзлайн только уменьшается, порог покрытия не снижается.

## Контекст
- Задействованные файлы:
  - `settings.gradle.kts` — один модуль `scripts`, `pluginManagement` без `includeBuild`; репозиторий KotestKit с `gpr.user`/`gpr.key`
  - `scripts/build.gradle.kts` — `kotlin("jvm") version "2.4.21"`, `languageVersion` 2.2, тесты с системными свойствами
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/cli/Main.kt:34,74`, `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:48` — точки входа: `exitProcess`
  - `templates/project/editorconfig` → `.editorconfig` — источник и копия (`scripts/src/main/kotlin/io/github/nakulov/standards/setup/StandardFiles.kt:56`)
  - `docs/code-style/kotlin/README.md:47` — формат «Проверка»; `functions.md`, `classes.md`, `naming.md:35`, `docs/testing/kotlin/organization.md`, `kotest-kit.md:8`
- Паттерны: правила и тесты генератора `../ui-design-system-generator/generator/detekt-rules/` (правило — `Rule(config, description)`, находка — `Finding(entity, message)`; тесты — `withGivens` + `lintSample`); `detekt.yml` и `build.gradle.kts:25-62` генератора — Kover, `check` → `detektMain`/`detektTest`.
- Отличия от генератора: образцы кода — в `…SpecData.kt` функциями `make…` (решение груминга), а не файлами `samples/`; находка в тесте — строка «строка:столбец сообщение» вместо класса `Reported`; сообщение начинается с ID стандарта; правила — `internal`, провайдер — `public` (его создаёт `ServiceLoader`); вспомогательные функции — члены класса без получателя (KT-FUN-1, KT-NAME-3).
- Зависимости (сверено по jar в кэше Gradle): `dev.detekt.gradle.extensions.DetektExtension` — `config`, `buildUponDefaultConfig`, `baseline`; `kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension.reports` → `verify` → `rule` → `KoverVerifyRule.minBound(Provider<Int>)`; id плагинов — `dev.detekt`, `org.jetbrains.kotlinx.kover`. По умолчанию в detekt 2.0: `Undocumented*` выключены, `NoEmptyFirstLineInClassBody` (ktlint) выключено, `ForbiddenComment` — в `style`, `EmptyFunctionBlock` — в `empty-blocks`.
- Исключения тестов в конфиге по умолчанию (сверено по `default-detekt-config.yml` из `detekt-core-2.0.0-alpha.6.jar`): у активных правил `'**/test/**'` — `TooManyFunctions`, `InstanceOfCheckForException`, `ThrowingExceptionsWithoutMessageOrCause`, `TooGenericExceptionCaught`, `FunctionNaming`, `ForEachOnRange`, `SpreadOperator`, `UnsafeCallOnNullableType`, `MagicNumber` (у него ещё `'**/*.kts'`); у выключенных (`Undocumented*`, `KDocReferencesNonPublicProperty`, `StringLiteralDuplication`, `LateinitUsage`) — не трогаем.
- Стандарт о `sealed`: наследники объявляются внутри (`naming.md:36`, `classes.md:232`) — `OneClassPerFile` строгий, вложенные не считает. Правила о `@Suppress` в стандарте нет — плагин его не трогает.

## Структура файлов

Отвечает за свои правила detekt:
- Создать: `gradle-plugin/settings.gradle.kts` — отдельная сборка: репозитории, версия Kotlin, модули
- Создать: `gradle-plugin/detekt-rules/build.gradle.kts` — модуль правил
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StandardsProvider.kt` — набор правил `standards`
- Создать: `gradle-plugin/detekt-rules/src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider` — регистрация набора
- Создать: `…/detekt/NamedLiteralArgument.kt` — KT-FUN-4
- Создать: `…/detekt/NoImplicitIt.kt` — KT-FUN-6
- Создать: `…/detekt/OneClassPerFile.kt` — KT-NAME-3: второй класс верхнего уровня
- Создать: `…/detekt/SpecSuperType.kt` — имя супертипа-спеки Kotest; вход: `KtClass.specSuperType(): String?`
- Создать: `…/detekt/BehaviorSpecOnly.kt` — KT-KIT-1
- Создать: `…/detekt/SpecDataContent.kt` — KT-TEST-2, KT-TEST-3, KT-TEST-5
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/RuleLint.kt` — запуск правила на коде; вход: `Rule.lintCode(code: String, filename: String): List<String>`, `T.lintCodeWithContext(code: String): List<String>`
- Создать: `…/detekt/StandardsProviderSpec.kt`, `StandardsProviderSpecData.kt` — id и состав набора
- Создать: `…/detekt/<Правило>Spec.kt`, `<Правило>SpecData.kt` — по паре на каждое из 5 правил

Отвечает за плагин соглашений:
- Изменить: `gradle-plugin/settings.gradle.kts` — модуль `conventions`
- Создать: `gradle-plugin/conventions/build.gradle.kts` — модуль плагина, TestKit
- Создать: `gradle-plugin/conventions/src/main/kotlin/io/github/nakulov/standards/gradle/StandardsKotlinPlugin.kt` — плагин `standards.kotlin`
- Создать: `…/gradle/StandardsExtension.kt` — `standards { coverageMinBound }`
- Создать: `…/gradle/MissingSetup.kt` — чего не хватает проекту; вход: `missingSetup(hasKotlin: Boolean, coverageMinBound: Int?): String?`
- Создать: `gradle-plugin/conventions/src/main/resources/standards/detekt.yml` — конфиг detekt стандарта
- Создать: `gradle-plugin/conventions/src/test/kotlin/io/github/nakulov/standards/gradle/CheckRun.kt` — запуск упавшего `check` в проекте-образце; вход: `failedCheckLines(fixture: String, mark: String): List<String>`
- Создать: `…/gradle/StandardsKotlinPluginSpec.kt`, `StandardsKotlinPluginSpecData.kt` — что роняет `check` в образцах
- Создать: `…/gradle/MissingSetupSpec.kt`, `MissingSetupSpecData.kt` — тексты ошибок конфигурации
- Создать: `gradle-plugin/conventions/src/test/fixtures/violation/settings.gradle.kts`, `build.gradle.kts`, `src/main/kotlin/sample/Names.kt` — образец с неявным `it`
- Создать: `gradle-plugin/conventions/src/test/fixtures/no-bound/settings.gradle.kts`, `build.gradle.kts` — образец без порога
- Создать: `gradle-plugin/conventions/src/test/fixtures/low-coverage/settings.gradle.kts`, `build.gradle.kts`, `src/main/kotlin/sample/Greeting.kt` — образец с порогом выше покрытия

Отвечает за подключение к `scripts/` и стандарт:
- Изменить: `settings.gradle.kts:1` — `includeBuild("gradle-plugin")` в `pluginManagement`
- Изменить: `scripts/build.gradle.kts:4` — `id("standards.kotlin")`, порог, исключение точек входа
- Создать: `scripts/detekt-baseline-main.xml`, `scripts/detekt-baseline-test.xml` — текущие нарушения (имена — как создаёт detekt, сверить в задаче 11)
- Изменить: `templates/project/editorconfig`, `.editorconfig` — выключить `no-empty-first-line-in-class-body`
- Изменить: `docs/code-style/kotlin/functions.md` — KT-FUN-7, KT-FUN-8
- Изменить: `docs/code-style/kotlin/classes.md` — KT-CLASS-10
- Изменить: `docs/code-style/kotlin/naming.md:35` — «Проверка» KT-NAME-3
- Изменить: `docs/testing/kotlin/kotest-kit.md:8` — «Проверка» KT-KIT-1
- Изменить: `docs/testing/kotlin/organization.md:19,38,95` — «Проверка» KT-TEST-2/3/5, KT-TEST-7
- Изменить: `docs/code-style/kotlin/README.md:47` — бейзлайн и порог

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: из груминга — А «переносим только то, за чем стоит правило стандарта»: 5 правил, конфиг «умолчания detekt + отклонения только с ID правила». Подходы: из груминга — А.
- Решения:
  - 7 блоков PR вместо 3 из груминга — изменено при плане: лимит 10 файлов на PR; блоки — по смыслу (правило или группа правил, detekt в плагине, настройки проекта, покрытие, подключение).
  - `TooManyFunctions` — исключение `'**/test/**'` оставлено, хотя груминг снял исключения тестов, — изменено при плане: в файле данных `make…` на каждый контекст (KT-TEST-4), их число растёт с числом случаев.
  - Свои правила подключаются файлом jar правил с classpath плагина (`detektPlugins(files(…))`), а не координатами — работает и из `includeBuild`, и в TestKit, без `includeBuild` вне `pluginManagement`.
  - Конфиг — ресурс плагина `standards/detekt.yml`, передаётся detekt через `TextResource.asFile()`; проект сужает своим файлом поверх (`detekt { config.from(…) }`).
  - Метаданные Kotlin в `conventions` — версии 2.2: Kotlin DSL проекта читает расширение `standards { }` компилятором Gradle, а он не читает метаданные новее себя.
  - Ошибки конфигурации — чистая функция `missingSetup` (все случаи — `shouldReturnTest`), плагин бросает `GradleException` в `afterEvaluate`.
  - TestKit — три проекта-образца, как в груминге, но без `kotlin("jvm")` — только `MissingSetupSpec` (образец без Kotlin — ещё 2 файла, а вызов `afterEvaluate` уже проверяет образец без порога): `violation` — нарушение `NoImplicitIt` роняет `check` с KT-FUN-6; `no-bound` — без `coverageMinBound` ошибка конфигурации; `low-coverage` — порог выше покрытия роняет `koverVerify`.
- Как вписывается в систему: `scripts/task` по-прежнему собирается `kotlinc`; Gradle — только для тестов и проверок.
- Обратная совместимость: не нужна — проверок в `scripts/` не было, бейзлайн держит текущие нарушения.
- Запасной: подключение правил координатами `io.github.nakulov.standards:detekt-rules` и `includeBuild("gradle-plugin")` ещё и вне `pluginManagement` — переходим, если jar правил не находится на classpath плагина (ошибка «правила detekt не в jar»).
- Отклонено: Б «все 11 правил, три группы» — правила сериализации пришлось бы грумить как новый раздел стандарта, `BlockBody` противоречит KT-FMT-6; Б «конфиг генератора как есть» — проверки без правила в документе; Б «только инструмент покрытия без порога» — до #107 покрытие падало бы незаметно; Б «эпик из 2–3 задач» — правила попали бы в `dev` раньше, чем их что-то запускает.

## Технические детали
- Сообщение находки: `KT-<РАЗДЕЛ>-<N>. <текст>` — ID стандарта первым; описание правила — тоже с ID.
- Набор правил: id `standards`; `BehaviorSpecOnly`, `SpecDataContent` — `includes: ['**/test/**']` в `detekt.yml`.
- Находка в тесте правила: строка `"<строка>:<столбец> <сообщение>"`. Образец кода в `make…` начинается с перевода строки после `"""` — код с 2-й строки.
- Порог: `standards.coverageMinBound` — `Property<Int>`, %, обязателен; передаётся в `KoverVerifyRule.minBound(Provider<Int>)`.
- Бейзлайн: плагин всегда ставит `detekt.baseline` на `detekt-baseline.xml` проекта; detekt для задач по source set берёт файл с суффиксом (`-main`, `-test`), отсутствующий файл пропускает — проверяет образец `violation` без бейзлайна (задача 7).
- Поток: `check` → `detektMain` + `detektTest` (с анализом типов — для `NamedArguments`, `NamedLiteralArgument`) + `koverVerify`; после `test` — `koverHtmlReport`.
- Запуск тестов `gradle-plugin/` — корневым wrapper: `./gradlew -p gradle-plugin …`.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: Сборка правил detekt с правилом KT-FUN-4
  - [x] Задача 1: Сборка `gradle-plugin/` и набор `standards`
  - [x] Задача 2: `NamedLiteralArgument`
  - [x] Приёмка
- PR 2: Базовые правила KT-FUN-6 и KT-NAME-3
  - [ ] Задача 3: `NoImplicitIt`
  - [ ] Задача 4: `OneClassPerFile`
  - [ ] Приёмка
- PR 3: Правила тестов KT-KIT-1 и KT-TEST-2/3/5
  - [ ] Задача 5: `BehaviorSpecOnly`
  - [ ] Задача 6: `SpecDataContent`
  - [ ] Приёмка
- PR 4: Плагин `standards.kotlin` подключает detekt стандарта
  - [ ] Задача 7: Плагин с detekt, конфигом и своими правилами
  - [ ] Приёмка
- PR 5: Плагин требует настроек проекта
  - [ ] Задача 8: Ошибки конфигурации `missingSetup`
  - [ ] Задача 9: Расширение `standards { coverageMinBound }` и проверка настроек
  - [ ] Приёмка
- PR 6: Плагин проверяет покрытие по порогу проекта
  - [ ] Задача 10: Kover с порогом `coverageMinBound`
  - [ ] Приёмка
- PR 7: `scripts/` проверяется стандартом
  - [ ] Перед блоком: «Почему» от пользователя
  - [ ] Задача 11: Подключение к `scripts/` с бейзлайном и порогом
  - [ ] Задача 12: Правила стандарта и `.editorconfig`
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Ручная проверка пробным нарушением в `scripts/` (раздел «После завершения»).
- #107 снимает бейзлайн и поднимает порог — связь уже записана в #107.

## PR 1: Сборка правил detekt с правилом KT-FUN-4
**Объём:** `gradle-plugin/settings.gradle.kts`, `detekt-rules/build.gradle.kts`, провайдер, регистрация, `NamedLiteralArgument`, `RuleLint.kt`, спеки и данные провайдера и правила; ≈ 10 файлов

### Задача 1: Сборка `gradle-plugin/` и набор `standards`
- Создать: `gradle-plugin/settings.gradle.kts` — сборка
- Создать: `gradle-plugin/detekt-rules/build.gradle.kts` — модуль
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/StandardsProvider.kt` — набор
- Создать: `gradle-plugin/detekt-rules/src/main/resources/META-INF/services/dev.detekt.api.RuleSetProvider` — регистрация
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/StandardsProviderSpec.kt`, `StandardsProviderSpecData.kt` — id и состав

- [x] 1. Падающий тест: набор называется `standards`, правил в нём пока нет (правила добавляет каждая следующая задача). Сборка:
  ```kotlin
  // gradle-plugin/settings.gradle.kts
  // Общая сборка стандарта: свои правила detekt и плагин соглашений. Отдельная сборка, а не buildSrc,
  // чтобы эпик доставки опубликовал её без переписывания; проекты подключают её через pluginManagement.
  pluginManagement {
      repositories {
          gradlePluginPortal()
          mavenCentral()
      }
      plugins {
          kotlin("jvm") version "2.4.21"
      }
  }

  dependencyResolutionManagement {
      repositories {
          gradlePluginPortal()
          mavenCentral()

          // KotestKit нужен только тестам сборки: без учётных данных сборка плагина для проекта не падает.
          maven {
              url = uri("https://maven.pkg.github.com/Nakulov/KotestKit")
              credentials {
                  username = providers.gradleProperty("gpr.user").orNull
                  password = providers.gradleProperty("gpr.key").orNull
              }
          }
      }
  }

  rootProject.name = "gradle-plugin"
  include("detekt-rules")
  ```
  ```kotlin
  // gradle-plugin/detekt-rules/build.gradle.kts
  plugins {
      kotlin("jvm")
  }

  val detektVersion = "2.0.0-alpha.6"

  dependencies {
      compileOnly("dev.detekt:detekt-api:$detektVersion")

      testImplementation("dev.detekt:detekt-api:$detektVersion")
      testImplementation("dev.detekt:detekt-test:$detektVersion")
      testImplementation("io.github.nakulov:kotestkit:2.2.0")
  }

  tasks.test {
      useJUnitPlatform()
  }
  ```
  ```kotlin
  // StandardsProviderSpecData.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.RuleName
  import dev.detekt.api.RuleSetId

  /** Набор правил в `detekt.yml`: `standards`. */
  fun makeStandardsRuleSetId(): RuleSetId {
      return RuleSetId(value = "standards")
  }

  /** Правила набора — по имени класса, как они включаются в `detekt.yml`. */
  fun makeStandardsRuleNames(): Set<RuleName> {
      return setOf(
      )
  }
  ```
  ```kotlin
  // StandardsProviderSpec.kt
  package io.github.nakulov.standards.detekt

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.kotest.core.spec.style.BehaviorSpec

  class StandardsProviderSpec :
      BehaviorSpec({
          shouldReturnTest(makeStandardsRuleSetId()) { StandardsProvider().ruleSetId }

          shouldReturnTest(makeStandardsRuleNames()) { StandardsProvider().instance().rules.keys }
      })
  ```
- [x] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*StandardsProviderSpec*'` — падает: `Unresolved reference 'StandardsProvider'`
- [x] 3. Минимальный код: провайдер и регистрация:
  ```kotlin
  // StandardsProvider.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.RuleSet
  import dev.detekt.api.RuleSetId
  import dev.detekt.api.RuleSetProvider

  /** Свои правила detekt стандарта: проверки правил кодстайла и тестов, которых нет в стандартном наборе detekt. */
  class StandardsProvider : RuleSetProvider {

      override val ruleSetId: RuleSetId = RuleSetId(value = "standards")

      override fun instance(): RuleSet {
          return RuleSet(
              ruleSetId,
              listOf(
              ),
          )
      }
  }
  ```
  ```
  # META-INF/services/dev.detekt.api.RuleSetProvider
  io.github.nakulov.standards.detekt.StandardsProvider
  ```
  Пустые `listOf()`/`setOf()` с переносом — место, куда задачи 2–6 добавляют по строке; тип выводится из параметра `RuleSet` и типа результата `make…`.
- [x] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*StandardsProviderSpec*'` — зелёный; ошибка репозиториев или версии Kotlin — разобраться до задачи 2
- [x] 5. Коммит — тест и код вместе

### Задача 2: `NamedLiteralArgument` — KT-FUN-4
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/NamedLiteralArgument.kt` — правило
- Создать: `gradle-plugin/detekt-rules/src/test/kotlin/io/github/nakulov/standards/detekt/RuleLint.kt` — запуск правила на коде
- Создать: `…/detekt/NamedLiteralArgumentSpec.kt`, `NamedLiteralArgumentSpecData.kt` — случаи
- Изменить: `…/detekt/StandardsProvider.kt`, `StandardsProviderSpecData.kt` — правило в наборе

- [x] 1. Падающий тест: литерал без имени (Kotlin и Java), с именем, исключения (`vararg`, инфикс, аннотация); в `makeStandardsRuleNames()` — `RuleName(value = "NamedLiteralArgument"),`.
  ```kotlin
  // RuleLint.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Finding
  import dev.detekt.api.RequiresAnalysisApi
  import dev.detekt.api.Rule
  import dev.detekt.test.lint
  import dev.detekt.test.lintWithContext
  import dev.detekt.test.utils.compileContentForTest
  import dev.detekt.test.utils.createEnvironment

  /** Находки правила на коде [code] из файла [filename]: строки «строка:столбец сообщение». */
  internal fun Rule.lintCode(code: String, filename: String): List<String> {
      val file = compileContentForTest(content = code, filename = filename)
      return lint(file).toLines()
  }

  /** То же для правила с анализом типов: код компилируется с classpath тестов (JDK, stdlib). */
  internal fun <T> T.lintCodeWithContext(code: String): List<String> where T : Rule, T : RequiresAnalysisApi {
      return lintWithContext(environment = createEnvironment(), content = code).toLines()
  }

  private fun List<Finding>.toLines(): List<String> {
      return map { finding ->
          val source = finding.entity.location.source
          "${source.line}:${source.column} ${finding.message}"
      }
  }
  ```
  ```kotlin
  // NamedLiteralArgumentSpecData.kt
  package io.github.nakulov.standards.detekt

  fun makeUnnamedLiteralCode(): String {
      return """
  open class Base(
      val name: String,
  )

  class Child : Base("x")

  fun greet(name: String, times: Int, loud: Boolean, mark: Char, extra: String?) {
      println(name)
  }

  fun main() {
      greet("Ann", 2, true, '!', null)
      repeat(-1) { }
  }
  """
  }

  fun makeUnnamedLiteralFindings(): List<String> {
      return listOf(
          "6:20 KT-FUN-4. Литерал \"x\" без имени: назовите параметр.",
          "13:11 KT-FUN-4. Литерал \"Ann\" без имени: назовите параметр.",
          "13:18 KT-FUN-4. Литерал 2 без имени: назовите параметр.",
          "13:21 KT-FUN-4. Литерал true без имени: назовите параметр.",
          "13:27 KT-FUN-4. Литерал '!' без имени: назовите параметр.",
          "13:32 KT-FUN-4. Литерал null без имени: назовите параметр.",
          "14:12 KT-FUN-4. Литерал -1 без имени: назовите параметр.",
      )
  }

  fun makeJavaLiteralCode(): String {
      return """
  fun main() {
      System.err.println("x")
      System.err.println(/* p0 = */ "y")
  }
  """
  }

  fun makeJavaLiteralFindings(): List<String> {
      return listOf(
          "3:24 KT-FUN-4. Литерал \"x\" без имени: добавьте комментарий /* имя = */ (метод Java).",
      )
  }

  fun makeNamedLiteralCode(): String {
      return """
  fun greet(name: String, times: Int) {
      println(message = name)
  }

  fun main() {
      greet(name = "Ann", times = -1)
  }
  """
  }

  fun makeLiteralExceptionsCode(): String {
      return """
  @Suppress("UNUSED")
  fun main() {
      val list = listOf("a", "b")
      val pair = "a" to 1
      println(message = list + pair.first)
  }
  """
  }
  ```
  ```kotlin
  // NamedLiteralArgumentSpec.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class NamedLiteralArgumentSpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "код: ${case.arg2}" },
              case(arg1 = makeUnnamedLiteralFindings(), arg2 = "литералы без имени", arg3 = makeUnnamedLiteralCode()),
              case(arg1 = makeJavaLiteralFindings(), arg2 = "метод Java", arg3 = makeJavaLiteralCode()),
              case(arg1 = emptyList(), arg2 = "литералы с именем", arg3 = makeNamedLiteralCode()),
              case(arg1 = emptyList(), arg2 = "vararg, инфикс, аннотация", arg3 = makeLiteralExceptionsCode()),
          ) { expected, _, code ->
              shouldReturnTest(expected) { NamedLiteralArgument(config = Config.empty).lintCodeWithContext(code = code) }
          }
      })
  ```
- [x] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*NamedLiteralArgumentSpec*'` — падает: `Unresolved reference 'NamedLiteralArgument'`
- [x] 3. Минимальный код:
  ```kotlin
  // NamedLiteralArgument.kt
  package io.github.nakulov.standards.detekt

  import com.intellij.psi.PsiComment
  import com.intellij.psi.util.PsiTreeUtil
  import dev.detekt.api.Config
  import dev.detekt.api.Entity
  import dev.detekt.api.Finding
  import dev.detekt.api.RequiresAnalysisApi
  import dev.detekt.api.Rule
  import org.jetbrains.kotlin.analysis.api.analyze
  import org.jetbrains.kotlin.analysis.api.resolution.singleFunctionCallOrNull
  import org.jetbrains.kotlin.analysis.api.resolution.symbol
  import org.jetbrains.kotlin.psi.KtCallElement
  import org.jetbrains.kotlin.psi.KtCallExpression
  import org.jetbrains.kotlin.psi.KtConstantExpression
  import org.jetbrains.kotlin.psi.KtExpression
  import org.jetbrains.kotlin.psi.KtLambdaArgument
  import org.jetbrains.kotlin.psi.KtPrefixExpression
  import org.jetbrains.kotlin.psi.KtStringTemplateExpression
  import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
  import org.jetbrains.kotlin.psi.KtValueArgument

  private val NAME_COMMENT = Regex(pattern = """/\*\s*\w+\s*=\s*\*/""")

  /**
   * KT-FUN-4: литерал в аргументе вызова (число, `Boolean`, `Char`, строка, `null`) — с именем параметра. У вызова
   * Java-метода имена параметров не стабильны: перед литералом комментарий `/* имя = */`. Не затрагиваются аргументы
   * аннотаций, `vararg` и инфиксные вызовы.
   */
  internal class NamedLiteralArgument(
      config: Config,
  ) : Rule(
          config = config,
          description = "KT-FUN-4. Литерал в аргументе вызова — с именем параметра.",
      ),
      RequiresAnalysisApi {

      override fun visitCallExpression(expression: KtCallExpression) {
          super.visitCallExpression(expression)
          reportUnnamedLiterals(call = expression)
      }

      override fun visitSuperTypeCallEntry(call: KtSuperTypeCallEntry) {
          super.visitSuperTypeCallEntry(call)
          reportUnnamedLiterals(call = call)
      }

      private fun reportUnnamedLiterals(call: KtCallElement) {
          val literals = call.valueArguments
              .filterIsInstance<KtValueArgument>()
              .filter { argument -> isUnnamedLiteral(argument = argument) }
          if (literals.isEmpty()) {
              return
          }
          analyze(call) {
              val functionCall = call.resolveToCall()?.singleFunctionCallOrNull() ?: return@analyze
              val stableNames = functionCall.symbol.hasStableParameterNames
              literals
                  .filter { argument ->
                      functionCall.valueArgumentMapping[argument.getArgumentExpression()]?.symbol?.isVararg == false
                  }
                  .forEach { argument -> reportArgument(argument = argument, stableNames = stableNames) }
          }
      }

      private fun reportArgument(argument: KtValueArgument, stableNames: Boolean) {
          if (!stableNames && hasNameComment(argument = argument)) {
              return
          }
          val hint = if (stableNames) {
              "назовите параметр"
          } else {
              "добавьте комментарий /* имя = */ (метод Java)"
          }
          report(
              Finding(
                  entity = Entity.from(argument),
                  message = "KT-FUN-4. Литерал ${argument.text} без имени: $hint.",
              ),
          )
      }

      private fun isUnnamedLiteral(argument: KtValueArgument): Boolean {
          return !argument.isNamed() &&
              argument !is KtLambdaArgument &&
              isLiteral(expression = argument.getArgumentExpression())
      }

      private fun isLiteral(expression: KtExpression?): Boolean {
          return when (expression) {
              is KtConstantExpression, is KtStringTemplateExpression -> true
              is KtPrefixExpression -> expression.baseExpression is KtConstantExpression
              else -> false
          }
      }

      private fun hasNameComment(argument: KtValueArgument): Boolean {
          val comment = argument.firstChild as? PsiComment
              ?: PsiTreeUtil.skipWhitespacesBackward(argument) as? PsiComment
          return comment != null && NAME_COMMENT.matches(input = comment.text)
      }
  }
  ```
  В `StandardsProvider.instance()` — `{ config -> NamedLiteralArgument(config = config) },` в `listOf(`.
- [x] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test` — зелёный (`NamedLiteralArgumentSpec`, `StandardsProviderSpec`); столбец или строка находки разошлись — сверить отступ образца (код с 2-й строки), не менять правило
- [x] 5. Коммит — тест и код вместе

### Приёмка
- [x] Набор `standards` с `NamedLiteralArgument` собирается, `./gradlew -p gradle-plugin :detekt-rules:test` → `BUILD SUCCESSFUL`
- [x] Крайние случаи: метод Java с комментарием и без, `vararg`, инфикс, аннотация, вызов конструктора супертипа
- [x] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check` и `./gradlew :scripts:test`

## PR 2: Базовые правила KT-FUN-6 и KT-NAME-3
**Объём:** `NoImplicitIt`, `OneClassPerFile` с парами спека + данные, провайдер и его данные, `naming.md`; ≈ 9 файлов

### Задача 3: `NoImplicitIt` — KT-FUN-6
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/NoImplicitIt.kt`
- Создать: `…/detekt/NoImplicitItSpec.kt`, `NoImplicitItSpecData.kt`
- Изменить: `…/detekt/StandardsProvider.kt`, `StandardsProviderSpecData.kt` — правило в наборе

- [ ] 1. Падающий тест: неявный `it`, `it` во вложенной лямбде, именованный параметр и `by lazy { }` без `it`.
  ```kotlin
  // NoImplicitItSpecData.kt
  package io.github.nakulov.standards.detekt

  fun makeImplicitItCode(): String {
      return """
  fun upper(names: List<String>): List<String> {
      return names.map { it.uppercase() }
  }
  """
  }

  fun makeImplicitItFindings(): List<String> {
      return listOf(
          "3:22 KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`).",
      )
  }

  fun makeNestedItCode(): String {
      return """
  fun letters(names: List<String>): List<String> {
      return names.map { name -> name.filter { it.isLetter() } }
  }
  """
  }

  fun makeNestedItFindings(): List<String> {
      return listOf(
          "3:44 KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`).",
      )
  }

  fun makeNamedParameterCode(): String {
      return """
  fun upper(names: List<String>): List<String> {
      val size by lazy { names.size }
      return names.map { name -> name.uppercase() }
  }
  """
  }
  ```
  ```kotlin
  // NoImplicitItSpec.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class NoImplicitItSpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "код: ${case.arg2}" },
              case(arg1 = makeImplicitItFindings(), arg2 = "неявный it", arg3 = makeImplicitItCode()),
              case(arg1 = makeNestedItFindings(), arg2 = "it во вложенной лямбде", arg3 = makeNestedItCode()),
              case(arg1 = emptyList(), arg2 = "параметр с именем", arg3 = makeNamedParameterCode()),
          ) { expected, _, code ->
              shouldReturnTest(expected) {
                  NoImplicitIt(config = Config.empty).lintCode(code = code, filename = "Sample.kt")
              }
          }
      })
  ```
  В `StandardsProviderSpecData.makeStandardsRuleNames()` — добавить `RuleName(value = "NoImplicitIt"),` после `NamedLiteralArgument`.
- [ ] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*NoImplicitItSpec*'` — падает: `Unresolved reference 'NoImplicitIt'`
- [ ] 3. Минимальный код:
  ```kotlin
  // NoImplicitIt.kt
  package io.github.nakulov.standards.detekt

  import com.intellij.psi.util.PsiTreeUtil
  import dev.detekt.api.Config
  import dev.detekt.api.Entity
  import dev.detekt.api.Finding
  import dev.detekt.api.Rule
  import org.jetbrains.kotlin.psi.KtLambdaExpression
  import org.jetbrains.kotlin.psi.KtNameReferenceExpression

  private const val IMPLICIT_IT = "it"
  private const val MESSAGE = "KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`)."

  /** KT-FUN-6: параметр лямбды — с именем; лямбда без параметров, которая читает `it`, — находка. */
  internal class NoImplicitIt(
      config: Config,
  ) : Rule(
          config = config,
          description = "KT-FUN-6. Параметр лямбды — с именем, неявный it не пишется.",
      ) {

      override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
          super.visitLambdaExpression(lambdaExpression)
          if (lambdaExpression.functionLiteral.hasParameterSpecification()) {
              return
          }
          val references = PsiTreeUtil.collectElementsOfType(lambdaExpression, KtNameReferenceExpression::class.java)
          if (references.any { reference -> isImplicitItOf(reference = reference, lambda = lambdaExpression) }) {
              report(Finding(entity = Entity.from(lambdaExpression), message = MESSAGE))
          }
      }

      private fun isImplicitItOf(reference: KtNameReferenceExpression, lambda: KtLambdaExpression): Boolean {
          val owner = PsiTreeUtil.getParentOfType(reference, KtLambdaExpression::class.java)
          return reference.getReferencedName() == IMPLICIT_IT && owner == lambda
      }
  }
  ```
  В `StandardsProvider.instance()` — добавить `{ config -> NoImplicitIt(config = config) },` после `NamedLiteralArgument`.
- [ ] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Задача 4: `OneClassPerFile` — KT-NAME-3
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/OneClassPerFile.kt`
- Создать: `…/detekt/OneClassPerFileSpec.kt`, `OneClassPerFileSpecData.kt`
- Изменить: `…/detekt/StandardsProvider.kt`, `StandardsProviderSpecData.kt`
- Изменить: `docs/code-style/kotlin/naming.md:35` — «Проверка»

- [ ] 1. Падающий тест: три класса в файле, наследник `sealed` рядом, а не внутри (стандарт: `naming.md:36`), вложенные наследники `sealed` и приватный вложенный класс не считаются.
  ```kotlin
  // OneClassPerFileSpecData.kt
  package io.github.nakulov.standards.detekt

  fun makeTwoClassesCode(): String {
      return """
  package sample

  class First

  class Second

  interface Third
  """
  }

  fun makeTwoClassesFindings(): List<String> {
      return listOf(
          "6:7 KT-NAME-3. «Second» — второй класс верхнего уровня в файле: вынесите в свой файл.",
          "8:11 KT-NAME-3. «Third» — второй класс верхнего уровня в файле: вынесите в свой файл.",
      )
  }

  fun makeSealedBesideCode(): String {
      return """
  sealed interface State

  data object Idle : State
  """
  }

  fun makeSealedBesideFindings(): List<String> {
      return listOf(
          "4:13 KT-NAME-3. «Idle» — второй класс верхнего уровня в файле: вынесите в свой файл.",
      )
  }

  fun makeNestedClassesCode(): String {
      return """
  package sample

  sealed class Shape {

      data class Circle(
          val radius: Int,
      ) : Shape()

      private class Helper
  }

  fun makeShape(): Shape {
      return Shape.Circle(radius = 1)
  }
  """
  }
  ```
  ```kotlin
  // OneClassPerFileSpec.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class OneClassPerFileSpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "код: ${case.arg2}" },
              case(arg1 = makeTwoClassesFindings(), arg2 = "три класса", arg3 = makeTwoClassesCode()),
              case(arg1 = makeSealedBesideFindings(), arg2 = "наследник sealed рядом", arg3 = makeSealedBesideCode()),
              case(arg1 = emptyList(), arg2 = "вложенные классы", arg3 = makeNestedClassesCode()),
          ) { expected, _, code ->
              shouldReturnTest(expected) {
                  OneClassPerFile(config = Config.empty).lintCode(code = code, filename = "Sample.kt")
              }
          }
      })
  ```
  В `makeStandardsRuleNames()` — добавить `RuleName(value = "OneClassPerFile"),`.
- [ ] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*OneClassPerFileSpec*'` — падает: `Unresolved reference 'OneClassPerFile'`
- [ ] 3. Минимальный код (сообщение без «или внутрь первого» — KT-CLASS-9 запрещает вложенные классы):
  ```kotlin
  // OneClassPerFile.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import dev.detekt.api.Entity
  import dev.detekt.api.Finding
  import dev.detekt.api.Rule
  import org.jetbrains.kotlin.psi.KtClassOrObject
  import org.jetbrains.kotlin.psi.KtFile

  /** KT-NAME-3: один класс, объект или интерфейс верхнего уровня на файл; вложенные не считаются. */
  internal class OneClassPerFile(
      config: Config,
  ) : Rule(
          config = config,
          description = "KT-NAME-3. Одна верхнеуровневая декларация на файл: второй класс — в свой файл.",
      ) {

      override fun visitKtFile(file: KtFile) {
          super.visitKtFile(file)
          file.declarations
              .filterIsInstance<KtClassOrObject>()
              .drop(n = 1)
              .forEach { declaration -> reportSecond(declaration = declaration) }
      }

      private fun reportSecond(declaration: KtClassOrObject) {
          report(
              Finding(
                  entity = Entity.atName(declaration),
                  message = "KT-NAME-3. «${declaration.name}» — второй класс верхнего уровня в файле: вынесите в свой файл.",
              ),
          )
      }
  }
  ```
  В `StandardsProvider.instance()` — добавить `{ config -> OneClassPerFile(config = config) },`.
  `docs/code-style/kotlin/naming.md:35`:
  - было: `Проверка: detekt \`MatchingDeclarationName\``
  - стало: `Проверка: detekt \`MatchingDeclarationName\` (имя файла), \`OneClassPerFile\` (своё правило: второй класс)`
- [ ] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test` — зелёный
- [ ] 5. Коммит — тест, код и строка стандарта вместе

### Приёмка
- [ ] Набор `standards` — 3 правила, `StandardsProviderSpec` зелёный
- [ ] Крайние случаи: вложенная лямбда, `by lazy`, наследник `sealed` рядом и внутри, приватный вложенный класс
- [ ] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check`

## PR 3: Правила тестов KT-KIT-1 и KT-TEST-2/3/5
**Объём:** `SpecSuperType`, `BehaviorSpecOnly`, `SpecDataContent` с парами спека + данные, провайдер и его данные, `kotest-kit.md`; ≈ 10 файлов

### Задача 5: `BehaviorSpecOnly` — KT-KIT-1
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/SpecSuperType.kt` — общее для двух правил тестов
- Создать: `…/detekt/BehaviorSpecOnly.kt`
- Создать: `…/detekt/BehaviorSpecOnlySpec.kt`, `BehaviorSpecOnlySpecData.kt`
- Изменить: `…/detekt/StandardsProvider.kt`, `StandardsProviderSpecData.kt`
- Изменить: `docs/testing/kotlin/kotest-kit.md:8` — «Проверка»

- [ ] 1. Падающий тест: `FunSpec` — находка; `BehaviorSpec` с полным именем и класс-не-спека — нет.
  ```kotlin
  // BehaviorSpecOnlySpecData.kt
  package io.github.nakulov.standards.detekt

  fun makeFunSpecCode(): String {
      return """
  class ExampleSpec :
      FunSpec({
      })
  """
  }

  fun makeFunSpecFindings(): List<String> {
      return listOf(
          "2:7 KT-KIT-1. «ExampleSpec» наследует FunSpec: спека — только BehaviorSpec.",
      )
  }

  fun makeBehaviorSpecCode(): String {
      return """
  class ExampleSpec :
      io.kotest.core.spec.style.BehaviorSpec({
      })

  class Child : Base()
  """
  }
  ```
  ```kotlin
  // BehaviorSpecOnlySpec.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class BehaviorSpecOnlySpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "код: ${case.arg2}" },
              case(arg1 = makeFunSpecFindings(), arg2 = "FunSpec", arg3 = makeFunSpecCode()),
              case(arg1 = emptyList(), arg2 = "BehaviorSpec и не спека", arg3 = makeBehaviorSpecCode()),
          ) { expected, _, code ->
              shouldReturnTest(expected) {
                  BehaviorSpecOnly(config = Config.empty).lintCode(code = code, filename = "ExampleSpec.kt")
              }
          }
      })
  ```
  В `makeStandardsRuleNames()` — добавить `RuleName(value = "BehaviorSpecOnly"),`.
- [ ] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*BehaviorSpecOnlySpec*'` — падает: `Unresolved reference 'BehaviorSpecOnly'`
- [ ] 3. Минимальный код:
  ```kotlin
  // SpecSuperType.kt
  package io.github.nakulov.standards.detekt

  import org.jetbrains.kotlin.psi.KtClass

  /** Короткое имя супертипа-спеки Kotest (оканчивается на `Spec`) или `null`, если класс — не спека. */
  internal fun KtClass.specSuperType(): String? {
      return superTypeListEntries
          .map { entry -> entry.typeReference?.text.orEmpty() }
          .map { name -> name.substringBefore(delimiter = '<').substringAfterLast(delimiter = '.') }
          .firstOrNull { name -> name.endsWith(suffix = "Spec") }
  }
  ```
  ```kotlin
  // BehaviorSpecOnly.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import dev.detekt.api.Entity
  import dev.detekt.api.Finding
  import dev.detekt.api.Rule
  import org.jetbrains.kotlin.psi.KtClass

  private const val BEHAVIOR_SPEC = "BehaviorSpec"

  /** KT-KIT-1: спека Kotest — только `BehaviorSpec`. В `detekt.yml` — только для тестов. */
  internal class BehaviorSpecOnly(
      config: Config,
  ) : Rule(
          config = config,
          description = "KT-KIT-1. Спеки — только BehaviorSpec.",
      ) {

      override fun visitClass(klass: KtClass) {
          super.visitClass(klass)
          val spec = klass.specSuperType() ?: return
          if (spec != BEHAVIOR_SPEC) {
              report(
                  Finding(
                      entity = Entity.atName(klass),
                      message = "KT-KIT-1. «${klass.name}» наследует $spec: спека — только BehaviorSpec.",
                  ),
              )
          }
      }
  }
  ```
  В `StandardsProvider.instance()` — добавить `{ config -> BehaviorSpecOnly(config = config) },`.
  `docs/testing/kotlin/kotest-kit.md:8`:
  - было: `Проверка: ревью`
  - стало: `Проверка: detekt \`BehaviorSpecOnly\` (своё правило из engineering-standards)`
- [ ] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test` — зелёный
- [ ] 5. Коммит — тест, код и строка стандарта вместе

### Задача 6: `SpecDataContent` — KT-TEST-2, KT-TEST-3, KT-TEST-5
- Создать: `gradle-plugin/detekt-rules/src/main/kotlin/io/github/nakulov/standards/detekt/SpecDataContent.kt`
- Создать: `…/detekt/SpecDataContentSpec.kt`, `SpecDataContentSpecData.kt`
- Изменить: `…/detekt/StandardsProvider.kt`, `StandardsProviderSpecData.kt`

- [ ] 1. Падающий тест: в `*SpecData.kt` — `val` без `const`, функция без `make`, `makeup` (не `make` + заглавная), класс-спека; правильный файл данных (константа, `typealias`, data-класс, приватный `make…`) и обычный файл — без находок.
  ```kotlin
  // SpecDataContentSpecData.kt
  package io.github.nakulov.standards.detekt

  fun makeBrokenSpecDataCode(): String {
      return """
  package sample

  const val NAME = "x"

  val shared = listOf(1)

  fun id(): String {
      return NAME
  }

  fun makeup(): String {
      return NAME
  }

  class ExampleSpec :
      BehaviorSpec({
      })
  """
  }

  fun makeBrokenSpecDataFindings(): List<String> {
      return listOf(
          "6:5 KT-TEST-5. «shared»: val без const.",
          "8:5 KT-TEST-3. «id»: функция без префикса make.",
          "12:5 KT-TEST-3. «makeup»: функция без префикса make.",
          "16:7 KT-TEST-2. «ExampleSpec»: класс-спека (BehaviorSpec) в файле данных.",
      )
  }

  fun makeValidSpecDataCode(): String {
      return """
  package sample

  const val NAME = "x"

  typealias Name = String

  data class Fixture(
      val name: Name,
  )

  fun makeId(): String {
      return NAME
  }

  private fun makeHelper(): String {
      return NAME
  }
  """
  }

  fun makePlainFileCode(): String {
      return """
  package sample

  val shared = listOf(1)

  fun id(): String {
      return "x"
  }
  """
  }
  ```
  ```kotlin
  // SpecDataContentSpec.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class SpecDataContentSpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "файл: ${case.arg2}" },
              case(arg1 = makeBrokenSpecDataFindings(), arg2 = "BrokenSpecData.kt", arg3 = makeBrokenSpecDataCode()),
              case(arg1 = emptyList(), arg2 = "ExampleSpecData.kt", arg3 = makeValidSpecDataCode()),
              case(arg1 = emptyList(), arg2 = "Example.kt", arg3 = makePlainFileCode()),
          ) { expected, filename, code ->
              shouldReturnTest(expected) {
                  SpecDataContent(config = Config.empty).lintCode(code = code, filename = filename)
              }
          }
      })
  ```
  В `makeStandardsRuleNames()` — добавить `RuleName(value = "SpecDataContent"),`.
- [ ] 2. Запустить `./gradlew -p gradle-plugin :detekt-rules:test --tests '*SpecDataContentSpec*'` — падает: `Unresolved reference 'SpecDataContent'`
- [ ] 3. Минимальный код:
  ```kotlin
  // SpecDataContent.kt
  package io.github.nakulov.standards.detekt

  import dev.detekt.api.Config
  import dev.detekt.api.Entity
  import dev.detekt.api.Finding
  import dev.detekt.api.Rule
  import org.jetbrains.kotlin.lexer.KtTokens
  import org.jetbrains.kotlin.psi.KtClass
  import org.jetbrains.kotlin.psi.KtFile
  import org.jetbrains.kotlin.psi.KtNamedDeclaration
  import org.jetbrains.kotlin.psi.KtNamedFunction
  import org.jetbrains.kotlin.psi.KtProperty

  private const val SPEC_DATA_SUFFIX = "SpecData.kt"
  private val MAKE = Regex(pattern = "make[A-Z].*")

  /**
   * Тестовые данные `*SpecData.kt`: классов-спек нет (KT-TEST-2), функции — `make…` (KT-TEST-3),
   * `val` — только `const val` (KT-TEST-5). В `detekt.yml` — только для тестов.
   */
  internal class SpecDataContent(
      config: Config,
  ) : Rule(
          config = config,
          description = "KT-TEST-2, KT-TEST-3, KT-TEST-5. В *SpecData.kt нет классов-спек, функции — make…, val — const.",
      ) {

      override fun visitKtFile(file: KtFile) {
          super.visitKtFile(file)
          if (!file.name.endsWith(suffix = SPEC_DATA_SUFFIX)) {
              return
          }
          file.declarations
              .filterIsInstance<KtNamedDeclaration>()
              .forEach { declaration -> reportProblem(declaration = declaration) }
      }

      private fun reportProblem(declaration: KtNamedDeclaration) {
          val problem = problemOf(declaration = declaration) ?: return
          report(Finding(entity = Entity.atName(declaration), message = problem))
      }

      private fun problemOf(declaration: KtNamedDeclaration): String? {
          return when (declaration) {
              is KtClass -> specClassProblem(klass = declaration)
              is KtNamedFunction -> functionProblem(function = declaration)
              is KtProperty -> propertyProblem(property = declaration)
              else -> null
          }
      }

      private fun specClassProblem(klass: KtClass): String? {
          val spec = klass.specSuperType() ?: return null
          return "KT-TEST-2. «${klass.name}»: класс-спека ($spec) в файле данных."
      }

      private fun functionProblem(function: KtNamedFunction): String? {
          if (MAKE.matches(input = function.name.orEmpty())) {
              return null
          }
          return "KT-TEST-3. «${function.name}»: функция без префикса make."
      }

      private fun propertyProblem(property: KtProperty): String? {
          if (property.hasModifier(KtTokens.CONST_KEYWORD)) {
              return null
          }
          return "KT-TEST-5. «${property.name}»: val без const."
      }
  }
  ```
  В `StandardsProvider.instance()` — добавить `{ config -> SpecDataContent(config = config) },`.
- [ ] 4. Запустить `./gradlew -p gradle-plugin :detekt-rules:test` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Набор `standards` — 5 правил, `StandardsProviderSpec` зелёный
- [ ] Крайние случаи: полное имя `BehaviorSpec`, приватный `make…`, `makeup`, `typealias` и data-класс в файле данных, обычный файл
- [ ] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check`

## PR 4: Плагин `standards.kotlin` подключает detekt стандарта
**Объём:** `gradle-plugin/settings.gradle.kts`, модуль `conventions` (сборка, плагин, `detekt.yml`, спека, данные, `CheckRun.kt`), проект-образец из 3 файлов; ≈ 10 файлов

### Задача 7: Плагин с detekt, конфигом и своими правилами
- Изменить: `gradle-plugin/settings.gradle.kts` — `include("conventions")`
- Создать: `gradle-plugin/conventions/build.gradle.kts`
- Создать: `gradle-plugin/conventions/src/main/kotlin/io/github/nakulov/standards/gradle/StandardsKotlinPlugin.kt`
- Создать: `gradle-plugin/conventions/src/main/resources/standards/detekt.yml`
- Создать: `gradle-plugin/conventions/src/test/kotlin/io/github/nakulov/standards/gradle/CheckRun.kt`
- Создать: `…/gradle/StandardsKotlinPluginSpec.kt`, `StandardsKotlinPluginSpecData.kt`
- Создать: `gradle-plugin/conventions/src/test/fixtures/violation/settings.gradle.kts`, `build.gradle.kts`, `src/main/kotlin/sample/Names.kt`

- [ ] 1. Падающий тест: в проекте-образце неявный `it` — `check` падает, находка с KT-FUN-6.
  `gradle-plugin/settings.gradle.kts`:
  - было: `include("detekt-rules")`
  - стало: `include("detekt-rules", "conventions")`
  ```kotlin
  // gradle-plugin/conventions/build.gradle.kts
  import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

  plugins {
      kotlin("jvm")
      `java-gradle-plugin`
  }

  // Kotlin DSL проекта читает классы плагина компилятором Gradle: метаданные — не новее Kotlin внутри Gradle.
  kotlin {
      compilerOptions {
          languageVersion.set(KotlinVersion.KOTLIN_2_2)
          apiVersion.set(KotlinVersion.KOTLIN_2_2)
      }
  }

  // Плагин Kotlin для проекта-образца TestKit: на одном classpath с плагином, иначе detekt не видит классы Kotlin.
  val testPluginClasspath: Configuration by configurations.creating

  dependencies {
      implementation(project(":detekt-rules"))
      implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.6")

      testImplementation("io.github.nakulov:kotestkit:2.2.0")
      testPluginClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
  }

  gradlePlugin {
      plugins {
          create("standardsKotlin") {
              id = "standards.kotlin"
              implementationClass = "io.github.nakulov.standards.gradle.StandardsKotlinPlugin"
          }
      }
  }

  tasks.pluginUnderTestMetadata {
      pluginClasspath.from(testPluginClasspath)
  }

  // Проекты-образцы TestKit: путь — тестам, содержимое — входы теста (без их build и кэшей).
  tasks.test {
      useJUnitPlatform()
      systemProperty("standards.fixtures", file("src/test/fixtures").absolutePath)
      inputs.files(
          fileTree("src/test/fixtures") {
              exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**")
          },
      )
  }
  ```
  ```kotlin
  // fixtures/violation/settings.gradle.kts
  dependencyResolutionManagement {
      repositories {
          mavenCentral()
      }
  }

  rootProject.name = "violation"
  ```
  ```kotlin
  // fixtures/violation/build.gradle.kts
  plugins {
      id("org.jetbrains.kotlin.jvm")
      id("standards.kotlin")
  }
  ```
  ```kotlin
  // fixtures/violation/src/main/kotlin/sample/Names.kt
  package sample

  internal fun trimmed(names: List<String>): List<String> {
      return names.map { it.trim() }
  }
  ```
  ```kotlin
  // CheckRun.kt
  package io.github.nakulov.standards.gradle

  import org.gradle.testkit.runner.GradleRunner
  import java.io.File

  private const val FIXTURES = "standards.fixtures"
  private const val CHECK = "check"

  /**
   * Строки вывода упавшего `check` в проекте-образце [fixture], где есть [mark]; путь к образцу из строк убран.
   * Образцы задач 9 и 10 используют её же: находка detekt, ошибка конфигурации, нарушение порога Kover.
   */
  internal fun failedCheckLines(fixture: String, mark: String): List<String> {
      val project = File(System.getProperty(FIXTURES), fixture)
      val result = GradleRunner.create()
          .withProjectDir(project)
          .withPluginClasspath()
          .withArguments(CHECK)
          .buildAndFail()
      return result.output
          .lines()
          .filter { line -> mark in line }
          .map { line -> line.replace(oldValue = project.absolutePath, newValue = "") }
  }
  ```
  ```kotlin
  // StandardsKotlinPluginSpecData.kt
  package io.github.nakulov.standards.gradle

  /** Находка `NoImplicitIt` в образце `violation`: строка консольного отчёта detekt. */
  fun makeViolationFindings(): List<String> {
      return listOf(
          "/src/main/kotlin/sample/Names.kt:4:22: KT-FUN-6. Лямбда с неявным it: назовите параметр (`{ item -> … }`). [NoImplicitIt]",
      )
  }
  ```
  ```kotlin
  // StandardsKotlinPluginSpec.kt
  package io.github.nakulov.standards.gradle

  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class StandardsKotlinPluginSpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "образец: ${case.arg2}" },
              case(arg1 = makeViolationFindings(), arg2 = "violation", arg3 = ".kt:"),
          ) { expected, fixture, mark ->
              shouldReturnTest(expected) { failedCheckLines(fixture = fixture, mark = mark) }
          }
      })
  ```
- [ ] 2. Запустить `./gradlew -p gradle-plugin :conventions:test` — падает: в выводе образца `Could not find implementation class 'io.github.nakulov.standards.gradle.StandardsKotlinPlugin'` (класса ещё нет; дескриптор плагина `gradlePlugin { }` уже есть)
- [ ] 3. Минимальный код — плагин и конфиг:
  ```kotlin
  // StandardsKotlinPlugin.kt
  package io.github.nakulov.standards.gradle

  import dev.detekt.gradle.extensions.DetektExtension
  import org.gradle.api.Plugin
  import org.gradle.api.Project
  import org.gradle.api.tasks.TaskProvider
  import java.io.File
  import java.net.JarURLConnection

  private const val KOTLIN_PLUGIN = "org.jetbrains.kotlin.jvm"
  private const val DETEKT_PLUGIN = "dev.detekt"
  private const val DETEKT_PLUGINS = "detektPlugins"
  private const val KTLINT_WRAPPER = "dev.detekt:detekt-rules-ktlint-wrapper:2.0.0-alpha.6"
  private const val CONFIG_RESOURCE = "/standards/detekt.yml"
  private const val RULES_CLASS = "io/github/nakulov/standards/detekt/StandardsProvider.class"
  private const val BASELINE = "detekt-baseline.xml"
  private const val CHECK = "check"
  private const val PLAIN_DETEKT = "detekt"
  private const val DETEKT_MAIN = "detektMain"
  private const val DETEKT_TEST = "detektTest"

  /**
   * Плагин соглашений Kotlin `standards.kotlin`: detekt с конфигом стандарта, своими правилами и ktlint.
   * Kotlin не применяет — версия, toolchain и `languageVersion` остаются в проекте.
   */
  class StandardsKotlinPlugin : Plugin<Project> {

      override fun apply(project: Project) {
          project.pluginManager.withPlugin(KOTLIN_PLUGIN) { _ ->
              applyDetekt(project = project)
          }
      }

      private fun applyDetekt(project: Project) {
          project.pluginManager.apply(DETEKT_PLUGIN)
          val detekt = project.extensions.getByType(DetektExtension::class.java)
          detekt.buildUponDefaultConfig.set(/* value = */ true)
          detekt.config.from(project.provider { project.resources.text.fromString(configText()).asFile() })
          // Бейзлайн проекта: detekt берёт detekt-baseline-main.xml / -test.xml, отсутствующий файл пропускает.
          detekt.baseline.set(project.file(BASELINE))
          project.dependencies.add(DETEKT_PLUGINS, project.files(rulesJar()))
          project.dependencies.add(DETEKT_PLUGINS, KTLINT_WRAPPER)
          // NamedArguments и NamedLiteralArgument работают только с анализом типов: check — detektMain и detektTest.
          project.tasks.named(CHECK) { check ->
              check.setDependsOn(
                  check.dependsOn.filterNot { dependency -> (dependency as? TaskProvider<*>)?.name == PLAIN_DETEKT },
              )
              check.dependsOn(DETEKT_MAIN, DETEKT_TEST)
          }
      }

      private fun configText(): String {
          val resource = requireNotNull(javaClass.getResource(/* name = */ CONFIG_RESOURCE)) {
              "standards.kotlin: в плагине нет $CONFIG_RESOURCE"
          }
          return resource.readText()
      }

      // Свои правила — jar модуля detekt-rules с classpath плагина: так они находятся и из includeBuild, и в TestKit.
      private fun rulesJar(): File {
          val resource = requireNotNull(javaClass.classLoader.getResource(/* name = */ RULES_CLASS)) {
              "standards.kotlin: правил detekt стандарта нет на classpath плагина"
          }
          val connection = resource.openConnection()
          require(connection is JarURLConnection) { "standards.kotlin: правила detekt не в jar: $resource" }
          return File(connection.jarFileURL.toURI())
      }
  }
  ```
  ```yaml
  # gradle-plugin/conventions/src/main/resources/standards/detekt.yml
  # Конфиг detekt стандарта — поверх конфига по умолчанию (buildUponDefaultConfig). Каждое отклонение — с ID правила
  # стандарта (docs/code-style/kotlin, docs/testing/kotlin). Проект сужает правила своим файлом поверх (detekt.config.from).
  # Тесты проверяются наравне с кодом: у активных правил сняты исключения '**/test/**' конфига по умолчанию;
  # исключения не по тестам ('**/*.kts') оставлены.

  complexity:
    LargeClass: # KT-CLASS-10
      allowedLines: 250
    LongMethod: # KT-FUN-7
      allowedLines: 40
    NamedArguments: # KT-FUN-5
      active: true
    NestedBlockDepth: # KT-FUN-7
      allowedDepth: 2
    TooManyFunctions: # исключение '**/test/**' оставлено: KT-TEST-4 — в файле данных make… на каждый контекст
      active: true

  empty-blocks:
    EmptyFunctionBlock: # KT-DOC-6: пустой override и реализация по умолчанию в интерфейсе разрешены
      ignoreOverridden: true

  exceptions:
    InstanceOfCheckForException:
      excludes: []
    ThrowingExceptionsWithoutMessageOrCause:
      excludes: []
    TooGenericExceptionCaught:
      excludes: []

  naming:
    FunctionNaming:
      excludes: []

  performance:
    ForEachOnRange:
      excludes: []
    SpreadOperator:
      excludes: []

  potential-bugs:
    UnsafeCallOnNullableType:
      excludes: []

  style:
    ForbiddenComment: # KT-DOC-6
      comments:
        - reason: 'KT-DOC-6: заглушка запрещена — допишите код сейчас или заведите задачу.'
          value: 'TODO'
        - reason: 'KT-DOC-6: заглушка запрещена — исправьте сейчас или заведите задачу.'
          value: 'FIXME'
    ForbiddenMethodCall: # KT-FMT-7, KT-DOC-6; let и takeIf разрешены
      active: true
      methods:
        - reason: 'KT-FMT-7: with запрещён — обращайтесь к получателю по имени.'
          value: 'kotlin.with'
        - reason: 'KT-FMT-7: also запрещён — отдельная строка с именованной переменной.'
          value: 'kotlin.also'
        - reason: 'KT-FMT-7: run запрещён — блок кода обычной функцией или локальной переменной.'
          value: 'kotlin.run'
        - reason: 'KT-FMT-7: apply запрещён — настройка через параметры конструктора или именованную переменную.'
          value: 'kotlin.apply'
        - reason: 'KT-DOC-6: заглушка TODO() запрещена — допишите код или заведите задачу.'
          value: 'kotlin.TODO'
    MagicNumber:
      excludes: ['**/*.kts']
    ReturnCount: # KT-FUN-8: ранний выход не считается
      excludeGuardClauses: true

  ktlint:
    CommentWrapping: # KT-FUN-4: комментарий /* имя = */ перед литералом у аргумента Java-метода
      active: false
    FunctionExpressionBody: # KT-FMT-6: тело-выражение разрешено, но не обязательно
      active: false

  # Свои правила стандарта: модуль gradle-plugin/detekt-rules.
  standards:
    active: true
    NamedLiteralArgument: # KT-FUN-4
      active: true
    NoImplicitIt: # KT-FUN-6
      active: true
    OneClassPerFile: # KT-NAME-3
      active: true
    BehaviorSpecOnly: # KT-KIT-1, только тесты
      active: true
      includes: ['**/test/**']
    SpecDataContent: # KT-TEST-2, KT-TEST-3, KT-TEST-5, только тесты
      active: true
      includes: ['**/test/**']
  ```
- [ ] 4. Запустить `./gradlew -p gradle-plugin :conventions:test` — зелёный. Строка находки разошлась только форматом консольного отчёта detekt 2.0 (порядок пути, `[NoImplicitIt]`) — поправить ожидание в `makeViolationFindings()` по выводу, ID и текст сообщения не менять; строка повторяется дважды — ожидание с двумя строками, не фильтр в `CheckRun.kt`. Нет находки — разобраться (jar правил, конфиг) до коммита; ошибка «правила detekt не в jar» — запасной вариант из «Обзора решения», ⚠️ и вопрос пользователю.
  Опыт без коммита (настройка `EmptyFunctionBlock`, KT-DOC-6): временно добавить в образец `src/main/kotlin/sample/Listener.kt` — `internal interface Listener {\n\n    fun onShow() {\n        // По умолчанию делать нечего.\n    }\n}` и `src/main/kotlin/sample/QuietListener.kt` — `internal class QuietListener : Listener {\n\n    override fun onShow() {\n    }\n}`, запустить тест — в выводе нет `EmptyFunctionBlock`; удалить оба файла. Есть находка — ⚠️ и вопрос пользователю.
  Опыт без коммита (правила подключаются из `includeBuild`, до задачи 11): временно в корневом `settings.gradle.kts` — `includeBuild("gradle-plugin")` первой строкой в `pluginManagement { }`, в `scripts/build.gradle.kts` — `id("standards.kotlin")` в `plugins { }`; запустить `./gradlew :scripts:detektMain` — нет ошибки «правила detekt не в jar» и «нет на classpath плагина», в выводе отчёт detekt (находки и падение допустимы, бейзлайна ещё нет); вернуть оба файла (`git -C /Users/nakulov/projects/engineering-standards diff --stat` — пусто вне `gradle-plugin/`). Ошибка про jar — запасной вариант из «Обзора решения», ⚠️ и вопрос пользователю.
- [ ] 5. Коммит — тест, плагин, конфиг и образец вместе

### Приёмка
- [ ] Плагин подключает detekt стандарта: нарушение `NoImplicitIt` роняет `check` с KT-FUN-6
- [ ] Крайние случаи: пустой `override` и реализация по умолчанию в интерфейсе не находка (опыт шага 4); без бейзлайна detekt работает
- [ ] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check`

## PR 5: Плагин требует настроек проекта
**Объём:** `MissingSetup.kt` с парой спека + данные, `StandardsExtension.kt`, плагин, `StandardsKotlinPluginSpec.kt` и данные, `fixtures/violation/build.gradle.kts`, образец `no-bound` (2 файла); ≈ 10 файлов

### Задача 8: Ошибки конфигурации `missingSetup`
- Создать: `gradle-plugin/conventions/src/main/kotlin/io/github/nakulov/standards/gradle/MissingSetup.kt`
- Создать: `gradle-plugin/conventions/src/test/kotlin/io/github/nakulov/standards/gradle/MissingSetupSpec.kt`, `MissingSetupSpecData.kt`

- [ ] 1. Падающий тест: нет `kotlin("jvm")` — текст ошибки; нет порога — текст ошибки; всё задано — `null`.
  ```kotlin
  // MissingSetupSpecData.kt
  package io.github.nakulov.standards.gradle

  const val COVERAGE_MIN_BOUND = 80

  fun makeNoKotlinError(): String? {
      return "standards.kotlin требует kotlin(\"jvm\"): примените его в plugins рядом со standards.kotlin."
  }

  fun makeNoBoundError(): String? {
      return "standards.kotlin: задайте порог покрытия — standards { coverageMinBound = <замер, %> } (KT-TEST-7)."
  }

  fun makeCompleteSetup(): String? {
      return null
  }
  ```
  ```kotlin
  // MissingSetupSpec.kt
  package io.github.nakulov.standards.gradle

  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.kotest.core.spec.style.BehaviorSpec

  class MissingSetupSpec :
      BehaviorSpec({
          withGivens(
              nameFn = { case -> "kotlin: ${case.arg2}, порог: ${case.arg3}" },
              case(arg1 = makeNoKotlinError(), arg2 = false, arg3 = COVERAGE_MIN_BOUND),
              case(arg1 = makeNoBoundError(), arg2 = true, arg3 = null),
              case(arg1 = makeCompleteSetup(), arg2 = true, arg3 = COVERAGE_MIN_BOUND),
          ) { expected, hasKotlin, coverageMinBound ->
              shouldReturnTest(expected) { missingSetup(hasKotlin = hasKotlin, coverageMinBound = coverageMinBound) }
          }
      })
  ```
- [ ] 2. Запустить `./gradlew -p gradle-plugin :conventions:test --tests '*MissingSetupSpec*'` — падает: `Unresolved reference 'missingSetup'`
- [ ] 3. Минимальный код:
  ```kotlin
  // MissingSetup.kt
  package io.github.nakulov.standards.gradle

  private const val NO_KOTLIN = "standards.kotlin требует kotlin(\"jvm\"): " +
      "примените его в plugins рядом со standards.kotlin."
  private const val NO_BOUND = "standards.kotlin: задайте порог покрытия — " +
      "standards { coverageMinBound = <замер, %> } (KT-TEST-7)."

  /** Чего не хватает проекту для `standards.kotlin`: текст ошибки конфигурации или `null`, если всё задано. */
  internal fun missingSetup(hasKotlin: Boolean, coverageMinBound: Int?): String? {
      if (!hasKotlin) {
          return NO_KOTLIN
      }
      if (coverageMinBound == null) {
          return NO_BOUND
      }
      return null
  }
  ```
- [ ] 4. Запустить `./gradlew -p gradle-plugin :conventions:test --tests '*MissingSetupSpec*'` — зелёный
- [ ] 5. Коммит — тест и код вместе

### Задача 9: Расширение `standards { coverageMinBound }` и проверка настроек
- Создать: `gradle-plugin/conventions/src/main/kotlin/io/github/nakulov/standards/gradle/StandardsExtension.kt`
- Изменить: `…/gradle/StandardsKotlinPlugin.kt` — расширение, проверка настроек в `afterEvaluate`
- Изменить: `gradle-plugin/conventions/src/test/fixtures/violation/build.gradle.kts` — порог
- Создать: `gradle-plugin/conventions/src/test/fixtures/no-bound/settings.gradle.kts`, `build.gradle.kts`
- Изменить: `…/gradle/StandardsKotlinPluginSpec.kt`, `StandardsKotlinPluginSpecData.kt` — случай `no-bound`

- [ ] 1. Падающий тест: образец без порога — ошибка конфигурации с текстом `missingSetup`; образец `violation` задаёт порог и падает по-прежнему только на находке detekt.
  ```kotlin
  // fixtures/violation/build.gradle.kts
  plugins {
      id("org.jetbrains.kotlin.jvm")
      id("standards.kotlin")
  }

  // Тестов в образце нет: порог 0, чтобы check падал только на находке detekt.
  standards {
      coverageMinBound = 0
  }
  ```
  ```kotlin
  // fixtures/no-bound/settings.gradle.kts
  dependencyResolutionManagement {
      repositories {
          mavenCentral()
      }
  }

  rootProject.name = "no-bound"
  ```
  ```kotlin
  // fixtures/no-bound/build.gradle.kts — порога нет: настройка проекта, без которой плагин не работает
  plugins {
      id("org.jetbrains.kotlin.jvm")
      id("standards.kotlin")
  }
  ```
  В `StandardsKotlinPluginSpecData.kt`:
  ```kotlin
  /** Ошибка конфигурации образца `no-bound`: строка вывода Gradle с текстом `missingSetup`. */
  fun makeNoBoundOutput(): List<String> {
      return listOf(
          "> standards.kotlin: задайте порог покрытия — standards { coverageMinBound = <замер, %> } (KT-TEST-7).",
      )
  }
  ```
  В `StandardsKotlinPluginSpec.kt`, в `withGivens` после случая `violation`:
  ```kotlin
              case(arg1 = makeNoBoundOutput(), arg2 = "no-bound", arg3 = "standards.kotlin:"),
  ```
- [ ] 2. Запустить `./gradlew -p gradle-plugin :conventions:test --tests '*StandardsKotlinPluginSpec*'` — падает: у `violation` в выводе `Unresolved reference: standards`, находки `NoImplicitIt` нет; у `no-bound` сборка не падает (`buildAndFail` — `UnexpectedBuildSuccess`)
- [ ] 3. Минимальный код:
  ```kotlin
  // StandardsExtension.kt
  package io.github.nakulov.standards.gradle

  import org.gradle.api.provider.Property

  /** Настройки проекта для `standards.kotlin`: `standards { coverageMinBound = 85 }`. */
  abstract class StandardsExtension {

      /** Порог покрытия строк тестами, %: замер проекта с округлением вниз, не снижается (KT-TEST-7). */
      abstract val coverageMinBound: Property<Int>
  }
  ```
  `StandardsKotlinPlugin.kt` — импорт `org.gradle.api.GradleException`; константа:
  ```kotlin
  private const val EXTENSION = "standards"
  ```
  `apply` целиком:
  ```kotlin
      override fun apply(project: Project) {
          val standards = project.extensions.create(/* name = */ EXTENSION, StandardsExtension::class.java)
          project.pluginManager.withPlugin(KOTLIN_PLUGIN) { _ ->
              applyDetekt(project = project)
          }
          project.afterEvaluate { evaluated ->
              val problem = missingSetup(
                  hasKotlin = evaluated.pluginManager.hasPlugin(KOTLIN_PLUGIN),
                  coverageMinBound = standards.coverageMinBound.orNull,
              )
              if (problem != null) {
                  throw GradleException(problem)
              }
          }
      }
  ```
  KDoc класса — дописать: «без `kotlin("jvm")` или без порога `standards { coverageMinBound }` — ошибка конфигурации».
- [ ] 4. Запустить `./gradlew -p gradle-plugin check` — зелёный (`StandardsKotlinPluginSpec` — оба образца, `MissingSetupSpec`, правила). Строка ошибки `no-bound` разошлась только префиксом вывода Gradle (`>`, отступ) — поправить ожидание в `makeNoBoundOutput()` по выводу, текст `missingSetup` не менять.
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Плагин требует настроек: без порога — ошибка конфигурации с текстом (образец `no-bound`), без `kotlin("jvm")` — текст из `MissingSetupSpec`
- [ ] Крайние случаи: `violation` с порогом по-прежнему падает только на находке detekt
- [ ] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check`

## PR 6: Плагин проверяет покрытие по порогу проекта
**Объём:** `conventions/build.gradle.kts`, плагин, `StandardsKotlinPluginSpec.kt` и данные, образец `low-coverage` (3 файла); ≈ 7 файлов

### Задача 10: Kover с порогом `coverageMinBound`
- Изменить: `gradle-plugin/conventions/build.gradle.kts` — зависимость Kover
- Изменить: `…/gradle/StandardsKotlinPlugin.kt` — Kover, порог, HTML-отчёт
- Создать: `gradle-plugin/conventions/src/test/fixtures/low-coverage/settings.gradle.kts`, `build.gradle.kts`, `src/main/kotlin/sample/Greeting.kt`
- Изменить: `…/gradle/StandardsKotlinPluginSpec.kt`, `StandardsKotlinPluginSpecData.kt` — случай `low-coverage`

- [ ] 1. Падающий тест: порог 100 при непокрытом коде — `check` падает в `koverVerify`.
  ```kotlin
  // fixtures/low-coverage/settings.gradle.kts
  dependencyResolutionManagement {
      repositories {
          mavenCentral()
      }
  }

  rootProject.name = "low-coverage"
  ```
  ```kotlin
  // fixtures/low-coverage/build.gradle.kts
  plugins {
      id("org.jetbrains.kotlin.jvm")
      id("standards.kotlin")
  }

  // Тестов в образце нет: покрытие 0, порог 100 — koverVerify падает.
  standards {
      coverageMinBound = 100
  }
  ```
  ```kotlin
  // fixtures/low-coverage/src/main/kotlin/sample/Greeting.kt
  package sample

  internal fun greeting(name: String): String {
      return "Привет, $name"
  }
  ```
  В `StandardsKotlinPluginSpecData.kt`:
  ```kotlin
  /** Нарушение порога в образце `low-coverage`: строка вывода koverVerify. */
  fun makeLowCoverageOutput(): List<String> {
      return listOf(
          "  Rule violated: lines covered percentage is 0.000000, but expected minimum is 100",
      )
  }
  ```
  В `StandardsKotlinPluginSpec.kt`, в `withGivens` после случая `no-bound`:
  ```kotlin
              case(arg1 = makeLowCoverageOutput(), arg2 = "low-coverage", arg3 = "Rule violated"),
  ```
- [ ] 2. Запустить `./gradlew -p gradle-plugin :conventions:test --tests '*StandardsKotlinPluginSpec*'` — падает: у `low-coverage` сборка не падает (`UnexpectedBuildSuccess`: Kover ещё не применён)
- [ ] 3. Минимальный код:
  `gradle-plugin/conventions/build.gradle.kts`, в `dependencies` после `detekt-gradle-plugin`:
  ```kotlin
      implementation("org.jetbrains.kotlinx:kover-gradle-plugin:0.9.11")
  ```
  `StandardsKotlinPlugin.kt` — импорты `kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension`, `kotlinx.kover.gradle.plugin.dsl.KoverVerificationRulesConfig`, `org.gradle.api.tasks.testing.Test`; константы:
  ```kotlin
  private const val KOVER_PLUGIN = "org.jetbrains.kotlinx.kover"
  private const val KOVER_HTML_REPORT = "koverHtmlReport"
  ```
  В `apply`, в `withPlugin(KOTLIN_PLUGIN)` после `applyDetekt(project = project)` — `applyKover(project = project, standards = standards)`; новые функции:
  ```kotlin
      // koverVerify Kover сам ставит в check; HTML-отчёт — после каждого прогона тестов: build/reports/kover/html.
      private fun applyKover(project: Project, standards: StandardsExtension) {
          project.pluginManager.apply(KOVER_PLUGIN)
          val kover = project.extensions.getByType(KoverProjectExtension::class.java)
          kover.reports { reports ->
              reports.verify { rules -> addCoverageRule(rules = rules, standards = standards) }
          }
          project.tasks.withType(Test::class.java).configureEach { test -> test.finalizedBy(KOVER_HTML_REPORT) }
      }

      private fun addCoverageRule(rules: KoverVerificationRulesConfig, standards: StandardsExtension) {
          rules.rule { rule -> rule.minBound(standards.coverageMinBound) }
      }
  ```
  KDoc класса — дописать: «и Kover с порогом проекта `standards { coverageMinBound }`».
- [ ] 4. Запустить `./gradlew -p gradle-plugin check` — зелёный (`StandardsKotlinPluginSpec` — три образца, `MissingSetupSpec`, правила). Строка `low-coverage` разошлась форматом сообщения Kover 0.9.11 — поправить ожидание в `makeLowCoverageOutput()` по выводу, порог 100 и факт 0 в строке должны остаться.
- [ ] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Плагин проверяет покрытие: порог выше покрытия роняет `koverVerify` (образец `low-coverage`)
- [ ] Крайние случаи: порог 0 в образцах без тестов (`violation`) — `koverVerify` не падает
- [ ] Полный набор тестов зелёный: `./gradlew -p gradle-plugin check`

## PR 7: `scripts/` проверяется стандартом
**Объём:** `settings.gradle.kts`, `scripts/build.gradle.kts`, 2 файла бейзлайна, `.editorconfig` (источник + копия), `functions.md`, `classes.md`, `organization.md`, `README.md` кодстайла; ≈ 10 файлов

- [ ] Перед блоком — остановка: тексты «Почему» для KT-FUN-7, KT-FUN-8, KT-CLASS-10 и подтверждение «Почему» KT-TEST-7 (предложен в задаче 12) — у пользователя. Без них задача 12 не выполнима: правило без причины не добавляется (`docs/code-style/kotlin/README.md:46`). Тексты пользователя — в шаги 1–2 задачи 12 этого плана, затем блок.

### Задача 11: Подключение к `scripts/` с бейзлайном и порогом
- Изменить: `settings.gradle.kts:1` — `includeBuild`
- Изменить: `scripts/build.gradle.kts:4` — плагин, порог, исключения точек входа
- Создать: `scripts/detekt-baseline-main.xml`, `scripts/detekt-baseline-test.xml`

- тест: не применим — подключение проверяется самим `check` (шаг 4) и пробным нарушением (раздел «После завершения»); правила и плагин покрыты тестами PR 1–6.
- [ ] 1. Подключить сборку. `settings.gradle.kts`:
  ```kotlin
  pluginManagement {
      // Плагин соглашений стандарта standards.kotlin и свои правила detekt — сборка gradle-plugin/.
      includeBuild("gradle-plugin")
      repositories {
          gradlePluginPortal()
          mavenCentral()
      }
  }
  ```
  `scripts/build.gradle.kts`:
  ```kotlin
  plugins {
      kotlin("jvm") version "2.4.21"
      id("standards.kotlin")
  }
  ```
  и после `dependencies`:
  ```kotlin
  // Порог покрытия — замер scripts/ с округлением вниз (KT-TEST-7); поднимает #107, не снижаем.
  standards {
      coverageMinBound = 0
  }

  // Точки входа: exitProcess завершает JVM, в тестах их не вызвать (KT-TEST-7); проверяются запуском scripts/task и scripts/setup.
  kover {
      reports {
          filters {
              excludes {
                  classes("io.github.nakulov.standards.task.cli.MainKt", "io.github.nakulov.standards.setup.SetupMainKt")
              }
          }
      }
  }
  ```
- [ ] 2. Бейзлайн: `./gradlew :scripts:detektBaselineMain :scripts:detektBaselineTest` → `BUILD SUCCESSFUL`; `ls scripts/detekt-baseline*.xml` — сверить имена файлов с картой (другие имена — обновить карту и «Объём»; `check` после шага 4 не видит бейзлайн — плагин ставит `detekt-baseline.xml`, ⚠️ и вопрос пользователю)
- [ ] 3. Порог: `./gradlew :scripts:koverLog` → `application line coverage: NN.NNNN%`; в `standards { coverageMinBound = … }` — `NN` (вниз до целого)
- [ ] 4. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`; `./gradlew :scripts:test` — зелёный, как до подключения
- [ ] 5. Коммит — подключение, бейзлайн и порог одним коммитом (`check` зелёный)

### Задача 12: Правила стандарта и `.editorconfig`
- Изменить: `docs/code-style/kotlin/functions.md` — KT-FUN-7, KT-FUN-8 в конце файла
- Изменить: `docs/code-style/kotlin/classes.md` — KT-CLASS-10 в конце файла
- Изменить: `docs/testing/kotlin/organization.md:19,38,95` — «Проверка»; KT-TEST-7 в конце файла
- Изменить: `docs/code-style/kotlin/README.md:47` — бейзлайн и порог
- Изменить: `templates/project/editorconfig`, `.editorconfig` — одинаково

- тест: не применим — текст стандарта и `.editorconfig`; проверка формата — копия `.editorconfig` совпадает с источником (`./gradlew :scripts:test` — тесты `setup`).
- [ ] 1. `docs/code-style/kotlin/functions.md`, в конец:
  ````markdown
  ### KT-FUN-7. Функция — не больше 40 строк и не больше двух уровней вложенных блоков
  Почему: <пишет пользователь — «Открыто»>
  Проверка: detekt `LongMethod`, `NestedBlockDepth`

  ```kotlin
  // Запрещено — третий уровень вложенности
  fun paid(orders: List<Order>): List<Order> {
      val result = mutableListOf<Order>()
      for (order in orders) {
          if (order.isPaid) {
              if (order.total > 0) {
                  result.add(order)
              }
          }
      }
      return result
  }

  // Разрешено
  fun paid(orders: List<Order>): List<Order> {
      return orders.filter { order -> order.isPaid && order.total > 0 }
  }
  ```

  ### KT-FUN-8. Не больше двух `return`, ранний выход не считается
  Почему: <пишет пользователь — «Открыто»>
  Проверка: detekt `ReturnCount`

  ```kotlin
  // Разрешено — ранний выход в начале не считается
  fun discount(order: Order?): Int {
      if (order == null) {
          return 0
      }
      if (order.isVip) {
          return VIP_DISCOUNT
      }
      return BASE_DISCOUNT
  }

  // Запрещено — три return посреди логики
  fun discount(order: Order): Int {
      val total = order.total
      if (total > LARGE) {
          return LARGE_DISCOUNT
      }
      val vip = order.isVip
      if (vip) {
          return VIP_DISCOUNT
      }
      return BASE_DISCOUNT
  }
  ```
  ````
  `docs/code-style/kotlin/classes.md`, в конец:
  ````markdown
  ### KT-CLASS-10. Класс — не больше 250 строк
  Почему: <пишет пользователь — «Открыто»>
  Проверка: detekt `LargeClass`

  Класс больше — признак нескольких ответственностей: делите по ним, а не по строкам.
  ````
  `<пишет пользователь>` — заменить текстами, полученными на остановке перед блоком; без них шаг не выполняется.
- [ ] 2. `docs/testing/kotlin/organization.md`:
  - `:19` было `Проверка: ревью` → стало `Проверка: detekt \`SpecDataContent\` (классов-спек нет) + ревью (данные нужны, лежат рядом со спекой)`
  - `:38` было `Проверка: ревью` → стало `Проверка: detekt \`SpecDataContent\` (префикс \`make\`) + ревью (контекст в имени)`
  - `:95` было `Проверка: ревью` → стало `Проверка: detekt \`SpecDataContent\``
  - в конец:
  ````markdown
  ### KT-TEST-7. Непокрытая строка — повод для нового случая теста
  Почему: порог покрытия ловит непроверенный код, только пока его не снижают: снизили ради новой строки — она уходит без теста.
  Проверка: Kover `koverVerify` (порог проекта `standards { coverageMinBound }`) + ревью

  - Порог — замер проекта с округлением вниз; поднимается, не снижается.
  - Из подсчёта исключается только точка входа, которую тест не вызовет (`exitProcess` завершает JVM), — с комментарием-причиной у исключения.
  - HTML-отчёт после тестов — `build/reports/kover/html`: непокрытые строки видны там.

  ```kotlin
  // Разрешено — точка входа исключена с причиной
  kover {
      reports {
          filters {
              excludes {
                  // Точка входа: exitProcess завершает JVM, в тестах её не вызвать.
                  classes("com.example.cli.MainKt")
              }
          }
      }
  }

  // Запрещено — порог снижен под новый код
  standards {
      coverageMinBound = 70 // было 85
  }
  ```
  ````
  Если «Почему» KT-TEST-7 пользователь сформулирует иначе — его текст.
- [ ] 3. `docs/code-style/kotlin/README.md`, после строки 47:
  ```markdown
  - **Бейзлайн detekt** (`detekt-baseline-*.xml` проекта) — нарушения, которые были при подключении проверок. Он только уменьшается: ради нового кода не пополняется, пустой — удаляется. Новое нарушение в файле из бейзлайна всё равно роняет сборку: бейзлайн держит находки (правило + место), а не файлы. Порог покрытия (KT-TEST-7) не снижается.
  ```
- [ ] 4. `templates/project/editorconfig` и `.editorconfig`, в конец секции `[*.{kt,kts}]`:
  ```
  # Тело класса начинается с пустой строки (KT-FMT-3): правило ktlint из стиля ktlint_official требует обратного.
  ktlint_standard_no-empty-first-line-in-class-body = disabled
  ```
  Запустить `./gradlew :scripts:test` — зелёный (копия совпадает с источником); `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] 5. Коммит — правила стандарта и `.editorconfig` вместе

### Приёмка
- [ ] `scripts/` проверяется стандартом: `./gradlew :scripts:check` → `BUILD SUCCESSFUL` с бейзлайном и порогом
- [ ] Крайние случаи: пробное нарушение роняет `check` (раздел «После завершения»); покрытие ниже порога роняет `koverVerify`
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:check` и `./gradlew -p gradle-plugin check`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам
- [ ] Документация: README репозитория — `gradle-plugin/` в карте файлов, если карта есть; строка «Проверка» KT-FUN-4, KT-FUN-6, KT-FMT-7, KT-DOC-6 уже называет правила — сверить, что совпадает с `detekt.yml`

## Открыто
- «Почему» для KT-FUN-7, KT-FUN-8, KT-CLASS-10 — пользователь — остановка перед PR 7
- «Почему» KT-TEST-7 — предложен текст в задаче 12, пользователь подтверждает или пишет свой — остановка перед PR 7
- Формат строк вывода (консольный отчёт detekt 2.0, ошибка конфигурации Gradle, сообщение Kover) — исполнитель по выводу — шаг 4 задач 7, 9, 10 (ожидания в `make…Findings`/`make…Output`, тексты правил и `missingSetup` не меняются)
- Имена файлов бейзлайна для `detektMain`/`detektTest` — исполнитель по выводу — шаг 2 задачи 11
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка: в любом файле `scripts/src/main/kotlin/…` дописать `listOf(1).map { it }` → `./gradlew :scripts:check` падает с `KT-FUN-6. Лямбда с неявным it` и `[NoImplicitIt]`; вернуть файл → `check` зелёный.
- Ручная проверка: временно поднять `coverageMinBound` на 1 выше замера → `./gradlew :scripts:check` падает в `koverVerify` с порогом и фактом; вернуть.
