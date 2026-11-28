# Поставка task готовым jar

**Цель:** запускатель `task` перестаёт компилировать код голым `kotlinc`: скачивает готовый jar этой версии из GitHub Packages, нет — собирает его локально Gradle, дальше запускает jar из кеша.
**Архитектура:** версия jar — хеш исходников, считает один скрипт `scripts/task-version`; по ней Gradle собирает один jar с библиотеками (shadow) и публикует его `maven-publish`, CI — при слиянии в `dev`. Запускатель `scripts/task`: кеш → скачивание `curl` с токеном → локальная сборка `gradlew` → `java -cp`.
**Стек:** bash 3.2, Gradle 9.8.2 (Kotlin DSL), плагин `com.gradleup.shadow` 9.6.1, `maven-publish`, GitHub Actions, GitHub Packages (Maven), Kotest + KotestKit.

---

## Ограничения
- Не трогать: `scripts/src/main/**` — код команд в этой задаче не меняется (решение 6 задачи).
- `task` рабочий после слияния: им же делаются PR остальных подзадач эпика.
- Скрипты — совместимы с bash 3.2 (macOS): без массивов при `set -u`, без `pipefail`.
- Репозиторий публичный: в workflow, задаче и PR — ни секретов, ни имён сторонних библиотек и проектов.
- Спека запускателя проверяет скрипт bash процессом и готовит файлы (заглушки `java` и Gradle, папка пакетов, кеш) — нарушение KT-TEST-6, как и сейчас (`makeOldKotlinPath`): скрипт за интерфейс не спрятать. Спека остаётся помеченной «Вне стандарта»; переезд таких спек — подзадача `repo-checks` эпика.
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: версия jar по хешу исходников, сборка одного jar с библиотеками, публикация в GitHub Packages из CI, новый запускатель `scripts/task` (кеш → скачивание → локальная сборка → `java -cp`).
- Какую проблему решает, что даёт: в рабочем коде можно подключать библиотеки (`implementation` из `gradle/libs.versions.toml`) — без этого остальные подзадачи эпика не начать; у человека нужна Java, а не Kotlin.
- Как встраивается: `scripts/setup`, `scripts/task-wrapper` и `hooks/session-start` вызывают `scripts/task` как раньше; точка входа — по-прежнему `TASK_MAIN`.
- Критерии цели:
  - CI публикует jar при слиянии в `dev` под версией = вывод `scripts/task-version`;
  - `task --help` в проекте с плагином скачивает jar, без компиляции;
  - `task --help` на ветке с изменённым кодом собирает jar локально Gradle;
  - `task` запускается без `kotlinc`, нужна Java ≥ 21;
  - токен для скачивания запускатель берёт сам: `gpr.key` из `~/.gradle/gradle.properties`, иначе `gh auth token`.

## Контекст
- Задействованные файлы:
  - `scripts/task` — запускатель: перенаправление `-C` (строки 12–21), проверка `kotlinc` (23–26), ключ кеша (28–32), компиляция (34–53), запуск `kotlin -cp` (55–57);
  - `scripts/setup` — запускает `scripts/task` с `TASK_MAIN=…SetupMainKt`;
  - `scripts/build.gradle.kts` — ограничение языка 2.2 ради `kotlinc` (9–16), системные свойства тестов (39–54);
  - `gradle/libs.versions.toml` — каталог версий;
  - `settings.gradle.kts:19-25` — репозиторий GitHub Packages с `gpr.user`/`gpr.key` (`.get()` — токен обязателен для любого запуска Gradle);
  - `gradle/gradle-daemon-jvm.properties:12` — Gradle собирает на JDK 21 (Amazon Corretto), jar — байткод 21;
  - `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpec.kt`, `TaskLauncherSpecData.kt` — спека запускателей; `LauncherRun` используют `TaskWrapperScriptSpec*` и `SessionStartHookSpecData.kt` — его форма не меняется;
  - `scripts/detekt-baseline-test.xml:453-460` — находки `TaskLauncherSpecData.kt`, часть уходит вместе с кодом;
  - `README.md:39-52` — раздел «Подключение».
- Паттерны: заглушки исполняемых файлов в спеке (`makeOldKotlinPath`, `TaskLauncherSpecData.kt:56-63`); временные папки — `makeTempDir` (`plugin/SessionStartHookSpecData.kt:8`); системные свойства путей — из `tasks.test` (`build.gradle.kts:41-45`).
- Зависимости: плагин `com.gradleup.shadow` 9.6.1 (Plugin Portal, 22.07.2026; требует Gradle ≥ 9.2), `maven-publish` из Gradle; на машине — `java` ≥ 21, `curl`, `shasum`.

## Структура файлов
- Создать: `scripts/task-version` — версия jar: хеш исходников рабочего кода и его сборки
- Изменить: `scripts/build.gradle.kts` — версия из `task-version`, shadow-jar `task.jar`, публикация `gitHubPackages` и `testPackages`, свойства тестов; убрать ограничение языка 2.2
- Изменить: `gradle/libs.versions.toml` — плагин shadow
- Изменить: `scripts/task` — кеш → скачивание → локальная сборка → `java -cp`; проверка Java вместо Kotlin
- Изменить: `scripts/setup:2` — комментарий: та же проверка Java и тот же jar
- Создать: `.github/workflows/publish-task.yml` — публикация jar при слиянии в `dev`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpec.kt` — случаи версии, скачивания, кеша, локальной сборки, Java
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/cli/TaskLauncherSpecData.kt` — окружения запуска (PATH, пакеты, Gradle, кеш), ожидаемый вывод
- Изменить: `scripts/detekt-baseline-test.xml:453-460` — убрать находки удалённого кода
- Изменить: `README.md:39-52` — что нужно на машине: Java 21 и токен `read:packages`

≈ 10 файлов → один PR.

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: из груминга — Gradle-jar с публикацией в GitHub Packages и локальной сборкой как запасным путём (груминг эпика, п. 0).
- Решения:
  - хеш считает один скрипт `scripts/task-version`, его вызывают и Gradle (версия публикации), и запускатель — два расчёта разошлись бы, и запускатель искал бы не тот jar;
  - в хеш входят `scripts/src/main/**` (без скрытых файлов — `.DS_Store` есть только на macOS), `scripts/build.gradle.kts`, `gradle/libs.versions.toml`: смена библиотеки или её версии меняет jar; файлы сортируются `LC_ALL=C` — порядок одинаков на macOS и Linux CI;
  - один jar со всеми библиотеками (shadow, имя `task.jar` без версии) — запускатель запускает его `java -cp` без разрешения зависимостей; `TASK_MAIN` выбирает точку входа, как раньше;
  - Java проверяется только при отсутствии jar в кеше, как раньше Kotlin, — обычный запуск не тратит лишний старт JVM; минимум 21 — байткод сборки (`gradle-daemon-jvm.properties:12`);
  - токен передаётся `curl` через `-K -` (stdin), а не аргументом — не виден в списке процессов;
  - не скачался jar (нет версии, нет доступа, нет сети) — одна строка в stderr с подсказкой `gh auth refresh -s read:packages` и локальная сборка: путь разработчика до слияния;
  - адрес пакетов (`TASK_PACKAGES_URL`) и Gradle (`TASK_GRADLE`) переопределяются окружением — спека скачивает из папки сборки `file://` и подменяет Gradle заглушкой: вложенный Gradle внутри `:scripts:test` упёрся бы в блокировку той же сборки;
  - CI только публикует, тесты не запускает: проверки идут до PR (`task pr`), запуск тестов в CI — вне задачи;
  - CI пропускает публикацию, если версия уже есть (повтор хеша после отката) — GitHub Packages не принимает версию повторно;
  - ограничение языка и API 2.2 в `build.gradle.kts` уходит: оно было только для `kotlinc` на машине человека.
- Как вписывается в систему: `scripts/task` остаётся единственной точкой запуска; `TASK_PLUGIN_ROOT`, перенаправление `-C` и `TASK_MAIN` не меняются.
- Обратная совместимость: не нужна — jar в кеше `~/.cache/engineering-standards/task-<ключ>.jar` от старого запускателя просто не используется (другой ключ).
- Запасной: только локальная сборка Gradle без скачивания — переходим, если публикация в GitHub Packages из CI не заработает (доступ `GITHUB_TOKEN` к пакетам).
- Отклонено:
  - только локальная сборка Gradle без публикации — первый запуск после обновления плагина собирает Gradle вместо скачивания;
  - `kotlinc` + скачивание jar'ов запускателем — транзитивные зависимости вручную, нет KSP, версии мимо `gradle/libs.versions.toml`, менеджер зависимостей на bash;
  - без зависимостей — свой JSON-парсер остаётся, модули упираются в одну папку `kotlinc`;
  - jar в git — двоичный файл в PR, конфликты, рассинхрон с исходниками;
  - GitHub Releases вместо Packages — выигрывает только у машин без токена, таких нет.

## Технические детали
- Структуры данных и изменения: jar — Maven-артефакт `io.github.nakulov.standards:task:<версия>`, файл `task-<версия>.jar` без классификатора; локально — `scripts/build/libs/task.jar`; кеш — `${XDG_CACHE_HOME:-$HOME/.cache}/engineering-standards/task-<версия>.jar`.
- Параметры и форматы:
  - версия — 16 шестнадцатеричных символов, вывод `scripts/task-version` с переводом строки;
  - адрес jar — `${TASK_PACKAGES_URL:-https://maven.pkg.github.com/Nakulov/engineering-standards}/io/github/nakulov/standards/task/<версия>/task-<версия>.jar`;
  - Gradle — `${TASK_GRADLE:-<корень>/gradlew} -p <корень> --quiet :scripts:shadowJar`, вывод — в stderr;
  - сообщения запускателя (stderr, код 2): «Нет Java — установите JDK 21: brew install openjdk@21»; «Java 17, нужна ≥ 21 — установите JDK 21: brew install openjdk@21»; «task: готовый jar не скачан — собираю локально; нет доступа к GitHub Packages — gh auth refresh -s read:packages» (код не 2 — продолжает); «сбой сборки task — подробности выше».
- Поток обработки: `-C` в другую копию → `exec` её `task`; версия → jar в кеше есть → `java -cp`; нет → проверка Java → токен → `curl` во временный файл → не скачался → сообщение, Gradle, копия `task.jar` → `mv` в кеш (атомарно) → `java -cp`.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: task запускается готовым jar из GitHub Packages
  - [x] Задача 1: Версия и сборка jar
  - [x] Задача 2: Запускатель скачивает или собирает jar
  - [x] Задача 3: Публикация jar из CI
  - [x] Задача 4: Требования к машине в README
  - [x] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Доступ `GITHUB_TOKEN` этого репозитория к пакету KotestKit (настройки пакета, «Manage Actions access») — до первого запуска workflow.
- Первый запуск workflow после слияния — jar в Packages репозитория.
- Ручные проверки «Критериев готовности» задачи — раздел «После завершения».

## PR 1: task запускается готовым jar из GitHub Packages
**Объём:** `scripts/task-version`, `scripts/task`, `scripts/setup`, `scripts/build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/publish-task.yml`, `TaskLauncherSpec.kt`, `TaskLauncherSpecData.kt`, `scripts/detekt-baseline-test.xml`, `README.md`; ≈ 10 файлов

### Задача 1: Версия и сборка jar — Gradle собирает и публикует jar под версией из `task-version`
- Создать: `scripts/task-version` — версия jar
- Изменить: `scripts/build.gradle.kts` — версия, shadow-jar, публикация, свойства тестов `task.version` и `task.versionScript`, входы тестов; убрать ограничение 2.2
- Изменить: `gradle/libs.versions.toml` — плагин shadow
- Изменить: `TaskLauncherSpec.kt`, `TaskLauncherSpecData.kt` — случай «версия скрипта = версия Gradle»

- [x] 1. Падающий тест: вывод `scripts/task-version` совпадает с версией, под которой Gradle собирает jar.
  В `TaskLauncherSpec.kt` — первым тестом в теле спеки:
  ```kotlin
        shouldReturnTest(makeVersionRun(), options = TestOptions(name = "task-version: версия сборки Gradle")) {
            runLauncher(path = makeBarePath(), args = emptyList(), launcher = makeVersionScript())
        }
  ```
  В `TaskLauncherSpecData.kt` — после `makeOldKotlinRun()`:
  ```kotlin
  /** Вывод `scripts/task-version`: версия, под которой Gradle собирает и публикует jar. */
  fun makeVersionRun(): LauncherRun {
      return LauncherRun(code = 0, out = makeTaskVersion() + "\n", err = "")
  }

  fun makeVersionScript(): String {
      return System.getProperty(/* key = */ "task.versionScript")
  }

  private fun makeTaskVersion(): String {
      return System.getProperty(/* key = */ "task.version")
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*TaskLauncherSpec*'` — падает: `NullPointerException` в `makeTaskVersion` — свойства `task.version` нет.
- [x] 3. Минимальный код:
  `scripts/task-version` (новый; затем `chmod +x scripts/task-version`, проверка `ls -l scripts/task-version` → `-rwxr-xr-x`; `git` сохраняет бит исполнения — `git diff --summary` после `git add`: `create mode 100755`):
  ```bash
  #!/bin/bash
  # Версия jar task: хеш исходников рабочего кода и его сборки. Под ней Gradle собирает и публикует jar, по ней
  # запускатель ищет его в кеше и GitHub Packages — расчёт один, иначе версии CI и запускателя разойдутся.
  # Сортировка LC_ALL=C — один порядок на macOS и Linux CI; скрытые файлы (.DS_Store) не в git — не в хеше.
  # Совместим с bash 3.2 (macOS).
  set -eu

  root=$(cd "$(dirname "$0")/.." && pwd -P)
  cd "$root"
  {
    find scripts/src/main -type f ! -name '.*' | LC_ALL=C sort | xargs shasum
    shasum scripts/build.gradle.kts gradle/libs.versions.toml
  } | shasum | cut -c1-16
  ```
  `gradle/libs.versions.toml` — в `[versions]` после `mockk`:
  ```toml
  shadow = "9.6.1"
  ```
  в `[plugins]` после `kotlin-jvm`:
  ```toml
  shadow = { id = "com.gradleup.shadow", version.ref = "shadow" }
  ```
  `scripts/build.gradle.kts` — целиком:
  ```kotlin
  plugins {
      alias(libs.plugins.kotlin.jvm)
      alias(libs.plugins.shadow)
      id("standards.kotlin")
      `maven-publish`
  }

  val versionScript = file(/* path = */ "task-version")
  val testPackages = layout.buildDirectory.dir(/* path = */ "test-packages")

  // Версия jar — вывод scripts/task-version: под ней CI публикует jar, по ней запускатель ищет его в кеше и GitHub
  // Packages.
  group = "io.github.nakulov.standards"
  version = providers.exec { commandLine(versionScript.absolutePath) }
      .standardOutput
      .asText
      .get()
      .trim()

  dependencies {
      testImplementation(libs.kotestkit)
      testImplementation(libs.mockk)
  }

  // Один jar с библиотеками рабочего кода: запускатель запускает его `java -cp` без Gradle. Имя без версии — по нему
  // запускатель забирает локальную сборку.
  tasks.shadowJar {
      archiveBaseName = "task"
      archiveClassifier = ""
      archiveVersion = ""
  }

  // gitHubPackages — публикация из CI (.github/workflows/publish-task.yml); testPackages — тот же jar в папке сборки,
  // из неё скачивает TaskLauncherSpec.
  publishing {
      publications {
          create<MavenPublication>(/* name = */ "task") {
              artifactId = "task"
              from(components["shadow"])
          }
      }
      repositories {
          maven {
              name = "gitHubPackages"
              url = uri(/* path = */ "https://maven.pkg.github.com/Nakulov/engineering-standards")
              credentials {
                  username = providers.gradleProperty(/* propertyName = */ "gpr.user").get()
                  password = providers.gradleProperty(/* propertyName = */ "gpr.key").get()
              }
          }
          maven {
              name = "testPackages"
              url = uri(testPackages)
          }
      }
  }

  // Порог покрытия — замер scripts/ с округлением вниз (KT-TEST-7); поднимает #107, не снижаем.
  standards {
      coverageMinBound = 97
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

  tasks.test {
      useJUnitPlatform()
      systemProperty("task.launcher", file("task").absolutePath)
      systemProperty("task.setupLauncher", file("setup").absolutePath)
      systemProperty("task.versionScript", versionScript.absolutePath)
      systemProperty("task.version", version.toString())
      systemProperty("task.repoRoot", rootProject.projectDir.absolutePath)
      systemProperty("task.prTemplate", rootProject.file("templates/github/pull_request_template.md").absolutePath)
      systemProperty("task.cache", layout.buildDirectory.dir("test-cache").get().asFile.absolutePath)
      // Спеки читают эти файлы через task.repoRoot: без входов правка только в них оставляет тест UP-TO-DATE.
      // Запускатели — входы спеки запускателей: их правка тоже должна перезапускать тест.
      inputs.files(
          rootProject.files(
              "skills", "agents", "rules", "docs", "templates", ".github",
              "hooks/session-start", "scripts/task-wrapper", "scripts/task", "scripts/setup", "scripts/task-version",
          ),
      )
          .withPropertyName(/* propertyName = */ "repoFiles")
  }
  ```
  Ограничение `languageVersion`/`apiVersion` 2.2 и импорты `KotlinVersion`, `KotlinCompile` убраны: старый запускатель компилирует `kotlinc` с флагами `-language-version 2.2` сам, до задачи 2 он работает как раньше.
- [x] 4. Запустить `./gradlew :scripts:test --tests '*TaskLauncherSpec*'` — зелёный (`BUILD SUCCESSFUL`). Дополнительно:
  - `./gradlew :scripts:shadowJar` — `BUILD SUCCESSFUL`; `unzip -l scripts/build/libs/task.jar kotlin/Unit.class` — строка `kotlin/Unit.class` (стандартная библиотека внутри jar);
  - `./gradlew :scripts:publishTaskPublicationToTestPackagesRepository` — `BUILD SUCCESSFUL`; `ls scripts/build/test-packages/io/github/nakulov/standards/task/` — одна папка, имя = вывод `scripts/task-version`, в ней `task-<версия>.jar`.
- [x] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`.
- [x] 6. Коммит — тест и код вместе: «Версия jar task — хеш исходников, Gradle собирает и публикует jar».

### Задача 2: Запускатель скачивает или собирает jar
- Изменить: `scripts/task` — кеш → скачивание → локальная сборка → `java -cp`
- Изменить: `scripts/setup:2` — комментарий
- Изменить: `scripts/build.gradle.kts` (`tasks.test`) — свойства `task.packages`, `task.jar`, зависимость от публикации в `testPackages`
- Изменить: `TaskLauncherSpec.kt`, `TaskLauncherSpecData.kt` — окружения запуска и случаи
- Изменить: `scripts/detekt-baseline-test.xml:453-460` — убрать находки удалённого кода

- [x] 1. Падающий тест: скачивание, кеш, локальная сборка, сбой сборки, нет Java, Java 17.
  `scripts/build.gradle.kts` — в `tasks.test` после `useJUnitPlatform()`:
  ```kotlin
      // Спека запускателя скачивает jar из testPackages и забирает локальную сборку build/libs/task.jar.
      dependsOn("publishTaskPublicationToTestPackagesRepository")
      systemProperty("task.packages", testPackages.get().asFile.absolutePath)
      systemProperty("task.jar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
  ```
  `TaskLauncherSpec.kt` — целиком:
  ```kotlin
  package io.github.nakulov.standards.task.cli

  import io.github.nakulov.kotestkit.TestOptions
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.github.nakulov.standards.plugin.MISSING_DIRECTORY
  import io.github.nakulov.standards.plugin.STANDARD_COPY
  import io.github.nakulov.standards.plugin.makeCopyRun
  import io.github.nakulov.standards.plugin.makeGitProject
  import io.github.nakulov.standards.plugin.makeStandardCopy
  import io.github.nakulov.standards.plugin.makeStandardRepo
  import io.kotest.core.spec.style.BehaviorSpec
  import kotlin.time.Duration.Companion.seconds

  // Вне стандарта: спека проверяет не вызов класса, а запускатели `scripts/task`, `scripts/setup` и
  // `scripts/task-version` — запускает их процессом с разным окружением: PATH с заглушкой java, папка пакетов, заглушка
  // Gradle, кеш. Окружение готовится файлами (KT-TEST-6): скрипт bash за интерфейс не спрятать. Запуск и окружения — в
  // TaskLauncherSpecData.
  class TaskLauncherSpec :
      BehaviorSpec({
          shouldReturnTest(makeVersionRun(), options = TestOptions(name = "task-version: версия сборки Gradle")) {
              runLauncher(environment = makeBareEnvironment(), args = emptyList(), launcher = makeVersionScript())
          }

          shouldReturnTest(makeUnknownCommandRun(), options = TestOptions(name = "task: jar из GitHub Packages")) {
              runLauncher(environment = makeDownloadEnvironment(), args = listOf(UNKNOWN_COMMAND))
          }

          shouldReturnTest(makeUnknownCommandRun(), options = TestOptions(name = "task: jar из кеша")) {
              runLauncher(environment = makeCachedEnvironment(), args = listOf(UNKNOWN_COMMAND))
          }

          shouldReturnTest(makeLocalBuildRun(), options = TestOptions(name = "task: jar не скачан — локальная сборка")) {
              runLauncher(environment = makeLocalBuildEnvironment(), args = listOf(UNKNOWN_COMMAND))
          }

          shouldReturnTest(makeFailedBuildRun(), options = TestOptions(name = "task: jar не скачан, сборка упала")) {
              runLauncher(environment = makeFailedBuildEnvironment(), args = listOf(UNKNOWN_COMMAND))
          }

          shouldReturnTest(
              makeSetupHelpRun(),
              options = TestOptions(name = "setup --help: тот же jar, другая точка входа"),
          ) {
              runLauncher(
                  environment = makeDownloadEnvironment(),
                  args = listOf("--help"),
                  launcher = makeSetupLauncher(),
              )
          }

          withGivens(
              nameFn = { given -> "без перенаправления, нет java: ${given.arg1}" },
              case(arg1 = listOf("--help")),
              case(arg1 = listOf("-C", makeRepoRoot(), "--help")),
              case(arg1 = listOf("-C", makeGitProject().path, "--help")),
              case(arg1 = listOf("-C", MISSING_DIRECTORY, "--help")),
          ) { args ->
              shouldReturnTest(makeNoJavaRun()) {
                  runLauncher(environment = makeNoJavaEnvironment(), args = args, timeout = 10.seconds)
              }
          }

          shouldReturnTest(makeCopyRun(), options = TestOptions(name = "-C в копию стандарта — task копии")) {
              val repo = makeStandardRepo()
              makeStandardCopy(repo = repo)
              runLauncher(
                  environment = makeBareEnvironment(),
                  args = listOf("-C", STANDARD_COPY, "--help"),
                  directory = repo,
              )
          }

          shouldReturnTest(makeOldJavaRun(), options = TestOptions(name = "java 17")) {
              runLauncher(environment = makeOldJavaEnvironment(), args = listOf("--help"))
          }
      })
  ```
  `TaskLauncherSpecData.kt` — целиком:
  ```kotlin
  package io.github.nakulov.standards.task.cli

  import io.github.nakulov.standards.plugin.makeTempDir
  import java.io.File
  import java.util.concurrent.TimeUnit
  import kotlin.time.Duration
  import kotlin.time.Duration.Companion.minutes

  const val UNKNOWN_COMMAND = "nope"
  const val DOWNLOAD_MISS = "task: готовый jar не скачан — собираю локально; нет доступа к GitHub Packages — " +
      "gh auth refresh -s read:packages\n"
  const val UNKNOWN_COMMAND_ERROR = "неизвестная команда nope — см. task --help\n"
  const val CACHE_PREFIX = "cache"

  /** Результат запуска `scripts/task`: код выхода, stdout, stderr. */
  data class LauncherRun(
      val code: Int,
      val out: String,
      val err: String,
  )

  /** Ответ `Task` на неизвестную команду: не зависит от списка команд и их опций. */
  fun makeUnknownCommandRun(): LauncherRun {
      return LauncherRun(code = 2, out = "", err = UNKNOWN_COMMAND_ERROR)
  }

  /** jar не скачан, заглушка Gradle «собрала» его: запускатель забрал `build/libs/task.jar` сборки тестов. */
  fun makeLocalBuildRun(): LauncherRun {
      return LauncherRun(code = 2, out = "", err = DOWNLOAD_MISS + UNKNOWN_COMMAND_ERROR)
  }

  fun makeFailedBuildRun(): LauncherRun {
      return LauncherRun(code = 2, out = "", err = DOWNLOAD_MISS + "сбой сборки task — подробности выше\n")
  }

  fun makeSetupHelpRun(): LauncherRun {
      return LauncherRun(
          code = 0,
          out = "setup — настройка репозитория текущей папки по стандарту. Запуск:\n\n" +
              "  setup — применить недостающее; удаляющие шаги только печатаются\n" +
              "  setup --confirm — применить, включая удаляющие шаги\n" +
              "  setup --check — только сверить: 0 — соответствует, 1 — отклонения\n",
          err = "",
      )
  }

  fun makeNoJavaRun(): LauncherRun {
      return LauncherRun(code = 2, out = "", err = "Нет Java — установите JDK 21: brew install openjdk@21\n")
  }

  fun makeOldJavaRun(): LauncherRun {
      return LauncherRun(
          code = 2,
          out = "",
          err = "Java 17, нужна ≥ 21 — установите JDK 21: brew install openjdk@21\n",
      )
  }

  /** Вывод `scripts/task-version`: версия, под которой Gradle собирает и публикует jar. */
  fun makeVersionRun(): LauncherRun {
      return LauncherRun(code = 0, out = makeTaskVersion() + "\n", err = "")
  }

  fun makeVersionScript(): String {
      return System.getProperty(/* key = */ "task.versionScript")
  }

  fun makeSetupLauncher(): String {
      return System.getProperty("task.setupLauncher")
  }

  /** Корень репозитория, из которого запущены тесты: `-C` в него — свой корень, без перенаправления. */
  fun makeRepoRoot(): String {
      return System.getProperty(/* key = */ "task.repoRoot")
  }

  /** jar этой версии опубликован в папке пакетов сборки; Gradle падает — запуск идёт только со скачанного jar. */
  fun makeDownloadEnvironment(): Map<String, String> {
      return makeEnvironment(path = makeSystemPath(), packages = makePublishedPackages(), gradle = makeGradle(code = 1))
  }

  /** jar этой версии уже в кеше; пакетов нет, Gradle падает — запуск идёт только из кеша. */
  fun makeCachedEnvironment(): Map<String, String> {
      val cache = makeTempDir(prefix = CACHE_PREFIX)
      val jar = File(/* parent = */ cache, /* child = */ "engineering-standards/task-${makeTaskVersion()}.jar")
      jar.parentFile.mkdirs()
      File(System.getProperty(/* key = */ "task.jar")).copyTo(target = jar)
      return makeEnvironment(
          path = makeSystemPath(),
          packages = makeEmptyPackages(),
          gradle = makeGradle(code = 1),
          cache = cache,
      )
  }

  fun makeLocalBuildEnvironment(): Map<String, String> {
      return makeEnvironment(path = makeSystemPath(), packages = makeEmptyPackages(), gradle = makeGradle(code = 0))
  }

  fun makeFailedBuildEnvironment(): Map<String, String> {
      return makeEnvironment(path = makeSystemPath(), packages = makeEmptyPackages(), gradle = makeGradle(code = 1))
  }

  /** Первая `java` в PATH не запускается — как заглушка macOS `/usr/bin/java` без установленной JDK. */
  fun makeNoJavaEnvironment(): Map<String, String> {
      return makeEnvironment(
          path = makeJavaPath(script = "exit 1"),
          packages = makeEmptyPackages(),
          gradle = makeGradle(code = 1),
      )
  }

  fun makeOldJavaEnvironment(): Map<String, String> {
      return makeEnvironment(
          path = makeJavaPath(script = "echo 'openjdk version \"17.0.2\" 2022-01-18' >&2"),
          packages = makeEmptyPackages(),
          gradle = makeGradle(code = 1),
      )
  }

  /** Только системные утилиты: для скриптов, которым не нужна Java. */
  fun makeBareEnvironment(): Map<String, String> {
      return makeEnvironment(path = makeBarePath(), packages = makeEmptyPackages(), gradle = makeGradle(code = 1))
  }

  /** PATH без Kotlin и Java: только системные утилиты. */
  fun makeBarePath(): String {
      return "/usr/bin:/bin"
  }

  /**
   * Запуск `scripts/task` с таймаутом: вывод — в файлы, иначе сломанное перенаправление `-C` (копия запускает себя
   * бесконечно) повесит чтение вывода, и тест зависнет, а не упадёт.
   */
  fun runLauncher(
      environment: Map<String, String>,
      args: List<String>,
      launcher: String = System.getProperty("task.launcher"),
      directory: File? = null,
      timeout: Duration = 3.minutes,
  ): LauncherRun {
      val logs = makeTempDir(prefix = "launcher")
      val out = File(/* parent = */ logs, /* child = */ "out")
      val err = File(/* parent = */ logs, /* child = */ "err")
      val builder = ProcessBuilder(listOf(launcher) + args).directory(directory)
          .redirectOutput(out)
          .redirectError(err)
      builder.environment().putAll(environment)
      val process = builder.start()
      val isFinished = process.waitFor(timeout.inWholeSeconds, TimeUnit.SECONDS)
      if (!isFinished) {
          process.destroyForcibly()
      }
      check(isFinished) { "task ${args.joinToString(separator = " ")} не завершился за $timeout" }
      return LauncherRun(code = process.exitValue(), out = out.readText(), err = err.readText())
  }

  /** Окружение запуска: свой пустой кеш на каждый случай — jar прошлого случая не подменяет скачивание или сборку. */
  private fun makeEnvironment(
      path: String,
      packages: String,
      gradle: String,
      cache: File = makeTempDir(prefix = CACHE_PREFIX),
  ): Map<String, String> {
      return mapOf(
          "PATH" to path,
          "XDG_CACHE_HOME" to cache.absolutePath,
          "TASK_PACKAGES_URL" to "file://$packages",
          "TASK_GRADLE" to gradle,
      )
  }

  /** java тестовой JVM — первой: запускатель запускает jar той же Java, на которой его собрал Gradle. */
  private fun makeSystemPath(): String {
      return "${System.getProperty(/* key = */ "java.home")}/bin:${System.getenv(/* name = */ "PATH")}"
  }

  /** PATH, где первая `java` — заглушка с телом [script]. */
  private fun makeJavaPath(script: String): String {
      val java = makeStub(file = File(makeTempDir(prefix = "stub"), /* child = */ "java"), script = script)
      return "${java.parentFile.absolutePath}:${makeSystemPath()}"
  }

  /** Заглушка Gradle с кодом выхода [code]: код 0 — запускатель забирает `build/libs/task.jar`, собранный до тестов. */
  private fun makeGradle(code: Int): String {
      val gradle = makeStub(file = File(makeTempDir(prefix = "gradle"), /* child = */ "gradlew"), script = "exit $code")
      return gradle.absolutePath
  }

  private fun makeStub(file: File, script: String): File {
      file.writeText(text = "#!/bin/sh\n$script\n")
      file.setExecutable(/* executable = */ true)
      return file
  }

  private fun makePublishedPackages(): String {
      return System.getProperty(/* key = */ "task.packages")
  }

  private fun makeEmptyPackages(): String {
      return makeTempDir(prefix = "packages").absolutePath
  }

  private fun makeTaskVersion(): String {
      return System.getProperty(/* key = */ "task.version")
  }
  ```
  Удалены: `makeNoKotlinRun`, `makeOldKotlinRun`, `makeOldKotlinPath`, публичный `makeSystemPath` (теперь закрытый, вне спеки не используется — `grep -rn makeSystemPath scripts/src/test` до правки: только `TaskLauncherSpec*`). `LauncherRun` не меняется — его используют `TaskWrapperScriptSpec*` и `SessionStartHookSpecData.kt`. `makeBarePath` остаётся публичным с прежней сигнатурой — находка бейзлайна `FunctionOnlyReturningConstant:…fun makeBarePath: String` остаётся на месте.
- [x] 2. Запустить `./gradlew :scripts:test --tests '*TaskLauncherSpec*'` — красный прогон пропущен в попытке 1, выполнен отдельно после задачи 4 (`scripts/task` и `scripts/setup` временно взяты из `51303e2`): 12 тестов, 7 упало, 5 прошло — как ниже. Старый запускатель игнорирует `TASK_PACKAGES_URL`, `TASK_GRADLE` и кеш и компилирует `kotlinc`:
  - падают: «jar не скачан — локальная сборка», «сборка упала» — в stderr нет `DOWNLOAD_MISS`; четыре «нет java», «java 17» — вместо сообщения о Java справка или вывод `kotlinc`;
  - проходят и на старом запускателе (`kotlinc` на машине есть): «task-version», «jar из GitHub Packages», «jar из кеша», «setup --help», «-C в копию стандарта» — вывод тот же; скачивание и кеш они проверяют только на шаге 4, после нового кода;
  - упал другой случай или по другой причине (например, `NullPointerException` в `TaskLauncherSpecData`) — разобраться до кода.
- [x] 3. Минимальный код:
  `scripts/task` — целиком:
  ```bash
  #!/bin/bash
  # Запускатель task: готовый jar этой версии из кеша; нет — скачивает его из GitHub Packages, не скачался — собирает
  # локально Gradle (ветка разработчика до слияния). Версия — scripts/task-version, её же публикует CI.
  # Логики процесса здесь нет — она в src/main/kotlin. Совместим с bash 3.2 (macOS).
  # Точка входа — TASK_MAIN (по умолчанию task); scripts/setup запускает через него setup.
  # TASK_PACKAGES_URL и TASK_GRADLE подменяет спека запускателя: пакеты — папка file://, Gradle — заглушка.
  set -eu

  min_java=21 # Gradle собирает jar на JDK 21 (gradle/gradle-daemon-jvm.properties): на старой Java он не запустится

  dir=$(cd "$(dirname "$0")" && pwd -P)
  root=$(cd "$dir/.." && pwd)

  # Копия стандарта по `-C` (разбор — как в WorkingDirectory.kt): в сессии Claude `task` — scripts/task основной папки
  # из PATH хука, мимо обёртки ~/.local/bin/task. Корень папки `-C` — другая копия стандарта → её task; совпал со своим —
  # без перенаправления, иначе копия запускала бы себя бесконечно. Пути физические, как у git (`pwd -P`).
  if [ "${1:-}" = "-C" ] && [ -d "${2:-}" ]; then
    top=$(git -C "$2" rev-parse --show-toplevel 2>/dev/null || true)
    if [ -n "$top" ] && [ "$(cd "$top/scripts" 2>/dev/null && pwd -P)" != "$dir" ] &&
      grep -qs '"name": "engineering-standards"' "$top/.claude-plugin/plugin.json"; then
      exec "$top/scripts/task" "$@"
    fi
  fi

  version=$("$dir/task-version")
  cache="${XDG_CACHE_HOME:-$HOME/.cache}/engineering-standards"
  jar="$cache/task-$version.jar"

  if [ ! -f "$jar" ]; then
    # Нет java или это заглушка macOS без JDK — версии в выводе нет.
    java_version=$(java -version 2>&1 | sed -n '1s/.*version "\([0-9][0-9]*\).*/\1/p' || true)
    if [ -z "$java_version" ]; then
      echo "Нет Java — установите JDK $min_java: brew install openjdk@$min_java" >&2
      exit 2
    fi
    if [ "$java_version" -lt "$min_java" ]; then
      echo "Java $java_version, нужна ≥ $min_java — установите JDK $min_java: brew install openjdk@$min_java" >&2
      exit 2
    fi
    mkdir -p "$cache"
    tmp="$cache/tmp-$$.jar"
    packages="${TASK_PACKAGES_URL:-https://maven.pkg.github.com/Nakulov/engineering-standards}"
    # Токен — как у Gradle (gpr.key), иначе gh; передаётся curl через stdin, а не аргументом: не виден в списке процессов.
    token=$(sed -n 's/^gpr\.key=//p' "$HOME/.gradle/gradle.properties" 2>/dev/null || true)
    if [ -z "$token" ]; then
      token=$(gh auth token 2>/dev/null || true)
    fi
    if ! printf 'user = "token:%s"\n' "$token" |
      curl -K - -fsSL -o "$tmp" "$packages/io/github/nakulov/standards/task/$version/task-$version.jar" 2>/dev/null; then
      echo "task: готовый jar не скачан — собираю локально; нет доступа к GitHub Packages — gh auth refresh -s read:packages" >&2
      if ! "${TASK_GRADLE:-$root/gradlew}" -p "$root" --quiet :scripts:shadowJar >&2 ||
        ! cp "$root/scripts/build/libs/task.jar" "$tmp"; then
        rm -f "$tmp"
        echo "сбой сборки task — подробности выше" >&2
        exit 2
      fi
    fi
    mv -f "$tmp" "$jar" # атомарно: параллельный запуск не увидит недописанный jar
  fi

  TASK_PLUGIN_ROOT=$root
  export TASK_PLUGIN_ROOT # шаблоны плагина (этап) — от корня плагина
  exec java -cp "$jar" "${TASK_MAIN:-io.github.nakulov.standards.task.cli.MainKt}" "$@"
  ```
  `scripts/setup:2` — строка комментария:
  ```bash
  # Запускатель setup (#8): тот же jar и та же проверка Java, что у task, другая точка входа.
  ```
  `scripts/detekt-baseline-test.xml` — удалить строки находок удалённого кода (остаются `"task.launcher"` и `"task.setupLauncher"` — их код не меняется):
  ```xml
      <ID>NamedLiteralArgument:TaskLauncherSpecData.kt:"#!/bin/sh\necho 'info: kotlinc-jvm 2.1.20 (JRE 17.0.21)' &gt;&amp;2\n"</ID>
      <ID>NamedLiteralArgument:TaskLauncherSpecData.kt:"PATH"</ID>
      <ID>NamedLiteralArgument:TaskLauncherSpecData.kt:"kotlinc"</ID>
      <ID>NamedLiteralArgument:TaskLauncherSpecData.kt:"old-kotlin"</ID>
      <ID>NamedLiteralArgument:TaskLauncherSpecData.kt:"task.cache"</ID>
      <ID>NamedLiteralArgument:TaskLauncherSpecData.kt:true</ID>
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*TaskLauncherSpec*'` — зелёный (`BUILD SUCCESSFUL`). Затем `./gradlew :scripts:test --tests '*TaskWrapperScriptSpec*' --tests '*SessionStartHookSpec*'` — зелёный (`LauncherRun` не изменился).
- [x] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`; упал `NamedLiteralArgument` или `SpecDataContent` по новому коду `TaskLauncherSpecData.kt` — исправить код, бейзлайн не пополнять.
- [x] 6. Коммит — тест и код вместе: «Запускатель task скачивает готовый jar или собирает его Gradle».

### Задача 3: Публикация jar из CI
- Создать: `.github/workflows/publish-task.yml` — публикация при слиянии в `dev`

- [x] 1. Тест: не применим — workflow запускает GitHub после слияния; проверка — первый запуск (приёмка, «После завершения»). Проверки языка для YAML в банке нет.
- [x] 2. Код — `.github/workflows/publish-task.yml`:
  ```yaml
  # Публикация jar task в GitHub Packages при слиянии в dev. Версия — scripts/task-version (хеш исходников), её же
  # ищет запускатель scripts/task. Тесты здесь не запускаются: проверки идут до PR.
  name: Публикация task

  on:
    push:
      branches: [dev]
      paths:
        - scripts/src/main/**
        - scripts/build.gradle.kts
        - scripts/task-version
        - gradle/libs.versions.toml
    workflow_dispatch:

  permissions:
    contents: read
    packages: write

  jobs:
    publish:
      runs-on: ubuntu-latest
      env:
        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            distribution: corretto
            java-version: "21"
        - uses: gradle/actions/setup-gradle@v4
        - name: Версия jar
          id: version
          run: echo "value=$(scripts/task-version)" >> "$GITHUB_OUTPUT"
        # GitHub Packages не принимает версию повторно: тот же хеш (откат) — публикация пропускается.
        - name: Версия уже опубликована?
          id: published
          env:
            VERSION: ${{ steps.version.outputs.value }}
          run: |
            url="https://maven.pkg.github.com/$GITHUB_REPOSITORY/io/github/nakulov/standards/task/$VERSION/task-$VERSION.pom"
            if curl -fsS -o /dev/null -u "$GITHUB_ACTOR:$GITHUB_TOKEN" "$url"; then
              echo "value=true" >> "$GITHUB_OUTPUT"
            else
              echo "value=false" >> "$GITHUB_OUTPUT"
            fi
        - name: Публикация
          if: steps.published.outputs.value == 'false'
          run: >-
            ./gradlew :scripts:publishTaskPublicationToGitHubPackagesRepository
            -Pgpr.user="$GITHUB_ACTOR" -Pgpr.key="$GITHUB_TOKEN"
  ```
- [x] 3. Проверить синтаксис: `ruby -ryaml -e 'YAML.load_file(".github/workflows/publish-task.yml")'` — без вывода, код 0 (`ruby` есть в macOS); `./gradlew :scripts:test` — зелёный (`.github` — вход тестов, спеки содержимого репозитория перезапускаются).
- [x] 4. Коммит: «CI публикует jar task при слиянии в dev».

### Задача 4: Требования к машине в README
- Изменить: `README.md:52` — после абзаца «Обновление плагина…»

- [x] 1. Тест: не применим — текст документации без проверки формата.
- [x] 2. Текст — после строки `Обновление плагина — /plugin update …` (`README.md:52`), пустой строкой отделить:
  ```markdown
  На машине нужны JDK 21 (`brew install openjdk@21`) и токен GitHub с правом `read:packages`: `task` скачивает готовый
  jar своей версии из GitHub Packages. Токен берётся из `gpr.key` в `~/.gradle/gradle.properties`, иначе из `gh`
  (`gh auth refresh -s read:packages`); в CI проекта — секрет с этим правом в `GH_TOKEN`. jar этой версии не
  опубликован (ветка с изменённым кодом стандарта) — `task` собирает его локально Gradle.
  ```
- [x] 3. Коммит: «README: что нужно на машине для task».

### Приёмка
- [x] Что даёт этот PR — проверено: `scripts/task --help` в этом репозитории с пустым кешем (`XDG_CACHE_HOME=$(mktemp -d) scripts/task --help`) — до слияния jar не опубликован: строка «task: готовый jar не скачан — собираю локально…», затем справка `task`, код 0.
- [x] Крайние случаи обработаны: нет Java, Java 17, jar не скачан, сборка упала, jar из кеша, `-C` в копию — случаи `TaskLauncherSpec`.
- [x] Полный набор тестов зелёный: `./gradlew :scripts:check` — `BUILD SUCCESSFUL`.
- [x] Последний блок: критерии цели из «Обзора» — по пунктам:
  - публикация CI — workflow в PR, первый запуск — после слияния («После завершения»);
  - скачивание без компиляции — случай «jar из GitHub Packages», вживую — «После завершения»;
  - локальная сборка на ветке — приёмка выше;
  - без `kotlinc` — `grep -n kotlinc scripts/task scripts/setup scripts/build.gradle.kts` — пусто;
  - токен — строки `gpr.key` и `gh auth token` в `scripts/task` (задача 2).
- [x] Документация: `README.md` — требования к машине (задача 4).

## Открыто
- Доступ `GITHUB_TOKEN` этого репозитория к пакету KotestKit (настройки пакета, «Manage Actions access») — пользователь — до первого запуска workflow после слияния. Без него шаг «Публикация» упадёт на разрешении зависимостей Gradle.
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Ручная проверка: после слияния — workflow «Публикация task» в Actions зелёный, в Packages репозитория — `task` с версией = `scripts/task-version` из `dev`.
- Ручная проверка: в проекте с плагином после `/plugin update` — `XDG_CACHE_HOME=$(mktemp -d) task --help`: справка без строки «собираю локально» и без вывода Gradle.
- Ручная проверка: на ветке с изменённым `scripts/src/main` — `task --help`: строка «task: готовый jar не скачан — собираю локально…», затем справка.
