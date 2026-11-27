# Стандарт Python: ruff и pytest

**Цель:** стандарт Python для всех наших репозиториев с Python — правила в банке, конфиги ruff и pytest, доставка `setup`.
**Архитектура:** основа — все правила ruff (`select = ["ALL"]`), в банке только сознательные отклонения с ID `PY-…`, как у detekt. Конфиг стандарта — точная копия `ruff-standard.toml` и `pytest.ini` в проекте, настройки проекта — в его `ruff.toml` (`extend`) и `.coveragerc`. Команды проверки живут в разделе «Проверка» README языка, скилл плана и plan-reviewer ссылаются на банк.
**Стек:** ruff 0.17.6, pytest 9.2 + pytest-cov 7.1, coverage.py; Kotlin + Kotest/KotestKit + mockk (`scripts`); uv (`uv tool run`) в спеках.

---

## Ограничения
- Не трогать: соседний репозиторий с Python-скриптами (`tools/`), на котором отбирались правила, — другой репозиторий; его подключение — после PR 3, отдельной задачей там.
- В `docs/` — только абстрактные примеры, без названий наших продуктовых библиотек: репозиторий публичный.
- Файлов `*.py` в этом репозитории нет: `setup` увидел бы здесь Python, а образец с нарушением уронил бы `ruff check` самого репозитория. Образцы проектов для спек — константы в `SpecData`.
- Версия ruff закреплена `required-version` в `ruff-standard.toml`; `:scripts:test` требует uv — без него падает с «не найдена команда uv — установите: brew install uv».
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- Что реализуется: раздел банка «Кодстайл Python» (`PY-FMT`, `PY-LINT`) и «Тесты Python» (`PY-TEST`), строки BANK-6/7; раздел «Проверка» с командами у Kotlin и Python; конфиги `ruff-standard.toml`, `pytest.ini` и заготовки `ruff.toml`, `.coveragerc`; шаг `setup` для проектов с `*.py`.
- Какую проблему решает, что даёт: у скриптов на Python нет ни линтера, ни тестов; стандарт даёт машинную проверку стиля и порог покрытия, как detekt и Kover у Kotlin.
- Как встраивается: агент читает BANK-6/7 перед кодом Python и запускает команды «Проверки»; `setup` кладёт конфиги; `setup --check` ловит расхождение копии со стандартом.
- Критерии цели:
  - в банке есть правила `PY-…` и строки BANK-6/7;
  - `setup` ставит `ruff-standard.toml`, `pytest.ini`, `ruff.toml` и `.coveragerc` в проект с `*.py`, в проект без `*.py` — ничего;
  - спеки конфигов на образцах зелёные: чистый образец проходит ruff, нарушение ловится, порог покрытия роняет pytest.

## Контекст
- Задействованные файлы:
  - `docs/README.md` — оглавление банка, BANK-1…5;
  - `docs/code-style/kotlin/README.md` — образец формы раздела; раздела «Проверка» нет;
  - `docs/testing/kotlin/README.md`, `organization.md` — образец раздела тестов, KT-TEST-7 — порог покрытия;
  - `skills/decompose/SKILL.md:123,129,229,309`, `agents/plan-reviewer.md:87` — зашитый `./gradlew :<модуль>:check`; строки 123 и 129 — в блоке-маркере `plan:granularity`, его копия — в plan-reviewer (сверяет `MarkersSpec`);
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/StandardFiles.kt:15–47` — образец шага: копия ↔ шаблон, `Finding`;
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:26–44` — список шагов;
  - `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt:190` — `addedPaths`, рядом новый `trackedPaths`;
  - `scripts/src/test/.../plugin/BankIndexSpec.kt` — ловит раздел банка без строки: «раздел без строки: <раздел>»;
  - `templates/project/gitignore`, `.gitignore` — общий блок; `__pycache__/`, `.venv/` уже есть, `.coverage` — нет.
- Паттерны: спека с настоящими файлами — `makeProjectRoot()` (`GitignoreBlockSpecData.kt:7`), `writeProjectFile`, `readProjectFiles` (`StandardFilesSpecData.kt`); спека вне класса — комментарий «Вне стандарта» (`BankIndexSpec.kt`); запуск процесса — `ProcessShell` (`task/shell/ProcessShell.kt`), нет программы — «не найдена команда <программа> — установите: brew install <программа>».
- Зависимости: uv на машине разработчика (`uv tool run` качает ruff и pytest при первом запуске).

## Структура файлов

Отвечает за правила Python в банке (PR 1):
- Создать: `docs/code-style/python/README.md` — вход раздела: файлы, основа `ALL`, формат правила, бейзлайн `noqa`, подключение, «Проверка»
- Создать: `docs/code-style/python/formatting.md` — `PY-FMT`: длина строки, форматтер, докстринги
- Создать: `docs/code-style/python/lint.md` — `PY-LINT`: выключенные правила ruff, пороги, `noqa`, `ruff.toml` проекта
- Создать: `docs/testing/python/README.md` — вход раздела тестов: файлы, форма тестов через `PT`
- Создать: `docs/testing/python/organization.md` — `PY-TEST`: раскладка, порог покрытия, правила ruff в тестах
- Изменить: `docs/README.md:9–14` — BANK-6, BANK-7; BANK-3 и BANK-4 — с видами файлов Python

Отвечает за команды проверки языка (PR 1):
- Изменить: `docs/code-style/kotlin/README.md:51` — раздел «Проверка»: команды Gradle
- Изменить: `skills/decompose/SKILL.md:123,129,229,309` — шаг 5 и принцип 6 ссылаются на «Проверку» банка
- Изменить: `agents/plan-reviewer.md:87,93` — копия блока `plan:granularity`

Отвечает за конфиги ruff и pytest стандарта (PR 2):
- Создать: `templates/project/ruff-standard.toml` — `ALL`, `required-version`, отклонения с ID `PY-…`
- Создать: `templates/project/pytest.ini` — `minversion`, `required_plugins`, раскладка, `--cov`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/PythonTemplatesSpec.kt` — шаблоны на образцах
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/PythonTemplatesSpecData.kt` — образцы проектов и ожидаемый вывод
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindRuffOutput.kt` — запуск ruff в образце; вход: `findRuffOutput(files: Map<String, String>, args: List<String>): List<String>`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindPytestFailure.kt` — запуск pytest в образце; вход: `findPytestFailure(files: Map<String, String>): List<String>`

Отвечает за доставку конфигов Python (PR 3):
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt:196` — отслеживаемые файлы по шаблону; вход: `trackedPaths(pattern: String): List<String>`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt:184` — случай `trackedPaths`
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PythonConfig.kt` — шаг `setup` для проекта с `*.py`; вход: `PythonConfig(git: Git, pluginRoot: File) : Setup.Step`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PythonConfigSpec.kt` — случаи шага
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PythonConfigSpecData.kt` — шаблоны плагина и ожидаемые находки
- Создать: `templates/project/ruff.toml` — заготовка `ruff.toml` проекта
- Создать: `templates/project/coveragerc` — заготовка `.coveragerc` проекта
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:27` — шаг `PythonConfig` после `StandardFiles`
- Изменить: `templates/project/gitignore:11`, `.gitignore:12` — `.coverage` в общем блоке

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.

## Обзор решения
- Подход и архитектура: из груминга — А «основа — ruff»: `select = ["ALL"]`, отклонения — с ID и причиной; отбор — прогоном ruff 0.17.6 на скриптах соседнего репозитория (1 186 нарушений → 623 с конфигом стандарта; таблица утверждена пользователем при плане).
- Решения:
  - выключены 9 кодов в 6 решениях (`COM812`; `RUF001`–`003`; `T201`; `INP001`; `S603`, `S607`; `CPY001`), две настройки в конфиге (`line-length = 120`, `convention = "google"`), пороги сложности оставлены по умолчанию — без строки в конфиге, правила ruff для тестов — `per-file-ignores`; у каждого решения — `PY-…` и источник;
  - `ISC001` не выключается — изменено при плане: ruff 0.17.6 называет конфликтующим с форматтером только `COM812` (предупреждение `ruff format`);
  - конфиг pytest — точная копия `pytest.ini`; порог и исключения покрытия — в `.coveragerc` проекта: у pytest нет `extend`, а порог у каждого проекта свой; pytest-cov читает `fail_under` из `.coveragerc` (опыт: «FAIL Required test coverage of 100.0% not reached»);
  - `pythonpath = .` считается от папки `pytest.ini` — поэтому шаблон работает только копией в корне проекта, и спеки собирают образец с копией шаблона;
  - `include_namespace_packages = True` в `[report]` `.coveragerc` — изменено при плане (ревью, В1): без неё coverage.py не видит скрипт без тестов в папке без `__init__.py` (опыт: `tools/helper.py` без теста — отчёт 100%, с настройкой — 50%); образец спеки лежит в `tools/`, чтобы это ловилось;
  - разделы `[tool.pytest` и `[tool.coverage` в `pyproject.toml` — отклонения с советом, как `[tool.ruff]` — изменено при плане (ревью, В2): pytest берёт `pytest.ini` первым, даже пустой ([Configuration file formats](https://docs.pytest.org/en/stable/reference/customize.html#configuration-file-formats)), coverage — `.coveragerc` (опыт: `fail_under = 99` в `pyproject.toml` рядом с `.coveragerc` не действует). Свой `pytest.ini` проекта виден и так: `setup` пишет «отличается от стандарта», дифф виден до коммита;
  - образцы проектов — константы `SpecData`, а не файлы `*.py` (см. «Ограничения»);
  - шаг `PythonConfig` повторяет сверку копий из `StandardFiles` (≈ 15 строк) — дублирование выбрано пользователем: без связанности, PR 3 в лимите;
  - `.coverage` в общий блок `.gitignore` — добавлено при плане: `pytest` с `--cov` пишет его в корень проекта, без строки файл попадёт в коммит;
  - разбивка — 3 PR вместо 2 из груминга — изменено при плане: доставка не влезала в лимит 10 файлов.
- Как вписывается в систему: банк — BANK-6/7; конфиги — `templates/project/` рядом с `editorconfig`; доставка — шаг `setup` рядом с `StandardFiles`; процесс — шаг 5 задачи плана берёт команды из «Проверки» README языка.
- Обратная совместимость: не нужна — новый раздел и новый шаг; проекты без `*.py` шаг не меняет. Шаг 5 в планах, уже сохранённых с `./gradlew`, остаётся верным: та же команда теперь в «Проверке» Kotlin.
- Запасной: из груминга нет; при плане — если `uv tool run` недоступен в среде разработки, спеки PR 2 запускают `ruff` и `pytest` из `PATH` — переходим, если uv не ставится.
- Отклонено:
  - Б «основа — Google Python Style Guide» — заточен под инфраструктуру Google, 50+ разделов отбирать без опыта (груминг);
  - «только конфиг без docs/» — правила для людей тоже (груминг);
  - `per-file-ignores` как бейзлайн — новое нарушение тех же кодов не ловится (груминг);
  - шаблон `requirements-dev.txt` — навязывает pip, второй источник версий (груминг);
  - обёртка `task check` — прячет, что упало (груминг);
  - пропуск спек без uv — молчаливый пропуск хуже ручного шага (груминг);
  - расширить `StandardFiles` парами Python — PR 3 выходил за лимит (при плане);
  - образцы проектов файлами `*.py` в `src/test/resources` — `setup` и `ruff check` увидели бы их как код репозитория (при плане).

## Технические детали
- Структуры данных и изменения:
  - `PythonConfig.run(mode)` → `List<Finding>`, область `python`; копии: `ruff-standard.toml`, `pytest.ini`; заготовки: `ruff.toml`, `.coveragerc`.
  - Находки: нет `*.py` → OK «файлов Python нет — конфиги не нужны»; копии нет / отличается → DEVIATION «<файл>: нет» / «<файл>: отличается от стандарта», с apply → FIXED «<файл> записан (<состояние>) — закоммитьте через задачу»; заготовки нет → DEVIATION «<файл>: нет», с apply → FIXED «<файл> создан по заготовке — дальше его ведёт проект; закоммитьте через задачу»; `ruff.toml` без `extend = "ruff-standard.toml"`, `.ruff.toml`, `[tool.ruff`, `[tool.pytest`, `[tool.coverage` в `pyproject.toml` → DEVIATION с советом; всё по стандарту → OK «конфиги Python по стандарту».
- Параметры и форматы:
  - `noqa` с причиной — `# noqa: F401 причина`: ruff читает только коды, `RUF100` такую строку не помечает (опыт на 0.17.6);
  - порядок конфигов ruff в одной папке: `.ruff.toml` побеждает `ruff.toml` (опыт: `--show-settings` → `.ruff.toml`), `ruff.toml` побеждает `pyproject.toml` ([Config file discovery](https://docs.astral.sh/ruff/configuration/#config-file-discovery));
  - `--config <файл>` считает пути `per-file-ignores` от текущей папки — спеки запускают ruff из папки образца.
- Поток обработки: подключение — `setup` → проект ставит ruff и pytest → `ruff format` → `ruff check --add-noqa` → замер покрытия → `fail_under`; работа — BANK-6/7 → «Проверка» до коммита; изменение стандарта — шаблон и `PY-…` в одном PR → `setup --check` в проекте видит расхождение копии.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: правила Python и команды проверки языка в банке
  - [x] Задача 1: кодстайл Python в банке — BANK-6
  - [x] Задача 2: тесты Python в банке — BANK-7
  - [x] Задача 3: команды проверки — в «Проверке» банка, не в скилле
  - [ ] Приёмка
- PR 2: конфиги ruff и pytest стандарта, проверенные на образцах
  - [ ] Задача 4: `ruff-standard.toml` на образцах
  - [ ] Задача 5: `pytest.ini` и порог покрытия на образце
  - [ ] Приёмка
- PR 3: `setup` ставит конфиги Python в проект с `*.py`
  - [ ] Задача 6: `Git.trackedPaths`
  - [ ] Задача 7: шаг `PythonConfig`
  - [ ] Задача 8: шаг в `setup` и `.coverage` в `.gitignore`
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- `setup --check` в проекте с `*.py`: отклонения по четырём файлам; `setup` — файлы созданы; повторный `setup --check` — «конфиги Python по стандарту».
- Комментарий в задаче подключения соседнего репозитория со скриптами Python: стандарт готов — `setup`, `ruff format`, `ruff check --add-noqa`, порог покрытия.

## PR 1: правила Python и команды проверки языка в банке
**Объём:** `docs/code-style/python/` (3), `docs/testing/python/` (2), `docs/README.md`, `docs/code-style/kotlin/README.md`, `skills/decompose/SKILL.md`, `agents/plan-reviewer.md`; ≈ 9 файлов

### Задача 1: кодстайл Python в банке — BANK-6
- Создать: `docs/code-style/python/README.md` — вход раздела
- Создать: `docs/code-style/python/formatting.md` — `PY-FMT`
- Создать: `docs/code-style/python/lint.md` — `PY-LINT`
- Изменить: `docs/README.md:9` — строка BANK-6

Тест: проверка формата банка — `BankIndexSpec` (раздел без строки BANK падает).

- [x] 1. Падающий тест: раздел `code-style/python` без строки в оглавлении. Создать `docs/code-style/python/README.md`:

  ````markdown
  # Кодстайл Python

  Правила кода на Python для всех наших проектов. Читают:
  - человек — когда пишет код и проводит ревью;
  - агент, который пишет код, — перед правкой файлов `*.py`;
  - агенты ревью — когда проверяют код и имена в плане.

  ## Файлы

  | Префикс ID | Файл | О чём |
  |------------|------|-------|
  | `PY-FMT` | [formatting.md](formatting.md) | длина строки, форматтер, докстринги |
  | `PY-LINT` | [lint.md](lint.md) | выключенные правила ruff, пороги сложности, `noqa`, `ruff.toml` проекта |

  ## Основа — все правила ruff

  Стандарт включает все правила ruff: `select = ["ALL"]` в `ruff-standard.toml`. Правило ruff действует, пока стандарт его не выключил; что оно ловит и почему — на его странице в [документации ruff](https://docs.astral.sh/ruff/rules/), раздел «Why is this bad?». Здесь описаны только решения стандарта: выключенные правила, настройки и правила ревью. У каждого — ID, и тот же ID стоит в комментарии строки `ruff-standard.toml`.

  Версия ruff закреплена (`required-version`): `ALL` с новой версией включает новые правила, поэтому версия меняется только вместе со стандартом.

  ## Формат правила

  Тот же, что у кодстайла Kotlin, — [docs/code-style/kotlin/README.md](../kotlin/README.md#формат-правила): ID `PY-<РАЗДЕЛ>-<N>`, «Почему», «Проверка», примеры «Запрещено» и «Разрешено» в одном блоке. «Проверка» — `ruff <КОД>`, `ruff format` или `ревью`. Причина правила ревью — со ссылкой на источник: PEP, Google Python Style Guide, документация инструмента.

  Замечание ревью ссылается на ID: «нарушено PY-LINT-7».

  ## Бейзлайн `noqa`

  Бейзлайн — комментарии `# noqa: <коды>`, которые ставит `ruff check --add-noqa` при подключении стандарта или при обновлении стандарта с новыми правилами. Он держит находки (правило + строка), а не файлы: новое нарушение в той же строке другого правила и любое нарушение в другой строке ловятся. Бейзлайн только уменьшается: исправил нарушение — `RUF100` требует убрать ненужный `noqa`; ради нового кода бейзлайн не пополняется. Сознательное исключение — `noqa` с причиной (PY-LINT-7). Порог покрытия (PY-TEST-2) не снижается.

  ## Подключение

  `setup` ставит конфиги, если git видит в проекте файлы `*.py`:
  - `ruff-standard.toml` и `pytest.ini` — копии стандарта: проект их не правит, `setup --check` сверяет;
  - `ruff.toml` (`extend = "ruff-standard.toml"`) и `.coveragerc` — заготовки: `setup` создаёт их один раз, дальше их ведёт проект.

  Дальше проект один раз:
  1. Ставит ruff версии из `required-version` и pytest с pytest-cov своим способом — pip, uv, poetry.
  2. `ruff format` — у форматтера нет бейзлайна, код форматируется целиком.
  3. `ruff check --add-noqa` — бейзлайн.
  4. `pytest` — замер покрытия; `fail_under` в `.coveragerc` — замер с округлением вниз.

  ## Проверка

  Команды перед коммитом задачи плана — из корня проекта: там `ruff.toml` и `pytest.ini`.

  | Команда | Успех | Падение |
  |---------|-------|---------|
  | `ruff format --check` | `N files already formatted` | `Would reformat: <файл>` |
  | `ruff check` | `All checks passed!` | `<файл>:<строка>:<столбец>: <код> <сообщение>` |
  | `pytest` | `N passed`, порог покрытия пройден | упавший тест или `FAIL Required test coverage of <порог>% not reached` |
  | `pytest tests/test_<модуль>.py --no-cov` — один файл тестов | `N passed` | упавший тест; без `--no-cov` падает по порогу: он считается по всем тестам |
  ````

- [x] 2. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — падает: `раздел без строки: code-style/python`
- [x] 3. Строка BANK-6 и файлы раздела. В `docs/README.md` после строки BANK-5 (`docs/README.md:13`) добавить:

  ```markdown
  | BANK-6 | пишешь или правишь код Python (`*.py`) — в плане или в файлах | [code-style/python/README.md](code-style/python/README.md), затем файлы из его таблицы |
  ```

  Создать `docs/code-style/python/formatting.md`:

  ````markdown
  # Форматирование — PY-FMT

  Формат правил и префиксы — [README.md](README.md).

  ### PY-FMT-1. Строка — не длиннее 120 символов
  Почему: тот же предел, что в `.editorconfig` стандарта для всех файлов (`max_line_length = 120`): IDE переносит код по 120, а ruff с пределом по умолчанию 88 спорил бы с ней.
  Проверка: ruff `E501`, `ruff format`; `line-length = 120` в `ruff-standard.toml`
  ```python
  # Разрешено — 120 символов и меньше
  result = subprocess.run(command, capture_output=True, text=True, check=True)

  # Запрещено — длиннее 120: ruff format перенесёт, E501 — если перенести нельзя
  message = "Конфиг ruff проекта не наследует стандарт: добавьте строку extend в ruff.toml, закоммитьте её и запустите setup --check ещё раз"
  ```

  ### PY-FMT-2. Код форматирует `ruff format`; правило `COM812` выключено
  Почему: один формат во всех проектах без споров на ревью. `COM812` требует запятую там, где форматтер её убирает: ruff сам предупреждает о конфликте и советует выключить правило ([Conflicting lint rules](https://docs.astral.sh/ruff/formatter/#conflicting-lint-rules)).
  Проверка: `ruff format --check`; `COM812` в `ignore` `ruff-standard.toml`
  ```python
  # Разрешено — как оставил ruff format
  config = {"path": path, "strict": True}

  # Запрещено — ручное выравнивание и одинарные кавычки: ruff format перепишет
  config = {'path'  : path,
            'strict': True}
  ```

  ### PY-FMT-3. Докстринги — в стиле Google
  Почему: Google Python Style Guide, раздел 3.8 ([Comments and Docstrings](https://google.github.io/styleguide/pyguide.html#38-comments-and-docstrings)). Настройка `convention = "google"` выключает правила pydocstyle, несовместимые со стилем, в том числе по одному из пар `D203`/`D211` и `D212`/`D213`, которые иначе спорят ([lint.pydocstyle.convention](https://docs.astral.sh/ruff/settings/#lint_pydocstyle_convention)).
  Проверка: ruff `D`; `convention = "google"` в `ruff-standard.toml` + ревью (разделы `Args`, `Returns`)
  Докстринг обязателен у модуля, публичного класса и публичной функции (`D100`–`D107`); в тестах — нет (PY-TEST-3).

  ```python
  # Разрешено
  def greet(name: str) -> str:
      """Возвращает приветствие.

      Args:
          name: Имя адресата.

      Returns:
          Строка «Привет, <имя>».
      """
      return f"Привет, {name}"

  # Запрещено — параметры в стиле reST вместо раздела Args
  def greet(name: str) -> str:
      """Возвращает приветствие.

      :param name: имя адресата
      """
      return f"Привет, {name}"
  ```
  ````

  Создать `docs/code-style/python/lint.md`:

  ````markdown
  # Правила ruff — PY-LINT

  Формат правил и префиксы — [README.md](README.md).

  ### PY-LINT-1. Кириллица в строках, докстрингах и комментариях — не нарушение
  Почему: `RUF001`–`RUF003` ищут символы, похожие на латинские, и считают «неоднозначной» каждую кириллическую `а`, `е`, `о`, `с` ([ambiguous-unicode-character-string](https://docs.astral.sh/ruff/rules/ambiguous-unicode-character-string/)); тексты наших скриптов — русские, правило срабатывало бы почти на каждой строке.
  Проверка: `RUF001`, `RUF002`, `RUF003` в `ignore` `ruff-standard.toml`; кириллица в именах по-прежнему ловится — ruff `PLC2401`
  ```python
  # Разрешено
  print("Готово: файлы проверены")

  # Запрещено — кириллица в имени
  def проверить(path: str) -> bool:
      ...
  ```

  ### PY-LINT-2. `print` — вывод скрипта
  Почему: наш Python — CLI-скрипты, и `print` в них — результат работы; `T201` ищет отладочный вывод, который должен уйти из кода или в `logging` ([print](https://docs.astral.sh/ruff/rules/print/)).
  Проверка: `T201` в `ignore` `ruff-standard.toml` + ревью (отладочный `print` не остаётся)
  ```python
  # Разрешено — результат скрипта
  print(f"Проверено файлов: {count}")

  # Запрещено — отладка
  print("DEBUG", paths)
  ```

  ### PY-LINT-3. Папка скриптов — без `__init__.py`
  Почему: скрипт запускают (`python tools/check.py`), а не импортируют как пакет; `INP001` требует `__init__.py` в каждой папке с модулями ([implicit-namespace-package](https://docs.astral.sh/ruff/rules/implicit-namespace-package/)).
  Проверка: `INP001` в `ignore` `ruff-standard.toml`
  ```text
  // Разрешено
  tools/check.py
  tools/report.py

  // Запрещено — пустой __init__.py ради правила
  tools/__init__.py
  ```

  ### PY-LINT-4. `subprocess` — с командой-списком, без `shell=True`
  Почему: `S603` срабатывает на любой вызов `subprocess` без `shell=True` и не проверяет, откуда пришли аргументы ([subprocess-without-shell-equals-true](https://docs.astral.sh/ruff/rules/subprocess-without-shell-equals-true/)); `S607` — на каждый `git` и `gh` без полного пути ([start-process-with-partial-path](https://docs.astral.sh/ruff/rules/start-process-with-partial-path/)), а путь к ним у каждой машины свой. Опасное по-прежнему ловится: `shell=True` — `S602`, вызов без `check=` — `PLW1510`.
  Проверка: `S603`, `S607` в `ignore` `ruff-standard.toml`
  ```python
  # Разрешено
  subprocess.run(["git", "branch", "--show-current"], capture_output=True, text=True, check=True)

  # Запрещено — строка через оболочку (S602) и без check (PLW1510)
  subprocess.run(f"git log {branch}", shell=True)
  ```

  ### PY-LINT-5. Без заголовка с копирайтом
  Почему: в файлах наших репозиториев нет заголовков лицензии — ни в Kotlin, ни в Python; `CPY001` требует его в каждом файле ([missing-copyright-notice](https://docs.astral.sh/ruff/rules/missing-copyright-notice/)).
  Проверка: `CPY001` в `ignore` `ruff-standard.toml`
  ```python
  # Разрешено — файл начинается с докстринга модуля
  """Проверка ссылок в документации."""

  # Запрещено — заголовок ради правила
  # Copyright 2026 …
  ```

  ### PY-LINT-6. Сложность — пороги ruff по умолчанию
  Почему: своих порогов без опыта на Python не вводим, пороги Kotlin (KT-FUN-7) не переносятся. Ruff берёт значения pylint — `max-branches = 12`, `max-args = 5`, `max-returns = 6`, `max-statements = 50` ([lint.pylint](https://docs.astral.sh/ruff/settings/#lintpylint)) — и порог McCabe 10 ([lint.mccabe](https://docs.astral.sh/ruff/settings/#lintmccabe)).
  Проверка: ruff `C901`, `PLR0911`–`PLR0917`
  ```python
  # Разрешено — ветки вынесены в функции с именами
  def check(path: str) -> list[str]:
      return check_links(path) + check_headings(path)

  # Запрещено — все проверки в одной функции: ветки растут с каждой новой (PLR0912 — с 13-й)
  def check(path: str) -> list[str]:
      errors = []
      text = read(path)
      if "](" in text:
          errors += check_link_targets(text)
      if "# " in text:
          errors += check_heading_levels(text)
      # … ещё 11 if — PLR0912
      return errors
  ```

  ### PY-LINT-7. `noqa` — с кодом и причиной после него
  Почему: `# noqa` без кода глушит все правила строки (`PGH004`), а без причины следующий читатель не знает, можно ли его снять; текст после кодов ruff не читает ([Error suppression](https://docs.astral.sh/ruff/linter/#error-suppression)). `noqa` без причины — только бейзлайн от `ruff check --add-noqa` ([README.md](README.md#бейзлайн-noqa)).
  Проверка: ruff `PGH004` (без кода), `RUF100` (ненужный) + ревью (причина)
  ```python
  # Разрешено
  import readline  # noqa: F401 импорт включает историю ввода в input()

  # Запрещено — без кода
  import readline  # noqa

  # Запрещено — новый noqa без причины
  import readline  # noqa: F401
  ```

  ### PY-LINT-8. `ruff.toml` проекта не ослабляет стандарт
  Почему: `ignore` в проекте прячет правило во всех файлах и для нового кода, а бейзлайн `noqa` держит находки по строкам и только уменьшается ([README.md](README.md#бейзлайн-noqa)). Ruff не запрещает `ignore` в наследнике — это правило ревью.
  Проверка: ревью; `setup --check` — что `ruff.toml` наследует стандарт
  В `ruff.toml` проекта — пути (`src`, `extend-exclude` для сгенерированного кода) и более строгие настройки. `ignore`, `extend-ignore`, `per-file-ignores` — нет: исключение — `noqa` с причиной (PY-LINT-7).

  ```toml
  # Разрешено
  extend = "ruff-standard.toml"
  src = ["tools"]
  extend-exclude = ["tools/generated"]

  # Запрещено
  extend = "ruff-standard.toml"
  lint.ignore = ["ANN"]
  ```
  ````

- [x] 4. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — `BUILD SUCCESSFUL`
- [x] 5. Проверки языка: не применимы — Markdown; проверка формата банка — шаг 4
- [x] 6. Коммит — раздел и строка BANK-6 вместе

### Задача 2: тесты Python в банке — BANK-7
- Создать: `docs/testing/python/README.md` — вход раздела тестов
- Создать: `docs/testing/python/organization.md` — `PY-TEST`
- Изменить: `docs/README.md:11–12` — строка BANK-7; BANK-3 и BANK-4 с видами файлов Python

Тест: `BankIndexSpec`.

- [x] 1. Падающий тест: раздел `testing/python` без строки. Создать `docs/testing/python/README.md`:

  ````markdown
  # Тесты Python

  Правила тестов на Python для всех наших проектов. Читают:
  - человек — когда пишет тесты и проводит ревью;
  - агент, который пишет код, — перед правкой файлов в `tests/`;
  - агенты ревью — когда проверяют тесты.

  Тесты — тоже код: к ним применяется и кодстайл Python (`docs/code-style/python/`). Правило тестов, которое ему противоречит, называет исключение явно.

  ## Файлы

  | Префикс ID | Файл | О чём |
  |------------|------|-------|
  | `PY-TEST` | [organization.md](organization.md) | раскладка тестов, порог покрытия, правила ruff в тестах |

  Форма тестов pytest — фикстуры, `parametrize`, `raises` — проверяет группа ruff `PT` ([flake8-pytest-style](https://docs.astral.sh/ruff/rules/#flake8-pytest-style-pt)) из основы `ALL`; своих правил на форму нет.

  ## Формат правила

  Тот же, что у кодстайла Kotlin, — [docs/code-style/kotlin/README.md](../../code-style/kotlin/README.md#формат-правила): ID, «Почему», «Проверка», примеры «Запрещено» и «Разрешено» в одном блоке.

  Замечание ревью ссылается на ID: «нарушено PY-TEST-2».
  ````

- [x] 2. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — падает: `раздел без строки: testing/python`
- [x] 3. Строки банка и файл правил. В `docs/README.md` строки BANK-3 и BANK-4 заменить и после BANK-6 добавить BANK-7:

  ```markdown
  | BANK-3 | переносишь в файлы готовый код из плана | ничего: код плана уже по стандарту. BANK-1, BANK-2, BANK-6, BANK-7 — только когда пишешь код не из плана: чинишь упавший тест или сборку |
  | BANK-4 | проверяешь дифф | BANK-1, BANK-2, BANK-6, BANK-7 — по видам изменённых файлов |
  ```

  ```markdown
  | BANK-7 | пишешь или правишь тест Python (`tests/`) | BANK-6 и ещё [testing/python/README.md](testing/python/README.md) |
  ```

  Создать `docs/testing/python/organization.md`:

  ````markdown
  # Организация тестов — PY-TEST

  Формат правил и префиксы — [README.md](README.md).

  ### PY-TEST-1. Тесты — `tests/test_<модуль>.py` в корне проекта
  Почему: раскладка «тесты вне кода приложения» из pytest Good Integration Practices ([Choosing a test layout](https://docs.pytest.org/en/stable/explanation/goodpractices.html#choosing-a-test-layout)): по модулю тест находится сразу, и наоборот. `pytest.ini` стандарта ищет тесты в `tests/` (`testpaths`) и импортирует модули проекта из корня (`pythonpath = .`).
  Проверка: ревью
  ```text
  // Разрешено
  tools/check_links.py
  tests/test_check_links.py

  // Запрещено — тест рядом с кодом и по сценарию
  tools/test_check_links.py
  tests/test_release_flow.py
  ```

  ### PY-TEST-2. Непокрытая строка — повод для нового теста
  Почему: порог покрытия ловит непроверенный код, только пока его не снижают (как KT-TEST-7); pytest-cov роняет запуск ниже `fail_under` из `.coveragerc` ([pytest-cov: Configuration](https://pytest-cov.readthedocs.io/en/latest/config.html)).
  Проверка: pytest-cov (`fail_under` в `.coveragerc` проекта) + ревью
  - Порог — замер при подключении с округлением вниз; поднимается, не снижается.
  - Из подсчёта исключается только блок `if __name__ == "__main__":` — `exclude_also` в `.coveragerc` ([Excluding code](https://coverage.readthedocs.io/en/latest/excluding.html)): он запускает скрипт, а тесты вызывают функции.
  - Скрипты в папках без `__init__.py` (PY-LINT-3) считаются, только пока в `.coveragerc` стоит `include_namespace_packages = True` ([include_namespace_packages](https://coverage.readthedocs.io/en/latest/config.html#report-include-namespace-packages)): без неё скрипт без тестов не попадает в отчёт и не снижает процент.
  - Непокрытые строки — колонка `Missing` отчёта `pytest`.

  ```ini
  # Разрешено — .coveragerc
  [report]
  fail_under = 84
  include_namespace_packages = True
  exclude_also =
      if __name__ == .__main__.:

  # Запрещено — порог снижен под новый код
  [report]
  fail_under = 70  # было 84
  ```

  ### PY-TEST-3. В тестах выключены `S101`, `PLR2004`, `D1`
  Почему: pytest проверяет через `assert` (`S101` — о `assert` в коде приложения, [assert](https://docs.astral.sh/ruff/rules/assert/)); ожидаемое значение в тесте — литерал, а не константа (`PLR2004`); функция теста короткая, и её имя говорит, что проверяется, — докстринг не обязателен (Google Python Style Guide, [3.8.3 Functions and Methods](https://google.github.io/styleguide/pyguide.html#383-functions-and-methods)).
  Проверка: `per-file-ignores` для `tests/**` в `ruff-standard.toml`
  ```python
  # Разрешено — tests/test_check_links.py
  def test_broken_link_found() -> None:
      assert count_broken("[a](missing.md)") == 1

  # Запрещено — константа и докстринг ради правил
  EXPECTED_BROKEN = 1

  def test_broken_link_found() -> None:
      """Проверяет, что битая ссылка найдена."""
      assert count_broken("[a](missing.md)") == EXPECTED_BROKEN
  ```
  ````

- [x] 4. Запустить `./gradlew :scripts:test --tests '*BankIndexSpec*'` — `BUILD SUCCESSFUL`
- [x] 5. Проверки языка: не применимы — Markdown
- [x] 6. Коммит — раздел и строки банка вместе

### Задача 3: команды проверки — в «Проверке» банка, не в скилле
- Изменить: `docs/code-style/kotlin/README.md:51` — раздел «Проверка»
- Изменить: `skills/decompose/SKILL.md:123,129,229,309` — ссылки на «Проверку»
- Изменить: `agents/plan-reviewer.md:87,93` — копия блока `plan:granularity`

Тест: `MarkersSpec` — копия блока `plan:granularity` совпадает с источником; сам текст — «тест: не применим — текст скилла».

- [x] 1. Падающий тест: правка источника блока без копии. В `skills/decompose/SKILL.md` заменить строку 123:

  было:
  ```markdown
  5. Запусти проверки модуля (`./gradlew :<модуль>:check`) — убедись, что проходят.
  ```
  стало:
  ```markdown
  5. Запусти проверки языка — команды раздела «Проверка» в README кодстайла языка (`docs/code-style/<язык>/README.md` банка) — убедись, что проходят.
  ```

  и в строке 129 `проверки модуля — тоже не применимы` → `проверки языка — тоже не применимы`.
- [x] 2. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — падает на блоке `plan:granularity`: копия в `agents/plan-reviewer.md` расходится с источником
- [x] 3. Копия, остальные строки скилла и «Проверка» Kotlin:
  - `agents/plan-reviewer.md:87` и строка «Без логики …» блока — те же замены, что в шаге 1.
  - `skills/decompose/SKILL.md:229`:

    было:
    ```markdown
    6. **Код и команды в шагах** — полный код в каждом шаге, который меняет код; повторяющийся — допустимо «как в задаче N». Команды — точные, с ожидаемым выводом одной-двумя ключевыми строками: `./gradlew :scripts:test --tests '*PrBodySpec*'` → `BUILD SUCCESSFUL`; падение — `Unresolved reference: withoutComments`. Проверки модуля задачи — `./gradlew :scripts:check` → `BUILD SUCCESSFUL`; падение — `FunctionSignature`.
    ```
    стало:
    ```markdown
    6. **Код и команды в шагах** — полный код в каждом шаге, который меняет код; повторяющийся — допустимо «как в задаче N». Команды — точные, с ожидаемым выводом одной-двумя ключевыми строками: успех и причина падения. Команды теста и проверок языка и их вывод — из раздела «Проверка» README кодстайла языка в банке (`docs/code-style/<язык>/README.md`).
    ```
  - `skills/decompose/SKILL.md:309`: `- [ ] 5. Запустить \`./gradlew :[модуль]:check\` — зелёный` → `- [ ] 5. Запустить проверки языка из «Проверки» README языка в банке — зелёный`.
  - `docs/code-style/kotlin/README.md` — в конец файла:

    ```markdown

    ## Проверка

    Команды перед коммитом задачи плана — из корня репозитория; `<модуль>` — Gradle-модуль с изменёнными файлами.

    | Команда | Успех | Падение |
    |---------|-------|---------|
    | `./gradlew :<модуль>:test --tests '*<Имя>Spec*'` — одна спека | `BUILD SUCCESSFUL` | `Unresolved reference: <имя>` или упавший случай спеки |
    | `./gradlew :<модуль>:check` — тесты, detekt, порог покрытия | `BUILD SUCCESSFUL` | имя правила detekt, например `FunctionSignature`, или порог покрытия (KT-TEST-7) |
    ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*MarkersSpec*'` — `BUILD SUCCESSFUL`; `grep -rn "gradlew" skills/decompose/SKILL.md agents/plan-reviewer.md` — пусто
- [x] 5. Проверки языка: не применимы — Markdown; полный `./gradlew :scripts:test` — `BUILD SUCCESSFUL` (спеки читают `skills/` и `agents/`)
- [x] 6. Коммит — скилл, агент и «Проверка» Kotlin вместе

### Приёмка
- [ ] Правила Python и команды проверки языка в банке — проверено: BANK-6/7 ведут на разделы, в скилле и агенте нет `gradlew`
- [ ] Крайние случаи обработаны: BANK-3 и BANK-4 называют виды файлов Python; запуск одного файла тестов — с `--no-cov`
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` — `BUILD SUCCESSFUL`

## PR 2: конфиги ruff и pytest стандарта, проверенные на образцах
**Объём:** `templates/project/ruff-standard.toml`, `templates/project/pytest.ini`, `scripts/src/test/.../plugin/` (4 файла); ≈ 6 файлов

### Задача 4: `ruff-standard.toml` на образцах
- Создать: `templates/project/ruff-standard.toml` — конфиг стандарта
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindRuffOutput.kt` — запуск ruff в образце
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/PythonTemplatesSpec.kt` — случаи ruff
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/PythonTemplatesSpecData.kt` — образцы и ожидаемый вывод

- [ ] 1. Падающий тест: образцы проходят и нарушают стандарт. Создать `PythonTemplatesSpecData.kt`:

  ```kotlin
  package io.github.nakulov.standards.plugin

  const val TOOL_PATH = "tools/tool.py"
  const val TEST_TOOL_PATH = "tests/test_tool.py"

  /** Скрипт по стандарту: русские тексты, `print`, вызов `git`; тест покрывает только `greet`. */
  const val TOOL_PY = "\"\"\"Скрипт-образец: русские тексты, print и вызов git.\"\"\"\n" +
      "\n" +
      "import subprocess\n" +
      "\n" +
      "\n" +
      "def branch() -> str:\n" +
      "    \"\"\"Возвращает текущую ветку git.\"\"\"\n" +
      "    command = [\"git\", \"branch\", \"--show-current\"]\n" +
      "    result = subprocess.run(command, capture_output=True, text=True, check=True)\n" +
      "    return result.stdout.strip()\n" +
      "\n" +
      "\n" +
      "def greet(name: str) -> str:\n" +
      "    \"\"\"Возвращает приветствие «Привет, <имя>».\"\"\"\n" +
      "    return f\"Привет, {name}\"\n" +
      "\n" +
      "\n" +
      "if __name__ == \"__main__\":\n" +
      "    print(greet(branch()))\n"

  /** Тест без докстринга, с `assert` и литералом — по PY-TEST-3; скрипт — в `tools/` без `__init__.py` (PY-LINT-3). */
  const val TEST_TOOL_PY = "from tools.tool import greet\n" +
      "\n" +
      "\n" +
      "def test_greet() -> None:\n" +
      "    assert greet(\"Аня\") == \"Привет, Аня\"\n"

  const val UNUSED_IMPORT_PY = "\"\"\"Скрипт-образец с неиспользуемым импортом.\"\"\"\n" +
      "\n" +
      "import os\n"

  fun makeCleanProject(): Map<String, String> {
      return mapOf(TOOL_PATH to TOOL_PY, TEST_TOOL_PATH to TEST_TOOL_PY)
  }

  fun makeUnusedImportProject(): Map<String, String> {
      return mapOf(TOOL_PATH to UNUSED_IMPORT_PY)
  }

  fun makeCleanCheckOutput(): List<String> {
      return listOf("0", "All checks passed!")
  }

  fun makeCleanFormatOutput(): List<String> {
      return listOf("0", "2 files already formatted")
  }

  fun makeUnusedImportOutput(): List<String> {
      return listOf(
          "1",
          "tools/tool.py:3:8: F401 [*] `os` imported but unused",
          "Found 1 error.",
          "[*] 1 fixable with `--fix`.",
      )
  }
  ```

  Создать `FindRuffOutput.kt`:

  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.standards.setup.makeProjectRoot
  import io.github.nakulov.standards.setup.writeProjectFile
  import io.github.nakulov.standards.task.shell.ProcessShell
  import java.io.File

  private const val TEMPLATE = "templates/project/ruff-standard.toml"
  private const val CONFIG = "ruff-standard.toml"
  private const val VERSION_PATTERN = "required-version = \"==(.+)\""

  // Исключение KT-TEST-6: ruff проверяет настоящие файлы — образец [files] собирается во временной папке рядом с копией
  // шаблона, как её кладёт setup, и ruff запускается процессом из этой папки: пути per-file-ignores считаются от неё.
  // Версия ruff — из required-version шаблона: тест проверяет ту версию, которую стандарт требует от проектов.
  fun findRuffOutput(
      files: Map<String, String>,
      args: List<String>,
  ): List<String> {
      val template = File(System.getProperty(/* key = */ "task.repoRoot"), TEMPLATE)
      val match = Regex(pattern = VERSION_PATTERN).find(input = template.readText())
          ?: error(message = "в $TEMPLATE нет required-version")
      val project = makeProjectRoot()
      template.copyTo(target = File(project, CONFIG))
      files.forEach { (path, text) -> writeProjectFile(project, path, text) }
      val command = listOf("uv", "tool", "run", "ruff@${match.groupValues[1]}") + args +
          listOf("--no-cache", "--config", CONFIG)
      val result = ProcessShell(directory = project).run(command)
      val lines = result.out
          .lines()
          .filter { line -> line.isNotBlank() }
      return listOf("${result.code}") + lines
  }
  ```

  Создать `PythonTemplatesSpec.kt`:

  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.kotestkit.TestOptions
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.kotest.core.spec.style.BehaviorSpec

  // Вне стандарта: спека проверяет не класс, а шаблоны конфигов Python `templates/project/` на образцах проектов:
  // ошибка в шаблоне ломает проверки во всех проектах. Сборка образца и запуск — в FindRuffOutput и FindPytestFailure.
  class PythonTemplatesSpec : BehaviorSpec({
      shouldReturnTest(makeCleanCheckOutput(), options = TestOptions(name = "ruff check: образец по стандарту")) {
          findRuffOutput(files = makeCleanProject(), args = listOf("check", "--output-format", "concise"))
      }

      shouldReturnTest(makeCleanFormatOutput(), options = TestOptions(name = "ruff format: образец отформатирован")) {
          findRuffOutput(files = makeCleanProject(), args = listOf("format", "--check"))
      }

      shouldReturnTest(makeUnusedImportOutput(), options = TestOptions(name = "ruff check: неиспользуемый импорт")) {
          findRuffOutput(files = makeUnusedImportProject(), args = listOf("check", "--output-format", "concise"))
      }
  })
  ```

- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PythonTemplatesSpec*'` — падает: `FileNotFoundException` … `templates/project/ruff-standard.toml`
- [ ] 3. Минимальный код: создать `templates/project/ruff-standard.toml`:

  ```toml
  # Общий конфиг ruff стандарта: setup кладёт копию в корень проекта и сверяет её (setup --check).
  # Копию не правят: настройки проекта — в ruff.toml с extend = "ruff-standard.toml".
  # Основа — все правила ruff; у каждого отклонения — ID правила из docs/code-style/python/ и docs/testing/python/.
  # Версия закреплена: ALL с новой версией включает новые правила — версия меняется только вместе со стандартом.
  required-version = "==0.17.6"
  line-length = 120  # PY-FMT-1

  [lint]
  select = ["ALL"]
  ignore = [
      "COM812",  # PY-FMT-2: конфликтует с ruff format
      "RUF001",  # PY-LINT-1: кириллица в строках
      "RUF002",  # PY-LINT-1: кириллица в докстрингах
      "RUF003",  # PY-LINT-1: кириллица в комментариях
      "T201",  # PY-LINT-2: print — вывод скрипта
      "INP001",  # PY-LINT-3: папка скриптов — не пакет
      "S603",  # PY-LINT-4: subprocess с командой-списком
      "S607",  # PY-LINT-4: git и gh без полного пути
      "CPY001",  # PY-LINT-5: без заголовка с копирайтом
  ]

  [lint.pydocstyle]
  convention = "google"  # PY-FMT-3

  [lint.per-file-ignores]
  "tests/**" = ["S101", "PLR2004", "D1"]  # PY-TEST-3
  ```

- [ ] 4. Запустить `./gradlew :scripts:test --tests '*PythonTemplatesSpec*'` — `BUILD SUCCESSFUL`, 3 теста
- [ ] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] 6. Коммит — шаблон и спека вместе

### Задача 5: `pytest.ini` и порог покрытия на образце
- Создать: `templates/project/pytest.ini` — конфиг pytest стандарта
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/FindPytestFailure.kt` — запуск pytest в образце
- Изменить: `PythonTemplatesSpec.kt`, `PythonTemplatesSpecData.kt` — случай порога

- [ ] 1. Падающий тест: порог из `.coveragerc` роняет pytest. В `PythonTemplatesSpecData.kt` добавить:

  ```kotlin
  /** Порог 100% при покрытии 57%; блок `__main__` и `tests/` — вне подсчёта, `tools/` без `__init__.py` — в нём. */
  const val STRICT_COVERAGERC = "[run]\n" +
      "source = .\n" +
      "omit = tests/*\n" +
      "\n" +
      "[report]\n" +
      "fail_under = 100\n" +
      "include_namespace_packages = True\n" +
      "exclude_also =\n" +
      "    if __name__ == .__main__.:\n"

  fun makeLowCoverageProject(): Map<String, String> {
      return mapOf(TOOL_PATH to TOOL_PY, TEST_TOOL_PATH to TEST_TOOL_PY, ".coveragerc" to STRICT_COVERAGERC)
  }

  fun makeCoverageFailure(): List<String> {
      return listOf("1", "FAIL Required test coverage of 100.0% not reached. Total coverage: 57.14%")
  }
  ```

  В `PythonTemplatesSpec.kt` после третьего теста:

  ```kotlin
      shouldReturnTest(makeCoverageFailure(), options = TestOptions(name = "pytest: покрытие ниже порога")) {
          findPytestFailure(files = makeLowCoverageProject())
      }
  ```

  Создать `FindPytestFailure.kt`:

  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.standards.setup.makeProjectRoot
  import io.github.nakulov.standards.setup.writeProjectFile
  import io.github.nakulov.standards.task.shell.ProcessShell
  import java.io.File

  private const val TEMPLATE = "templates/project/pytest.ini"
  private const val CONFIG = "pytest.ini"
  private const val PYTEST = "pytest==9.2.0"
  private const val PYTEST_COV = "pytest-cov==7.1.0"
  private const val FAILURE = "FAIL "

  // Исключение KT-TEST-6: pytest запускается процессом в образце [files] с копией шаблона в корне, как её кладёт setup:
  // pythonpath шаблона считается от папки pytest.ini. Версии — нижние границы шаблона (minversion, required_plugins),
  // закреплённые: тест не зависит от новых выпусков; поднял границу в pytest.ini — подними и PYTEST, PYTEST_COV.
  // Из вывода остаются код выхода и строка FAIL pytest-cov — остальное (платформа, время) зависит от машины.
  fun findPytestFailure(files: Map<String, String>): List<String> {
      val project = makeProjectRoot()
      File(System.getProperty(/* key = */ "task.repoRoot"), TEMPLATE).copyTo(target = File(project, CONFIG))
      files.forEach { (path, text) -> writeProjectFile(project, path, text) }
      val command = listOf("uv", "tool", "run", "--from", PYTEST, "--with", PYTEST_COV, "pytest", "-p", "no:cacheprovider")
      val result = ProcessShell(directory = project).run(command)
      val failures = result.out
          .lines()
          .filter { line -> line.startsWith(prefix = FAILURE) }
      return listOf("${result.code}") + failures
  }
  ```

- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PythonTemplatesSpec*'` — падает: `NoSuchFileException` … `templates/project/pytest.ini` (бросает `copyTo`)
- [ ] 3. Минимальный код: создать `templates/project/pytest.ini`:

  ```ini
  # Общий конфиг pytest стандарта: setup кладёт копию в корень проекта и сверяет её (setup --check).
  # Порог покрытия и исключения — в .coveragerc проекта (PY-TEST-2): порог у каждого проекта свой.
  [pytest]
  minversion = 9.2
  required_plugins = pytest-cov>=7.1
  # Тесты — tests/test_<модуль>.py, модули проекта импортируются из корня (PY-TEST-1).
  testpaths = tests
  pythonpath = .
  addopts = --cov --cov-report=term-missing
  ```

- [ ] 4. Запустить `./gradlew :scripts:test --tests '*PythonTemplatesSpec*'` — `BUILD SUCCESSFUL`, 4 теста
- [ ] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] 6. Коммит — шаблон и случай спеки вместе

### Приёмка
- [ ] Конфиги стандарта проверены на образцах: чистый образец — `All checks passed!` и `2 files already formatted`, нарушение — `F401`, порог — `FAIL Required test coverage`
- [ ] Крайние случаи обработаны: кириллица, `print`, `subprocess` с `git`, `assert` в тестах проходят; блок `__main__` вне подсчёта (57.14%, а не меньше)
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` — `BUILD SUCCESSFUL`

## PR 3: `setup` ставит конфиги Python в проект с `*.py`
**Объём:** `Git.kt`, `GitSpec.kt`, `setup/PythonConfig.kt` со спекой и данными, `SetupMain.kt`, `templates/project/ruff.toml`, `templates/project/coveragerc`, `templates/project/gitignore`, `.gitignore`; ≈ 10 файлов

### Задача 6: `Git.trackedPaths`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/task/git/Git.kt:196` — после `addedPaths`
- Изменить: `scripts/src/test/kotlin/io/github/nakulov/standards/task/git/GitSpec.kt:184` — после случая «добавленные в ветке пути»

- [ ] 1. Падающий тест: в `GitSpec.kt` после строки 184:

  ```kotlin

          shouldReturnTest(listOf("a.txt", "b.txt"), options = TestOptions(name = "отслеживаемые файлы по шаблону")) {
              Git(shell = ProcessShell(directory = makeCloneWithChanges())).trackedPaths(pattern = "*.txt")
          }
  ```

  `makeCloneWithChanges` (`GitSpecData.kt:137`): `a.txt` и `b.txt` в индексе (`b.txt` удалён только в рабочей папке), `c.txt` не отслеживается.
- [ ] 2. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — падает: `Unresolved reference 'trackedPaths'`
- [ ] 3. Минимальный код: в `Git.kt` после `addedPaths`:

  ```kotlin

      /** Отслеживаемые файлы по шаблону [pattern] (`*` проходит и через папки) — по ним `setup` узнаёт язык проекта. */
      fun trackedPaths(pattern: String): List<String> {
          return read(args = listOf("ls-files", "--", pattern))
              .lines()
              .filter { line -> line.isNotBlank() }
      }
  ```

- [ ] 4. Запустить `./gradlew :scripts:test --tests '*GitSpec*'` — `BUILD SUCCESSFUL`
- [ ] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] 6. Коммит — метод и случай вместе

### Задача 7: шаг `PythonConfig`
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PythonConfig.kt` — шаг
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PythonConfigSpec.kt` — случаи
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PythonConfigSpecData.kt` — шаблоны плагина и находки
- Создать: `templates/project/ruff.toml`, `templates/project/coveragerc` — заготовки

- [ ] 1. Падающий тест. Создать `PythonConfigSpecData.kt`:

  ```kotlin
  package io.github.nakulov.standards.setup

  import java.io.File

  private const val AREA = "python"
  const val PYTHON_PATTERN = "*.py"
  const val STANDARD_RUFF = "стандарт ruff\n"
  const val STANDARD_PYTEST = "[pytest]\n"
  const val RUFF_SEED = "extend = \"ruff-standard.toml\"\n"
  const val COVERAGE_SEED = "[report]\nfail_under = 0\n"
  const val PROJECT_RUFF = "extend = \"ruff-standard.toml\"\nsrc = [\"tools\"]\n"

  /** Корень плагина с шаблонами Python: две копии стандарта и две заготовки проекта. */
  fun makePythonPluginRoot(): File {
      val plugin = makeProjectRoot()
      writeProjectFile(root = plugin, path = "templates/project/ruff-standard.toml", text = STANDARD_RUFF)
      writeProjectFile(root = plugin, path = "templates/project/pytest.ini", text = STANDARD_PYTEST)
      writeProjectFile(root = plugin, path = "templates/project/ruff.toml", text = RUFF_SEED)
      writeProjectFile(root = plugin, path = "templates/project/coveragerc", text = COVERAGE_SEED)
      return plugin
  }

  fun makePythonPaths(): List<String> {
      return listOf("tools/check.py")
  }

  fun makeNoPythonFinding(): Finding {
      return Finding(status = Finding.Status.OK, area = AREA, text = "файлов Python нет — конфиги не нужны")
  }

  fun makeMissingPythonFindings(): List<Finding> {
      return listOf(
          Finding(status = Finding.Status.DEVIATION, area = AREA, text = "ruff-standard.toml: нет"),
          Finding(status = Finding.Status.DEVIATION, area = AREA, text = "pytest.ini: нет"),
          Finding(status = Finding.Status.DEVIATION, area = AREA, text = "ruff.toml: нет"),
          Finding(status = Finding.Status.DEVIATION, area = AREA, text = ".coveragerc: нет"),
      )
  }

  fun makeWrittenPythonFindings(): List<Finding> {
      return listOf(
          Finding(
              status = Finding.Status.FIXED,
              area = AREA,
              text = "ruff-standard.toml записан (нет) — закоммитьте через задачу",
          ),
          Finding(
              status = Finding.Status.FIXED,
              area = AREA,
              text = "pytest.ini записан (нет) — закоммитьте через задачу",
          ),
          Finding(
              status = Finding.Status.FIXED,
              area = AREA,
              text = "ruff.toml создан по заготовке — дальше его ведёт проект; закоммитьте через задачу",
          ),
          Finding(
              status = Finding.Status.FIXED,
              area = AREA,
              text = ".coveragerc создан по заготовке — дальше его ведёт проект; закоммитьте через задачу",
          ),
      )
  }

  fun makePythonFiles(): Map<String, String> {
      return mapOf(
          "ruff-standard.toml" to STANDARD_RUFF,
          "pytest.ini" to STANDARD_PYTEST,
          "ruff.toml" to RUFF_SEED,
          ".coveragerc" to COVERAGE_SEED,
      )
  }

  fun makePythonFilesWithProjectRuff(): Map<String, String> {
      return mapOf(
          "ruff-standard.toml" to STANDARD_RUFF,
          "pytest.ini" to STANDARD_PYTEST,
          "ruff.toml" to PROJECT_RUFF,
          ".coveragerc" to COVERAGE_SEED,
      )
  }

  fun makePythonDeviation(text: String): Finding {
      return Finding(status = Finding.Status.DEVIATION, area = AREA, text = text)
  }

  fun makePythonOk(): Finding {
      return Finding(status = Finding.Status.OK, area = AREA, text = "конфиги Python по стандарту")
  }
  ```

  Создать `PythonConfigSpec.kt`:

  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.kotestkit.TestOptions
  import io.github.nakulov.kotestkit.case
  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.withGivens
  import io.github.nakulov.standards.task.git.Git
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.mockk.every
  import io.mockk.mockk

  class PythonConfigSpec : BehaviorSpec({
      isolationMode = IsolationMode.InstancePerRoot

      val git = mockk<Git>()
      val root = makeProjectRoot()
      every { git.root() } returns root.path
      every { git.trackedPaths(PYTHON_PATTERN) } returns makePythonPaths()
      val step = PythonConfig(git = git, pluginRoot = makePythonPluginRoot())

      shouldReturnTest(listOf(makeNoPythonFinding()), options = TestOptions(name = "нет файлов Python — находка")) {
          every { git.trackedPaths(PYTHON_PATTERN) } returns emptyList()
          step.run(makeCheckMode())
      }

      shouldReturnTest(emptyMap<String, String>(), options = TestOptions(name = "нет файлов Python — файлы не пишутся")) {
          every { git.trackedPaths(PYTHON_PATTERN) } returns emptyList()
          step.run(makeApplyMode())
          readProjectFiles(root)
      }

      shouldReturnTest(makeMissingPythonFindings(), options = TestOptions(name = "проект без конфигов Python")) {
          step.run(makeCheckMode())
      }

      shouldReturnTest(makeWrittenPythonFindings(), options = TestOptions(name = "setup пишет копии и заготовки")) {
          step.run(makeApplyMode())
      }

      shouldReturnTest(makePythonFiles(), options = TestOptions(name = "содержимое копий и заготовок")) {
          step.run(makeApplyMode())
          readProjectFiles(root)
      }

      shouldReturnTest(makePythonFilesWithProjectRuff(), options = TestOptions(name = "ruff.toml проекта не трогается")) {
          writeProjectFile(root, path = "ruff.toml", text = PROJECT_RUFF)
          step.run(makeApplyMode())
          readProjectFiles(root)
      }

      shouldReturnTest(listOf(makePythonOk()), options = TestOptions(name = "повторный запуск — по стандарту")) {
          step.run(makeApplyMode())
          step.run(makeCheckMode())
      }

      withGivens(
          nameFn = { given -> given.arg3 },
          case(
              arg1 = "ruff-standard.toml",
              arg2 = "старый стандарт\n",
              arg3 = "ruff-standard.toml: отличается от стандарта",
          ),
          case(
              arg1 = "ruff.toml",
              arg2 = "line-length = 100\n",
              arg3 = "ruff.toml: нет строки extend = \"ruff-standard.toml\" — стандарт не действует",
          ),
          case(
              arg1 = ".ruff.toml",
              arg2 = "line-length = 100\n",
              arg3 = ".ruff.toml: ruff читает его вместо ruff.toml — перенесите настройки в ruff.toml",
          ),
          case(
              arg1 = "pyproject.toml",
              arg2 = "[tool.ruff]\nline-length = 100\n",
              arg3 = "pyproject.toml: [tool.ruff] не действует рядом с ruff.toml — перенесите настройки в ruff.toml",
          ),
          case(
              arg1 = "pyproject.toml",
              arg2 = "[tool.pytest.ini_options]\naddopts = \"-x\"\n",
              arg3 = "pyproject.toml: [tool.pytest] не действует рядом с pytest.ini — " +
                  "настройки pytest задаёт стандарт, уберите раздел",
          ),
          case(
              arg1 = "pyproject.toml",
              arg2 = "[tool.coverage.report]\nfail_under = 90\n",
              arg3 = "pyproject.toml: [tool.coverage] не действует рядом с .coveragerc — " +
                  "перенесите настройки в .coveragerc",
          ),
      ) { path, text, expected ->
          shouldReturnTest(listOf(makePythonDeviation(expected))) {
              step.run(makeApplyMode())
              writeProjectFile(root, path, text)
              step.run(makeCheckMode())
          }
      }
  })
  ```

- [ ] 2. Запустить `./gradlew :scripts:test --tests '*PythonConfigSpec*'` — падает: `Unresolved reference 'PythonConfig'`
- [ ] 3. Минимальный код. Создать `PythonConfig.kt`:

  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.standards.task.git.Git
  import java.io.File

  private const val AREA = "python"
  private const val PYTHON_FILES = "*.py"
  private const val RUFF_CONFIG = "ruff.toml"
  private const val RUFF_DOTFILE = ".ruff.toml"
  private const val PYTEST_CONFIG = "pytest.ini"
  private const val COVERAGE_CONFIG = ".coveragerc"
  private const val PYPROJECT = "pyproject.toml"
  private const val EXTEND_LINE = "extend = \"ruff-standard.toml\""
  private const val TOOL_RUFF = "[tool.ruff"
  private const val TOOL_PYTEST = "[tool.pytest"
  private const val TOOL_COVERAGE = "[tool.coverage"

  // Копии стандарта: проект их не правит, поэтому setup перезаписывает их при расхождении.
  private val COPIES = listOf(
      "templates/project/ruff-standard.toml" to "ruff-standard.toml",
      "templates/project/pytest.ini" to PYTEST_CONFIG,
  )

  // Заготовки проекта: в них настройки и порог покрытия проекта, поэтому setup создаёт их один раз и не трогает.
  private val SEEDS = listOf(
      "templates/project/ruff.toml" to RUFF_CONFIG,
      "templates/project/coveragerc" to COVERAGE_CONFIG,
  )

  /**
   * Конфиги проверок Python — только в проекте, где git отслеживает файлы `*.py`. Копии стандарта сверяются, как в
   * [StandardFiles]; заготовки создаются, если их нет. Настройки ruff мимо `ruff.toml` — отклонение с советом: ruff
   * их не читает или читает вместо стандарта.
   */
  class PythonConfig(
      private val git: Git,
      private val pluginRoot: File,
  ) : Setup.Step {

      override fun run(mode: Setup.Mode): List<Finding> {
          val pythonFiles = git.trackedPaths(PYTHON_FILES)
          if (pythonFiles.isEmpty()) {
              val text = "файлов Python нет — конфиги не нужны"
              return listOf(Finding(status = Finding.Status.OK, area = AREA, text = text))
          }
          val root = File(git.root())
          val findings = produceCopyFindings(root, mode) + produceSeedFindings(root, mode) + findConfigDeviations(root)
          return findings.ifEmpty {
              listOf(Finding(status = Finding.Status.OK, area = AREA, text = "конфиги Python по стандарту"))
          }
      }

      private fun produceCopyFindings(
          root: File,
          mode: Setup.Mode,
      ): List<Finding> {
          val differing = COPIES.filter { (source, target) ->
              val file = File(root, target)
              !file.isFile || file.readText() != File(pluginRoot, source).readText()
          }
          return differing.map { (source, target) ->
              val file = File(root, target)
              val state = if (file.isFile) "отличается от стандарта" else "нет"
              if (mode.apply) {
                  file.writeText(File(pluginRoot, source).readText())
                  val text = "$target записан ($state) — закоммитьте через задачу"
                  Finding(status = Finding.Status.FIXED, area = AREA, text = text)
              } else {
                  Finding(status = Finding.Status.DEVIATION, area = AREA, text = "$target: $state")
              }
          }
      }

      private fun produceSeedFindings(
          root: File,
          mode: Setup.Mode,
      ): List<Finding> {
          val missing = SEEDS.filter { (_, target) -> !File(root, target).isFile }
          return missing.map { (source, target) ->
              if (mode.apply) {
                  File(root, target).writeText(File(pluginRoot, source).readText())
                  val text = "$target создан по заготовке — дальше его ведёт проект; закоммитьте через задачу"
                  Finding(status = Finding.Status.FIXED, area = AREA, text = text)
              } else {
                  Finding(status = Finding.Status.DEVIATION, area = AREA, text = "$target: нет")
              }
          }
      }

      private fun findConfigDeviations(root: File): List<Finding> {
          val texts = mutableListOf<String>()
          val ruffConfig = File(root, RUFF_CONFIG)
          if (ruffConfig.isFile && EXTEND_LINE !in readTrimmedLines(ruffConfig)) {
              texts += "$RUFF_CONFIG: нет строки $EXTEND_LINE — стандарт не действует"
          }
          if (File(root, RUFF_DOTFILE).isFile) {
              texts += "$RUFF_DOTFILE: ruff читает его вместо $RUFF_CONFIG — перенесите настройки в $RUFF_CONFIG"
          }
          // Ruff, pytest и coverage берут первый найденный конфиг: рядом с ruff.toml, pytest.ini и .coveragerc
          // разделы pyproject.toml молча не действуют.
          val pyproject = readTrimmedLines(File(root, PYPROJECT))
          if (pyproject.any { line -> line.startsWith(prefix = TOOL_RUFF) }) {
              texts += "$PYPROJECT: [tool.ruff] не действует рядом с $RUFF_CONFIG — перенесите настройки в $RUFF_CONFIG"
          }
          if (pyproject.any { line -> line.startsWith(prefix = TOOL_PYTEST) }) {
              texts += "$PYPROJECT: [tool.pytest] не действует рядом с $PYTEST_CONFIG — " +
                  "настройки pytest задаёт стандарт, уберите раздел"
          }
          if (pyproject.any { line -> line.startsWith(prefix = TOOL_COVERAGE) }) {
              texts += "$PYPROJECT: [tool.coverage] не действует рядом с $COVERAGE_CONFIG — " +
                  "перенесите настройки в $COVERAGE_CONFIG"
          }
          return texts.map { text -> Finding(status = Finding.Status.DEVIATION, area = AREA, text = text) }
      }

      private fun readTrimmedLines(file: File): List<String> {
          if (!file.isFile) {
              return emptyList()
          }
          return file.readLines()
              .map { line -> line.trim() }
      }
  }
  ```

  Создать `templates/project/ruff.toml`:

  ```toml
  # Конфиг ruff проекта: setup создаёт его один раз, дальше его ведёт проект.
  # Стандарт — в ruff-standard.toml: копия, её сверяет setup --check.
  # Здесь — пути проекта и более строгие настройки; ignore не добавляется (PY-LINT-8), исключение — noqa с причиной.
  extend = "ruff-standard.toml"
  ```

  Создать `templates/project/coveragerc`:

  ```ini
  # Конфиг покрытия проекта: setup создаёт его один раз, дальше его ведёт проект.
  # fail_under — замер при подключении с округлением вниз; поднимается, не снижается (PY-TEST-2).
  [run]
  source = .
  omit = tests/*

  [report]
  fail_under = 0
  # Скрипты в папках без __init__.py (PY-LINT-3): без этой строки непокрытый скрипт не попадает в отчёт.
  include_namespace_packages = True
  exclude_also =
      if __name__ == .__main__.:
  ```

- [ ] 4. Запустить `./gradlew :scripts:test --tests '*PythonConfigSpec*'` — `BUILD SUCCESSFUL`, 13 тестов
- [ ] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`; падение порога покрытия — найти непокрытую ветку в `build/reports/kover/html` и добавить случай, порог не снижать
- [ ] 6. Коммит — шаг, спека, данные и заготовки вместе

### Задача 8: шаг в `setup` и `.coverage` в `.gitignore`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:27` — шаг в списке
- Изменить: `templates/project/gitignore:11`, `.gitignore:12` — `.coverage`

Тест: не применим — точка входа `SetupMainKt` исключена из покрытия (`scripts/build.gradle.kts`, KT-TEST-7), строка `.gitignore` — текст шаблона; проверка — «После завершения».

- [ ] 1. Тест: не применим — см. выше
- [ ] 2. Запуск: не применим
- [ ] 3. Код. В `SetupMain.kt` после строки 27:

  ```kotlin
              PythonConfig(git = git, pluginRoot = pluginRoot),
  ```

  В `templates/project/gitignore` и в блоке `.gitignore` репозитория после `__pycache__/`:

  ```gitignore
  .coverage
  ```
- [ ] 4. Запустить `./gradlew :scripts:test` — `BUILD SUCCESSFUL`
- [ ] 5. Запустить `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] 6. Коммит — шаг и строка `.gitignore` вместе

### Приёмка
- [ ] `setup` ставит конфиги Python в проект с `*.py` — проверено спекой `PythonConfigSpec`
- [ ] Крайние случаи обработаны: нет `*.py` — ничего не пишется; `ruff.toml` проекта не перезаписывается; без `extend`, `.ruff.toml`, `[tool.ruff]`, `[tool.pytest]`, `[tool.coverage]` — отклонения с советом
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:check` — `BUILD SUCCESSFUL`
- [ ] Последний блок: критерии цели из «Обзора» — по пунктам: правила `PY-…` и BANK-6/7 (PR 1), `setup` ставит четыре файла только в проект с `*.py` (PR 3), спеки шаблонов на образцах зелёные (PR 2)
- [ ] Документация: `docs/code-style/python/README.md`, раздел «Подключение» — совпадает с тем, что делает `PythonConfig`

## Открыто
- нет

## После завершения
- Ручная проверка: в проекте с `*.py` (например, пустой репозиторий с одним `tools/check.py` в индексе) — `setup --check` → четыре отклонения в области `python`; `setup` → «записан» и «создан по заготовке»; повторный `setup --check` → «[ok] python: конфиги Python по стандарту»; `ruff check` и `pytest` из корня проекта запускаются с конфигами стандарта.
