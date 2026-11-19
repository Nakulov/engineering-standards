# Каркас плагина и подключение к проекту

**Цель:** репозиторий становится плагином Claude Code: скиллы, агенты, `rules/` и `task` приходят в сессию из плагина, а `setup` подключает плагин к проекту — пишет `settings.json` и ставит обёртку `task` для терминала.
**Архитектура:** весь репозиторий — плагин (`source: "./"`, #3). Хук `SessionStart` (bash) дописывает `scripts/` в PATH через `CLAUDE_ENV_FILE`, записывает корень плагина для обёртки и печатает `rules/`. Два новых шага `setup`: «подключение» пишет `.claude/settings.json` (свой разбор JSON без библиотек), «обёртка» ставит `~/.local/bin/task`. Здесь плагин подключается через `claude --plugin-dir .`.
**Стек:** bash 3.2 (хук, обёртка); Kotlin ≥ 2.2 без зависимостей (шаги `setup`); тесты — Kotest + KotestKit, mockk, bash-скрипты запускаются через `ProcessBuilder`.

---

## Ограничения
- Скиллы и агенты переносим из `.claude/` как есть, без правки текста (#43, решение 1): тексты правят #44, #46, #48.
- Kotlin без библиотек: запускатель компилирует голым `kotlinc` (`scripts/task:35`, #13). Python не берём (#3).
- Хук и обёртка — bash, совместимый с 3.2 (`scripts/task:3`). Хук без Kotlin: компиляция заняла бы ≈ 2 с на каждом старте (решение 9).
- Не трогать: `drafts/variants/` и строку `/drafts/` в `.git/info/exclude` — они нужны для #60.
- Не включать плагин в `settings.json` этого репозитория: здесь он подключается через `--plugin-dir` (решение 2), иначе загрузится дважды.
- Не ставить `version` в `plugin.json` до #23: версией служит коммит (решение 8).
- Репозиторий публичный: в `owner` и `author` — только ник `Nakulov`, без почты.
- Ветки, коммиты и PR — только командами `task` (`task commit --message-file`, `task pr --part`, `task final`).
<!-- Упёрся в ограничение — остановись, новый круг обсуждения с пользователем. -->

## Обзор
- **Что реализуется:**
  - `.claude-plugin/` — манифесты плагина и маркетплейса;
  - хук `SessionStart` и `rules/`;
  - перенос 4 скиллов и 5 агентов;
  - шаги `setup` «подключение» (`.claude/settings.json`) и «обёртка» (`~/.local/bin/task`);
  - строки `plans/done/` и `plans/journal/` в общем блоке `.gitignore`;
  - раздел README «Подключение».
- **Какую проблему решает:** скиллам эпика #4 некуда лечь, проект их не получит, а `drafts/` приходится копировать в `.claude/` вручную. После задачи #44, #46 и #48 правят файлы уже в плагине.
- **Как встраивается:**
  - проект получает плагин из маркетплейса GitHub по `settings.json`, который пишет `setup`;
  - скиллы зовут голое `task …` — его находит PATH из хука;
  - из терминала `task` вызывает обёртка `~/.local/bin/task`.
- **Критерии цели:**
  - здесь `claude --plugin-dir .` подгружает `rules/` в контекст, скиллы и агенты видны;
  - `task --help` работает из сессии Claude и из терминала (`~/.local/bin/task`);
  - `scripts/setup --check` в этом репозитории проходит: код 0;
  - `./gradlew :scripts:test` зелёный: спеки покрывают JSON, «подключение», «обёртку», хук и скрипт обёртки;
  - из `.git/info/exclude` убраны `/.claude/skills/`, `/.claude/agents/`, `/plans/done/`, `/plans/journal/` и `drafts/pilot-53.md`, если он там есть; `/drafts/` остаётся до #60.

## Контекст
- **Задействованные файлы:**
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:21-35` — список шагов `setup`, файлы первого коммита у `InitialBranch` (`:27`);
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/Setup.kt:19` — `fun interface Step { fun run(mode: Mode): List<Finding> }`; `TaskError` → код 2, остальные шаги не выполняются;
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/Finding.kt` — статусы `OK`, `FIXED`, `DEVIATION`, `NEEDS_CONFIRM`, `NOTE`;
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/GitignoreBlock.kt` — образец шага с файлом в корне git (`git.root()`), текст «закоммитьте через задачу»;
  - `scripts/src/main/kotlin/io/github/nakulov/standards/setup/Labels.kt:126` — образец разбора без библиотеки, ошибка `TaskError.Failure("$file: …")`;
  - `scripts/task:44` — `TASK_PLUGIN_ROOT` = папка над `scripts/`; кеш — `${XDG_CACHE_HOME:-$HOME/.cache}/engineering-standards`;
  - `scripts/build.gradle.kts:28` — системные свойства тестов: `task.repoRoot`, `task.cache`;
  - `.claude/skills/{groom,plan,execute,worktree}/SKILL.md`, `.claude/agents/*.md` (5) — текущие скиллы и агенты, не в git;
  - `templates/project/gitignore` — общий блок `.gitignore`;
  - `README.md:39` — раздел «Подключение».
- **Паттерны:**
  - спеки — `BehaviorSpec` + `shouldReturnTest` / `shouldThrowWithMessageTest`, данные в `…SpecData.kt`, `isolationMode = IsolationMode.InstancePerRoot`;
  - временные папки — `makeProjectRoot()` (`GitignoreBlockSpecData.kt:7`);
  - режимы — `makeCheckMode()`, `makeApplyMode()`, `makeConfirmedMode()` (`SetupSpecData.kt:5-13`);
  - bash через процесс — `TaskLauncherSpec.kt` и `LauncherRun` (`TaskLauncherSpecData.kt:6`);
  - именованные аргументы везде.
- **Зависимости:** `git rev-parse --path-format=absolute` (git ≥ 2.31); Claude Code 2.1.327: `claude plugin validate`, `claude plugin marketplace`, `claude plugin install/update`.

## Подход к разработке
- Завершай задачу целиком до следующей.
- Небольшие сфокусированные изменения.
- **ВАЖНО:** каждая задача — с новыми или обновлёнными тестами для её кода.
- **ВАЖНО:** все тесты зелёные до начала следующей задачи.
- **ВАЖНО:** изменение объёма — в этот план, не молча.
- После каждого изменения — конкретный тест; полный набор — в конце задачи.
- Обратная совместимость: нужна. `setup` в существующем проекте не трогает чужие ключи `settings.json` и чужой `~/.local/bin/task`; запуск `scripts/task` по пути работает как раньше.

## Структура файлов

Плагин загружается:
- Создать: `.claude-plugin/plugin.json` — манифест плагина: имя, автор
- Создать: `.claude-plugin/marketplace.json` — маркетплейс из одного плагина `source: "./"`
- Создать: `hooks/hooks.json` — `SessionStart` на все источники → `hooks/session-start`
- Создать: `hooks/session-start` — PATH, корень плагина для обёртки, `rules/` в контекст
- Создать: `rules/answers.md`, `rules/language.md`, `rules/no-pr-without-issue.md` — по одному правилу на файл
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/SessionStartHookSpec.kt`, `SessionStartHookSpecData.kt` — хук на временной папке плагина

Скиллы и агенты:
- Создать: `skills/{groom,plan,execute,worktree}/SKILL.md` — перенос из `.claude/skills/` как есть
- Создать: `agents/{implementation-reviewer,plan-executor,plan-reviewer,standards-reviewer,test-reviewer}.md` — перенос из `.claude/agents/` как есть

`setup` пишет `settings.json`:
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/JsonValue.kt` — значение JSON и его текст; вход: `JsonValue.toText(): String`
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/JsonParser.kt` — текст → `JsonValue`; вход: `JsonParser(text, file).parse(): JsonValue`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/JsonSpec.kt`, `JsonSpecData.kt` — разбор и печать
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PluginSettings.kt` — шаг «подключение»; вход: `PluginSettings(git, pluginRoot)`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PluginSettingsSpec.kt`, `PluginSettingsSpecData.kt` — случаи `settings.json`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:26-27` — шаг «подключение» после `GitignoreBlock`, `.claude/settings.json` в первом коммите
- Создать: `.claude/settings.json` — разрешение `Bash(task:*)` этого репозитория, пишет `setup`

Обёртка `task` для терминала:
- Создать: `scripts/task-wrapper` — источник `~/.local/bin/task`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/TaskWrapperScriptSpec.kt`, `TaskWrapperScriptSpecData.kt` — скрипт обёртки процессом
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/TaskWrapper.kt` — шаг «обёртка»; вход: `TaskWrapper(home, pluginRoot, path)`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/TaskWrapperSpec.kt`, `TaskWrapperSpecData.kt` — установка с временным `HOME`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:34` — шаг «обёртка» последним

`.gitignore` и README:
- Изменить: `templates/project/gitignore` — `plans/done/`, `plans/journal/`
- Изменить: `.gitignore` — общий блок по шаблону, пишет `setup`
- Изменить: `README.md:39` — раздел «Подключение»: `setup`, обёртка, алиас `--plugin-dir`

## Прогресс
- `[x]` — сразу после завершения; ➕ — новая задача (только с согласия пользователя); ⚠️ — проблема или блокер.
- Отклонение от объёма — обнови план. Всё — не молча: сообщай пользователю.
- План синхронен с фактической работой.
- Итоги опытов задачи 1 — строкой на опыт ниже, до начала задачи 2.
- Итоги опытов задачи 1:
  - опыт 1: ок — `es-exp task ping` из PATH хука; без правила `Bash(task:*)` команда не выполняется, нужен запрос подтверждения.
  - опыт 2а: ок — субагент видит PATH из хука (`es-exp task probe`).
  - опыт 2б: ок — после `/clear` `task ping` отработал, запроса не было, в логе есть `"source":"clear"` (и `startup`). Оговорка: первый запрос до `/clear` сессия обработала скиллом `ping` (ответ `pong-v2`), а не командой `task`; PATH при старте подтверждён опытом 1.
  - опыт 3: ок — `CLAUDE_PROJECT_DIR` задан (путь с префиксом `/private`), `CLAUDE_ENV_FILE` задан.
  - опыт 6: ок — работают обе формы `/ping` и `/es-exp:ping`; в README можно писать короткое `/ping`.
  - опыт 4: ок — после `marketplace update` и `plugin update` без `version` ответ `pong-v2`; в кеше две папки версий.
  - опыт 5: плагин включён и подключён через `--plugin-dir` — хук сработал 1 раз, ответила v2.

## Обзор решения
- **Подход и архитектура:** каркас и перенос текущих скиллов и агентов как есть; весь репозиторий — плагин (#3). Здесь работаем через `claude --plugin-dir .`, проекты подключаются через маркетплейс GitHub и `setup` (#43, решения 1–10).
- **Решения:**
  - `task` попадает в PATH через `CLAUDE_ENV_FILE`, а скиллы пишут голое `task` — так тексты не нужно править (решение 4).
  - Разрешение `Bash(task:*)` — в `settings.json` проекта, им владеет стандарт: без него на каждый `task` приходил бы запрос (решение 5).
  - Обёртка — одна на машину, в `~/.local/bin/task`. Корень плагина она берёт из записи хука, а не из внутреннего формата Claude Code (решение 6, секция 2).
  - Свой JSON на Kotlin: у запускателя нет зависимостей (секция 2).
  - Хук на bash: старт сессии без компиляции (решение 9).
  - Изменено при плане: «ожидания `GitignoreBlock` и `InitialBranch`» из секции 5 груминга не правим. Их спеки берут блок и список файлов из своих данных (`GitignoreBlockSpec.kt:18`, `InitialBranchSpec.kt:24`), а не из шаблона и `SetupMain` — правка данных ничего бы не проверила. Проверка — `scripts/setup --check` (задачи 8 и 11).
- **Как вписывается в систему:** `setup` получает 2 шага в общем списке: «подключение» после файлов, «обёртка» последним — это шаг машины. Хук и обёртка — тонкий bash над `scripts/task`.
- **Запасной:** скиллы вызывают `task` по полному пути `${CLAUDE_PLUGIN_ROOT}/scripts/task` (вывод #3). Переходим, если опыт 1 или 2 провалится — решает пользователь до PR 1.
- **Запасные по опытам:**
  - `version: "0.1.0"` в `plugin.json` — если опыт 4 покажет, что `/plugin update` без версии не берёт новый коммит;
  - `git rev-parse` из `$PWD` вместо `CLAUDE_PROJECT_DIR` — уже заложено в хуке: `${CLAUDE_PROJECT_DIR:-$PWD}`.
- **Отклонено:**
  - пустой каркас — `drafts/` и ручное копирование остались бы до каждой задачи;
  - перенос без подключения — проект не получит плагин;
  - установка из маркетплейса этого репозитория — правку видно только после `/plugin update`;
  - ссылка `.claude/skills → ../skills` — без хука, `rules/` и `${CLAUDE_PLUGIN_ROOT}`;
  - `${CLAUDE_PLUGIN_ROOT}/scripts/task` в текстах сразу — правило с версией в пути и правка ≈ 9 файлов;
  - не ставить разрешение — запрос на каждый `task`;
  - хук `PreToolUse` — пришлось бы разбирать составные команды;
  - `bin/task` в каждом проекте — лишний файл;
  - функция в `~/.zshrc` — хрупкая, без обновлений;
  - `kotlinx-serialization` — зависимость;
  - `python3` — второй язык;
  - `plutil` по `installed_plugins.json` — только macOS и внутренний формат;
  - скрыть всю `plans/` — `task plan` пришлось бы добавлять файлы с `-f`.

## Технические детали
- **`.claude/settings.json` по стандарту.** Ключи стандарта: `extraKnownMarketplaces.engineering-standards = {"source": {"source": "github", "repo": "Nakulov/engineering-standards"}}`, `enabledPlugins["engineering-standards@engineering-standards"] = true` и `"Bash(task:*)"` в `permissions.allow`. Новые ключи дописываются в конец объекта, существующие сохраняют место. Отступ — 2. Перевод строки в конце файла.
- **Репозиторий стандарта** определяется так: `pluginRoot.canonicalFile == File(git.root()).canonicalFile`. Здесь пишется только разрешение; маркетплейс и `enabledPlugins` — `NOTE`.
- **Корень плагина для обёртки.** Файл `${XDG_CACHE_HOME:-$HOME/.cache}/engineering-standards/roots/<ключ>`. Ключ — основная папка репозитория (`dirname` от `git rev-parse --path-format=absolute --git-common-dir`), в которой `/` заменены на `-`. Ворктри попадают в тот же ключ. Содержимое — путь к корню плагина и перевод строки.
- **Метка обёртки** — вторая строка `# engineering-standards task-wrapper`. По ней `setup` отличает свою обёртку от чужой.
- **Поток:**
  - сессия: хук → строка PATH в `CLAUDE_ENV_FILE` → запись корня → `rules/` в stdout; скилл → `task` → `<корень>/scripts/task`;
  - терминал: `~/.local/bin/task` → репозиторий стандарта? `<корень git>/scripts/task` : корень из записи;
  - `setup`: `StandardFiles` → `GitignoreBlock` → `PluginSettings` → `InitialBranch` → `RepositorySettings` → `Rulesets` → `Labels` → `BoardSetup` → `TaskWrapper`.

## Состав работ
**Шаги реализации** (в этой кодовой базе):
- PR 1: плагин загружается здесь
  - [x] Задача 1: опыты до кода
  - [x] Задача 2: манифесты плагина и маркетплейса
  - [x] Задача 3: правила `rules/`
  - [x] Задача 4: хук `SessionStart`
  - [x] Приёмка
- PR 2: скиллы и агенты из плагина
  - [x] Задача 5: перенос скиллов и агентов
  - [x] Приёмка
- PR 3: `setup` пишет `settings.json`
  - [x] Задача 6: разбор и печать JSON
  - [x] Задача 7: шаг «подключение»
  - [x] Задача 8: `settings.json` этого репозитория
  - [x] Приёмка
- PR 4: обёртка `task` для терминала
  - [x] Задача 9: скрипт обёртки
  - [x] Задача 10: шаг «обёртка»
  - [ ] Приёмка
- PR 5: `.gitignore` и README
  - [ ] Задача 11: `plans/done/` и `plans/journal/` в общем блоке
  - [ ] Задача 12: раздел README «Подключение»
  - [ ] Приёмка

**После завершения** (внешнее, без чекбоксов):
- Почистить `.git/info/exclude`, удалить локальные `.claude/skills/`, `.claude/agents/`, `drafts/` кроме `drafts/variants/`.
- Удалить из памяти ассистента дубли `rules/`: `feedback-answer-scope`, `feedback-language`, `feedback-no-pr-without-issue` — с подтверждения пользователя (решение 10).
- Живая проверка: `claude --plugin-dir .`, `task --help` из сессии и из терминала.

## PR 1: плагин загружается здесь
**Объём:** `.claude-plugin/{plugin,marketplace}.json`, `hooks/hooks.json`, `hooks/session-start`, `rules/` (3), `SessionStartHookSpec.kt`, `SessionStartHookSpecData.kt`; ≈ 9 файлов

### Задача 1: опыты до кода
Файлов в репозитории нет: опыты идут на временном плагине `es-exp`, итог — строкой на опыт в «Прогресс». Шаги с живой сессией (`/clear`) выполняет пользователь — исполнитель просит его и ждёт ответа. Любой провал — ⚠️ в «Прогресс», остановка до задачи 2: запасной вариант утверждает пользователь.

Тест: не применим — опыт и есть проверка; ожидаемый результат — у каждого шага.

- [x] 1. Подготовить временный плагин и проект:
  ```bash
  EXP=$(mktemp -d)/es-exp; P=$(mktemp -d)/es-project
  mkdir -p "$EXP/.claude-plugin" "$EXP/hooks" "$EXP/scripts" "$EXP/skills/ping" "$EXP/agents" "$P/.claude"
  cat >"$EXP/.claude-plugin/plugin.json" <<'EOF'
  {"name": "es-exp"}
  EOF
  cat >"$EXP/.claude-plugin/marketplace.json" <<'EOF'
  {"name": "es-exp", "owner": {"name": "test"}, "plugins": [{"name": "es-exp", "source": "./"}]}
  EOF
  cat >"$EXP/hooks/hooks.json" <<'EOF'
  {"hooks": {"SessionStart": [{"matcher": "startup|resume|clear|compact",
    "hooks": [{"type": "command", "command": "\"${CLAUDE_PLUGIN_ROOT}/hooks/start\""}]}]}}
  EOF
  cat >"$EXP/hooks/start" <<'EOF'
  #!/bin/bash
  root=$(cd "$(dirname "$0")/.." && pwd)
  echo "$(date +%s) $(tr -d '\n' | grep -o '"source":"[a-z]*"') project=${CLAUDE_PROJECT_DIR:-нет} env=${CLAUDE_ENV_FILE:-нет} root=$root" >>"$HOME/.cache/es-exp.log"
  [ -n "${CLAUDE_ENV_FILE:-}" ] && echo "export PATH=\"$root/scripts:\$PATH\"" >>"$CLAUDE_ENV_FILE"
  exit 0
  EOF
  printf '#!/bin/sh\necho "es-exp task $*"\n' >"$EXP/scripts/task"
  printf -- '---\nname: ping\ndescription: Опыт плагина — отвечает pong\n---\nОтветь одним словом: pong-v1\n' >"$EXP/skills/ping/SKILL.md"
  printf -- '---\nname: probe\ndescription: Опыт плагина — запускает task probe\ntools: Bash\n---\nВыполни `task probe` и верни вывод дословно.\n' >"$EXP/agents/probe.md"
  chmod +x "$EXP/hooks/start" "$EXP/scripts/task"
  printf '{"permissions": {"allow": ["Bash(task:*)"]}}\n' >"$P/.claude/settings.json"
  git -C "$P" init -q; mkdir -p ~/.cache; : >~/.cache/es-exp.log
  echo "EXP=$EXP P=$P"
  ```
  Ожидается: строка `EXP=… P=…` — пути нужны в следующих шагах.
- [x] 2. **Опыт 1** (решения 4 и 5): `Bash(task:*)` срабатывает без запроса на голое `task` из PATH хука.
  - Команда: `cd "$P" && claude -p --plugin-dir "$EXP" "Выполни в Bash команду: task ping. Верни её вывод дословно."`
  - Ожидается: в ответе `es-exp task ping`.
  - Контроль: убрать правило (`printf '{}\n' >"$P/.claude/settings.json"`), повторить — запрос отклонён (в `-p` без разрешения инструмент не запускается). Затем вернуть правило.
- [x] 3. **Опыт 2а** (решение 4): субагент видит PATH из хука.
  - Команда: `cd "$P" && claude -p --plugin-dir "$EXP" "Запусти агента probe и верни его ответ дословно."`
  - Ожидается: `es-exp task probe`.
- [x] 4. **Опыт 2б**, пользователь (решение 4): PATH после `/clear`.
  - `cd "$P" && claude --plugin-dir "$EXP"` → попросить «выполни task ping» → `/clear` → снова «выполни task ping».
  - Ожидается: оба раза `es-exp task ping`; в `~/.cache/es-exp.log` есть строка с `"source":"clear"`.
- [x] 5. **Опыт 3** (секция 2): у хука есть `CLAUDE_PROJECT_DIR`.
  - Команда: `grep -c "project=$P" ~/.cache/es-exp.log` (с учётом `/private` на macOS — сверять по концу пути `es-project`).
  - Ожидается: ≥ 1. Провал — не блокер: хук берёт `$PWD` (`${CLAUDE_PROJECT_DIR:-$PWD}`), отметить ⚠️ без остановки.
- [x] 6. **Опыт 6** (README): короткое `/ping` при `--plugin-dir`.
  - Команды: `cd "$P" && claude -p --plugin-dir "$EXP" "/ping"` и `claude -p --plugin-dir "$EXP" "/es-exp:ping"`.
  - Ожидается: в ответе `pong-v1`. Записать, какая форма работает — от неё зависит текст README в задаче 12. Не блокер.
- [x] 7. **Опыт 4** (решение 8): `/plugin update` без `version` берёт новый коммит.
  - Подготовка: `git -C "$EXP" init -q && git -C "$EXP" add -A && git -C "$EXP" -c user.name=t -c user.email=t@t commit -qm v1`.
  - Установка: `cd "$P" && claude plugin marketplace add "$EXP" --scope project && claude plugin install es-exp@es-exp --scope project`.
  - Обновление: заменить `pong-v1` на `pong-v2` в `$EXP/skills/ping/SKILL.md`, закоммитить (`commit -qam v2`), выполнить `claude plugin marketplace update es-exp && claude plugin update es-exp@es-exp --scope project`.
  - Проверка: `claude -p "/es-exp:ping"` — без `--plugin-dir`.
  - Ожидается: `pong-v2`; `ls ~/.claude/plugins/cache/es-exp/es-exp/` показывает папку новой версии.
  - Флаги команд — по `claude plugin <команда> --help`, если отличаются.
  - Оговорка: источник здесь локальная папка; источник `github` проверяет пилот #24.
- [x] 8. **Опыт 5** (решение 2): плагин включён и одновременно подключён через `--plugin-dir`.
  - Команда (после опыта 4, плагин установлен): `: >~/.cache/es-exp.log; cd "$P" && claude -p --plugin-dir "$EXP" "/es-exp:ping"; wc -l <~/.cache/es-exp.log`.
  - Записать: сколько раз сработал хук (1 или 2), какая версия ответила.
  - Решение 2 и так не включает плагин здесь — итог только записываем. Не блокер.
- [x] 9. Убрать за собой: `cd "$P" && claude plugin uninstall es-exp@es-exp --scope project; claude plugin marketplace remove es-exp; rm -rf "$EXP" "$P" ~/.cache/es-exp.log`.
  - Ожидается: `claude plugin list` без `es-exp`.
- [x] 10. Итог в «Прогресс»: строка на опыт — «опыт N: ок / провал — что видно». Коммита нет: файлов в репозитории задача не меняет.

### Задача 2: манифесты плагина и маркетплейса
- Создать: `.claude-plugin/plugin.json` — имя, описание, автор
- Создать: `.claude-plugin/marketplace.json` — маркетплейс из одного плагина `source: "./"`

Тест: не применим — манифест без логики; проверка формата — `claude plugin validate .`.
- [x] 1. Создать `.claude-plugin/plugin.json`:
  ```json
  {
    "name": "engineering-standards",
    "description": "Общие правила, скиллы и скрипты процесса наших проектов",
    "author": {
      "name": "Nakulov"
    },
    "repository": "https://github.com/Nakulov/engineering-standards"
  }
  ```
- [x] 2. Создать `.claude-plugin/marketplace.json`:
  ```json
  {
    "name": "engineering-standards",
    "owner": {
      "name": "Nakulov"
    },
    "plugins": [
      {
        "name": "engineering-standards",
        "source": "./",
        "description": "Общие правила, скиллы и скрипты процесса наших проектов"
      }
    ]
  }
  ```
- [x] 3. Запустить `claude plugin validate .` — проверка пройдена, ошибок нет. Если валидатор предупреждает о `CLAUDE.md` в корне — это не ошибка (memory: проверено 2026-11-13).
- [x] 4. Коммит

### Задача 3: правила `rules/`
- Создать: `rules/answers.md`, `rules/language.md`, `rules/no-pr-without-issue.md` — одно правило на файл, 1–3 строки с причиной и задачей (решение 10)

Тест: не применим — текст без логики; печать `rules/` проверяет спека хука в задаче 4.
- [x] 1. `rules/answers.md`:
  ```markdown
  Вопрос да/нет — короткий прямой ответ: без анализа и дорожных карт, пока их не попросили.
  Причина: сбой 2026-10-21 — на вопрос да/нет пришёл разбор на экран (#43, из #44).
  ```
- [x] 2. `rules/language.md`:
  ```markdown
  Язык — русский везде: ответы, задачи, комментарии, коммиты, заголовки и тела PR. Английский — только в именах веток.
  Причина: основной язык стандарта — русский (#1, #3).
  ```
- [x] 3. `rules/no-pr-without-issue.md`:
  ```markdown
  Нет задачи — нет PR: даже правка в одну строку идёт задача → ветка `task/<N>-<slug>` → PR. Одобрение правки в разговоре задачу не заменяет.
  Причина: решения и их история живут в задачах (#3).
  ```
- [x] 4. Коммит

### Задача 4: хук `SessionStart`
- Создать: `hooks/hooks.json` — `SessionStart` на `startup|resume|clear|compact`
- Создать: `hooks/session-start` — PATH, корень плагина, `rules/`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/SessionStartHookSpec.kt` — случаи хука
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/SessionStartHookSpecData.kt` — временный плагин, проект, запуск

- [x] 1. Падающий тест: PATH один раз, `rules/` по алфавиту, без `CLAUDE_ENV_FILE`, запись корня, кеш недоступен, пустые `rules/`.
  `SessionStartHookSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.standards.task.cli.LauncherRun
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec

  // Вне стандарта: спека проверяет не класс, а bash-хук `hooks/session-start` — запускает его копию процессом во
  // временной папке плагина (#43, решение 9). Подготовка папок и запуск — в SessionStartHookSpecData.
  class SessionStartHookSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val plugin = makeHookPlugin(rules = mapOf("b.md" to "Правило Б\n", "a.md" to "Правило А\n"))
          val bare = makeHookPlugin(rules = emptyMap())
          val project = makeGitProject()
          val envFile = makeEnvFile()
          val cache = makeTempDir(prefix = "cache")

          shouldReturnTest(makePathLine(plugin = plugin)) {
              runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
              runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
              envFile.readText()
          }

          shouldReturnTest(makeRulesRun()) {
              runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
          }

          shouldReturnTest(makeNoEnvFileRun(plugin = bare)) {
              runHook(plugin = bare, project = project, envFile = null, cache = cache)
          }

          shouldReturnTest(plugin.path + "\n") {
              runHook(plugin = plugin, project = project, envFile = envFile, cache = cache)
              readRootRecord(cache = cache, project = project)
          }

          val blocked = makeBlockedCache()
          shouldReturnTest(makeNoCacheRun(cache = blocked)) {
              runHook(plugin = plugin, project = project, envFile = envFile, cache = blocked)
          }

          shouldReturnTest(LauncherRun(code = 0, out = "", err = "")) {
              runHook(plugin = bare, project = project, envFile = envFile, cache = cache)
          }
      })
  ```
  `SessionStartHookSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.standards.task.cli.LauncherRun
  import java.io.File
  import java.nio.file.Files

  /** Пустая папка в кеше тестов модуля. */
  fun makeTempDir(prefix: String): File {
      val base = File(System.getProperty("task.cache"), "plugin")
      base.mkdirs()
      return Files.createTempDirectory(/* dir = */ base.toPath(), /* prefix = */ prefix).toFile()
  }

  /** Вывод `git` в папке [directory]; ненулевой код — ошибка теста. */
  fun runGit(
      directory: File,
      vararg args: String,
  ): String {
      val process = ProcessBuilder(listOf("git") + args).directory(directory).redirectErrorStream(true).start()
      val out = process.inputStream.bufferedReader().readText()
      check(process.waitFor() == 0) { "git ${args.joinToString(separator = " ")}: $out" }
      return out
  }

  fun makeGitProject(): File {
      val project = makeTempDir(prefix = "project")
      runGit(directory = project, "init", "-q")
      return project
  }

  /** Ключ записи корня: основная папка репозитория, `/` → `-` — как в хуке и обёртке. */
  fun makeRootKey(project: File): String {
      val common = runGit(directory = project, "rev-parse", "--path-format=absolute", "--git-common-dir").trim()
      return File(common).parent.replace(oldChar = '/', newChar = '-')
  }

  /** Копия `hooks/session-start` в новой папке плагина и [rules] — имя файла → текст; пусто — без папки `rules/`. */
  fun makeHookPlugin(rules: Map<String, String>): File {
      val plugin = makeTempDir(prefix = "plugin")
      val hook = File(plugin, "hooks/session-start")
      File(System.getProperty("task.repoRoot"), "hooks/session-start").copyTo(target = hook)
      hook.setExecutable(true)
      rules.forEach { (name, text) ->
          val rule = File(plugin, "rules/$name")
          rule.parentFile.mkdirs()
          rule.writeText(text)
      }
      return plugin
  }

  fun makeEnvFile(): File {
      val file = File(makeTempDir(prefix = "env"), "env")
      file.writeText("")
      return file
  }

  /** Кеш — обычный файл: папку записей в нём не создать. */
  fun makeBlockedCache(): File {
      val file = File(makeTempDir(prefix = "blocked"), "cache")
      file.writeText("")
      return file
  }

  fun makePathLine(plugin: File): String {
      return "export PATH=\"${plugin.path}/scripts:\$PATH\"\n"
  }

  fun makeRulesRun(): LauncherRun {
      return LauncherRun(code = 0, out = "Правило А\n\nПравило Б\n\n", err = "")
  }

  fun makeNoEnvFileRun(plugin: File): LauncherRun {
      return LauncherRun(code = 0, out = "`task` не в PATH — вызывай ${plugin.path}/scripts/task\n", err = "")
  }

  fun makeNoCacheRun(cache: File): LauncherRun {
      val err = "engineering-standards: корень плагина не записан в ${cache.path}/engineering-standards/roots\n"
      return LauncherRun(code = 0, out = makeRulesRun().out, err = err)
  }

  fun readRootRecord(
      cache: File,
      project: File,
  ): String {
      return File(cache, "engineering-standards/roots/${makeRootKey(project = project)}").readText()
  }

  /** Запуск хука: [envFile] `null` — без `CLAUDE_ENV_FILE`. */
  fun runHook(
      plugin: File,
      project: File,
      envFile: File?,
      cache: File,
  ): LauncherRun {
      val builder = ProcessBuilder(File(plugin, "hooks/session-start").path).directory(project)
      val environment = builder.environment()
      environment.remove("CLAUDE_ENV_FILE")
      environment["CLAUDE_PROJECT_DIR"] = project.path
      environment["XDG_CACHE_HOME"] = cache.path
      if (envFile != null) {
          environment["CLAUDE_ENV_FILE"] = envFile.path
      }
      val process = builder.start()
      process.outputStream.close()
      val out = process.inputStream.bufferedReader().readText()
      val err = process.errorStream.bufferedReader().readText()
      return LauncherRun(code = process.waitFor(), out = out, err = err)
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*SessionStartHookSpec*'` — падает: `NoSuchFileException: …/hooks/session-start` в `makeHookPlugin`.
- [x] 3. Минимальный код. `hooks/session-start` (исполняемый, `chmod +x`):
  ```bash
  #!/bin/bash
  # Хук SessionStart плагина engineering-standards (#43, решение 9): PATH к scripts/ плагина, корень плагина для обёртки
  # ~/.local/bin/task, rules/ в контекст сессии. Любая ошибка — код 0 и stderr: хук не блокирует сессию.
  # Совместим с bash 3.2 (macOS).
  set -u

  root=$(cd "$(dirname "$0")/.." && pwd)

  if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    line="export PATH=\"$root/scripts:\$PATH\""
    if ! grep -qxF "$line" "$CLAUDE_ENV_FILE" 2>/dev/null; then
      printf '%s\n' "$line" >>"$CLAUDE_ENV_FILE" 2>/dev/null ||
        echo "engineering-standards: PATH не записан в $CLAUDE_ENV_FILE" >&2
    fi
  else
    echo "\`task\` не в PATH — вызывай $root/scripts/task"
  fi

  # Корень плагина для обёртки: ключ — основная папка репозитория, ворктри попадают в тот же ключ.
  common=$(git -C "${CLAUDE_PROJECT_DIR:-$PWD}" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)
  if [ -n "$common" ]; then
    roots="${XDG_CACHE_HOME:-$HOME/.cache}/engineering-standards/roots"
    key=$(dirname "$common" | tr '/' '-')
    { mkdir -p "$roots" && printf '%s\n' "$root" >"$roots/$key"; } 2>/dev/null ||
      echo "engineering-standards: корень плагина не записан в $roots" >&2
  fi

  for rule in "$root"/rules/*.md; do
    if [ -f "$rule" ]; then
      cat "$rule"
      echo
    fi
  done
  exit 0
  ```
  `hooks/hooks.json`:
  ```json
  {
    "hooks": {
      "SessionStart": [
        {
          "matcher": "startup|resume|clear|compact",
          "hooks": [
            {
              "type": "command",
              "command": "\"${CLAUDE_PLUGIN_ROOT}/hooks/session-start\""
            }
          ]
        }
      ]
    }
  }
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*SessionStartHookSpec*'` — `BUILD SUCCESSFUL`; `claude plugin validate .` — ошибок нет.
- [x] 5. Коммит — тест и код вместе

### Приёмка
- [x] Что даёт этот PR — проверено: `claude --plugin-dir . -p "Перечисли дословно правила из контекста сессии про язык и PR"` — в ответе тексты `rules/language.md` и `rules/no-pr-without-issue.md`; `claude --plugin-dir . --allowedTools 'Bash(task:*)' -p "Выполни в Bash: task --help"` — справка `task` (флаг — до PR 3: правило в `settings.json` появится в задаче 8).
- [x] Крайние случаи обработаны: повторный старт не дублирует PATH, нет `CLAUDE_ENV_FILE`, кеш недоступен → код 0, нет `rules/`.
- [x] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`.

## PR 2: скиллы и агенты из плагина
**Объём:** `skills/{groom,plan,execute,worktree}/SKILL.md`, `agents/*.md` (5); 9 файлов

### Задача 5: перенос скиллов и агентов
- Создать: `skills/{groom,plan,execute,worktree}/SKILL.md` — копии `.claude/skills/*/SKILL.md`
- Создать: `agents/{implementation-reviewer,plan-executor,plan-reviewer,standards-reviewer,test-reviewer}.md` — копии `.claude/agents/*.md`

Тест: не применим — перенос без правки текста; проверка — `diff -r` с источником и `claude plugin validate .`.
- [x] 1. Скопировать: `mkdir -p skills agents && cp -R .claude/skills/groom .claude/skills/plan .claude/skills/execute .claude/skills/worktree skills/ && cp .claude/agents/*.md agents/`. Источник — `.claude/`, не `drafts/`: в `drafts/` поверх того же текста лежат пометки «Набросок по #N» и разделы «Открыто» (сверено 2026-11-19 `diff`).
- [x] 2. Сверить: `diff -r .claude/skills skills && diff -r .claude/agents agents` — пустой вывод, код 0.
- [x] 3. Сверить: `claude plugin validate .` — ошибок нет; `claude --plugin-dir . -p "Перечисли доступные скиллы и типы агентов плагина engineering-standards"` — в ответе 4 скилла и 5 агентов.
- [x] 4. Коммит. `.claude/skills/` и `.claude/agents/` не удалять: по ним идёт выполнение этого плана (PR 3–5), удаление — «После завершения».

### Приёмка
- [x] Что даёт этот PR — проверено: шаги 2–3 задачи 5.
- [x] Крайние случаи обработаны: тексты совпадают с `.claude/` побайтно.
- [x] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`.

## PR 3: `setup` пишет `settings.json`
**Объём:** `JsonValue.kt`, `JsonParser.kt`, `JsonSpec.kt`, `JsonSpecData.kt`, `PluginSettings.kt`, `PluginSettingsSpec.kt`, `PluginSettingsSpecData.kt`, `SetupMain.kt`, `.claude/settings.json`; 9 файлов

### Задача 6: разбор и печать JSON
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/JsonValue.kt` — значение и его текст
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/JsonParser.kt` — текст → значение
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/JsonSpec.kt`, `JsonSpecData.kt` — туда и обратно, порядок ключей, экранирование, кириллица, вложенность, ошибки со строкой

- [x] 1. Падающий тест.
  `JsonSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.standards.task.cli.TaskError
  import io.kotest.core.spec.style.BehaviorSpec

  class JsonSpec :
      BehaviorSpec({
          shouldReturnTest(makeFormattedJson()) {
              JsonParser(text = makeFormattedJson(), file = "a.json").parse().toText()
          }

          shouldReturnTest(makeFormattedJson()) {
              JsonParser(text = makeCompactJson(), file = "a.json").parse().toText()
          }

          shouldReturnTest(makeParsedValue()) {
              JsonParser(text = "{\"b\": 1, \"a\": [true, null, -2.5e3]}", file = "a.json").parse()
          }

          shouldReturnTest(JsonString(value = "\"\\\n\tЖ/")) {
              JsonParser(text = "\"\\\"\\\\\\n\\t\\u0416\\/\"", file = "a.json").parse()
          }

          shouldReturnTest("\"\\u0001 \\\" \\\\ \\n\"") {
              JsonString(value = "\u0001 \" \\ \n").toText()
          }

          shouldReturnTest("{\n  \"a\": {},\n  \"b\": []\n}") {
              JsonParser(text = "{\"a\":{},\"b\":[]}", file = "a.json").parse().toText()
          }

          shouldThrowWithMessageTest<TaskError.Failure>("a.json:3: ожидается ключ в кавычках") {
              JsonParser(text = "{\n  \"a\": 1,\n  b\n}", file = "a.json").parse()
          }

          shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: комментарии JSON не поддерживает — уберите их") {
              JsonParser(text = "// настройки\n{}", file = "a.json").parse()
          }

          shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: строка без закрывающей кавычки") {
              JsonParser(text = "{\"a", file = "a.json").parse()
          }

          shouldThrowWithMessageTest<TaskError.Failure>("a.json:2: ожидается конец файла") {
              JsonParser(text = "{}\nx", file = "a.json").parse()
          }

          shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: ожидается «:» после ключа a") {
              JsonParser(text = "{\"a\" 1}", file = "a.json").parse()
          }

          shouldThrowWithMessageTest<TaskError.Failure>("a.json:1: ожидается «,» или «]»") {
              JsonParser(text = "[1 2]", file = "a.json").parse()
          }
      })
  ```
  `JsonSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  /** Текст в формате печати: ключи не по алфавиту, вложенность, все типы, кириллица, экранирование. */
  fun makeFormattedJson(): String {
      return """
          {
            "model": "opus",
            "permissions": {
              "allow": [
                "Bash(./gradlew:*)",
                "Bash(task:*)"
              ],
              "deny": []
            },
            "enabledPlugins": {
              "b@m": true,
              "a@m": false
            },
            "заметка": "строка с \"кавычками\" и \\",
            "limits": [
              1,
              -2.5e3,
              null
            ]
          }
      """.trimIndent()
  }

  /** Тот же JSON в одну строку, без отступов. */
  fun makeCompactJson(): String {
      return "{\"model\":\"opus\",\"permissions\":{\"allow\":[\"Bash(./gradlew:*)\",\"Bash(task:*)\"],\"deny\":[]}," +
          "\"enabledPlugins\":{\"b@m\":true,\"a@m\":false},\"заметка\":\"строка с \\\"кавычками\\\" и \\\\\"," +
          "\"limits\":[1,-2.5e3,null]}"
  }

  fun makeParsedValue(): JsonValue {
      val items = listOf(JsonBoolean(value = true), JsonNull, JsonNumber(raw = "-2.5e3"))
      return JsonObject(fields = linkedMapOf("b" to JsonNumber(raw = "1"), "a" to JsonArray(items = items)))
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*JsonSpec*'` — падает: `Unresolved reference 'JsonParser'`.
- [x] 3. Минимальный код.
  `JsonValue.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  /**
   * Значение JSON для `.claude/settings.json` (#43): свой формат без библиотек — запускатель компилирует голым `kotlinc`
   * (#13). Объект хранит порядок ключей, число — исходный текст: после записи файл отличается только правкой стандарта.
   */
  sealed interface JsonValue {
      /** Текст с отступом 2, без перевода строки в конце. */
      fun toText(): String {
          return render(value = this, indent = "")
      }
  }

  data class JsonObject(
      val fields: Map<String, JsonValue>,
  ) : JsonValue

  data class JsonArray(
      val items: List<JsonValue>,
  ) : JsonValue

  data class JsonString(
      val value: String,
  ) : JsonValue

  data class JsonNumber(
      val raw: String,
  ) : JsonValue

  data class JsonBoolean(
      val value: Boolean,
  ) : JsonValue

  data object JsonNull : JsonValue

  private const val STEP = "  "

  private fun render(
      value: JsonValue,
      indent: String,
  ): String {
      val inner = indent + STEP
      return when (value) {
          is JsonObject -> if (value.fields.isEmpty()) {
              "{}"
          } else {
              value.fields.entries.joinToString(separator = ",\n", prefix = "{\n", postfix = "\n$indent}") { entry ->
                  "$inner${quote(text = entry.key)}: ${render(value = entry.value, indent = inner)}"
              }
          }
          is JsonArray -> if (value.items.isEmpty()) {
              "[]"
          } else {
              value.items.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n$indent]") { item ->
                  inner + render(value = item, indent = inner)
              }
          }
          is JsonString -> quote(text = value.value)
          is JsonNumber -> value.raw
          is JsonBoolean -> value.value.toString()
          JsonNull -> "null"
      }
  }

  private fun quote(text: String): String {
      val builder = StringBuilder("\"")
      text.forEach { char ->
          when {
              char == '"' -> builder.append("\\\"")
              char == '\\' -> builder.append("\\\\")
              char == '\n' -> builder.append("\\n")
              char == '\r' -> builder.append("\\r")
              char == '\t' -> builder.append("\\t")
              char < ' ' -> builder.append("\\u%04x".format(char.code))
              else -> builder.append(char)
          }
      }
      return builder.append('"').toString()
  }
  ```
  `JsonParser.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.standards.task.cli.TaskError

  /**
   * Разбор JSON в [JsonValue] (#43): объекты, массивы, строки, числа, true/false/null. Ошибка — [TaskError.Failure] с
   * файлом и строкой: неразобранный файл `setup` не перезаписывает. Комментариев JSON не допускает — тоже ошибка.
   */
  class JsonParser(
      private val text: String,
      private val file: String,
  ) {
      private var position = 0

      fun parse(): JsonValue {
          val value = value()
          skipSpace()
          if (position < text.length) {
              fail(message = "ожидается конец файла")
          }
          return value
      }

      private fun value(): JsonValue {
          skipSpace()
          val char = text.getOrNull(index = position) ?: fail(message = "ожидается значение")
          return when {
              char == '{' -> objectValue()
              char == '[' -> arrayValue()
              char == '"' -> JsonString(value = string())
              char == '/' -> fail(message = "комментарии JSON не поддерживает — уберите их")
              char == '-' || char.isDigit() -> number()
              else -> literal()
          }
      }

      private fun objectValue(): JsonObject {
          position++
          val fields = linkedMapOf<String, JsonValue>()
          skipSpace()
          var closed = text.getOrNull(index = position) == '}'
          if (closed) {
              position++
          }
          while (!closed) {
              skipSpace()
              if (text.getOrNull(index = position) != '"') {
                  fail(message = "ожидается ключ в кавычках")
              }
              val key = string()
              skipSpace()
              if (text.getOrNull(index = position) != ':') {
                  fail(message = "ожидается «:» после ключа $key")
              }
              position++
              fields[key] = value()
              closed = separator(close = '}')
          }
          return JsonObject(fields = fields)
      }

      private fun arrayValue(): JsonArray {
          position++
          val items = mutableListOf<JsonValue>()
          skipSpace()
          var closed = text.getOrNull(index = position) == ']'
          if (closed) {
              position++
          }
          while (!closed) {
              items += value()
              closed = separator(close = ']')
          }
          return JsonArray(items = items)
      }

      /** После элемента: `,` — дальше, [close] — конец; `true`, если контейнер закрыт. */
      private fun separator(close: Char): Boolean {
          skipSpace()
          val char = text.getOrNull(index = position)
          if (char != ',' && char != close) {
              fail(message = "ожидается «,» или «$close»")
          }
          position++
          return char == close
      }

      private fun string(): String {
          position++
          val builder = StringBuilder()
          var char = next()
          while (char != '"') {
              when {
                  char == '\\' -> builder.append(escape())
                  char < ' ' -> fail(message = "перевод строки внутри строки — экранируйте \\n")
                  else -> builder.append(char)
              }
              char = next()
          }
          return builder.toString()
      }

      private fun next(): Char {
          val char = text.getOrNull(index = position) ?: fail(message = "строка без закрывающей кавычки")
          position++
          return char
      }

      private fun escape(): Char {
          val char = next()
          return when (char) {
              '"', '\\', '/' -> char
              'n' -> '\n'
              't' -> '\t'
              'r' -> '\r'
              'b' -> '\b'
              'f' -> '\u000C'
              'u' -> unicode()
              else -> fail(message = "неизвестное экранирование \\$char")
          }
      }

      private fun unicode(): Char {
          val hex = text.substring(startIndex = position, endIndex = minOf(position + HEX_LENGTH, text.length))
          if (hex.length < HEX_LENGTH || !hex.all { char -> char in HEX_DIGITS }) {
              fail(message = "ожидается \\uXXXX")
          }
          position += HEX_LENGTH
          return hex.toInt(radix = 16).toChar()
      }

      private fun number(): JsonNumber {
          val match = NUMBER.matchAt(input = text, index = position) ?: fail(message = "ожидается число")
          position = match.range.last + 1
          return JsonNumber(raw = match.value)
      }

      private fun literal(): JsonValue {
          val word = LITERALS.keys.firstOrNull { word -> text.startsWith(prefix = word, startIndex = position) }
              ?: fail(message = "ожидается значение")
          position += word.length
          return LITERALS.getValue(key = word)
      }

      private fun skipSpace() {
          while (position < text.length && text[position] in SPACE) {
              position++
          }
      }

      private fun fail(message: String): Nothing {
          val line = text.take(n = position).count { char -> char == '\n' } + 1
          throw TaskError.Failure(message = "$file:$line: $message")
      }

      private companion object {
          const val HEX_LENGTH = 4
          const val HEX_DIGITS = "0123456789abcdefABCDEF"
          const val SPACE = " \t\r\n"
          val NUMBER = Regex(pattern = "-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")
          val LITERALS = mapOf("true" to JsonBoolean(value = true), "false" to JsonBoolean(value = false), "null" to JsonNull)
      }
  }
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*JsonSpec*'` — `BUILD SUCCESSFUL`.
- [x] 5. Коммит — тест и код вместе

### Задача 7: шаг «подключение»
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/PluginSettings.kt` — три ключа стандарта в `.claude/settings.json`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/PluginSettingsSpec.kt`, `PluginSettingsSpecData.kt` — нет файла, ключи проекта целы, `false`, другой источник ± `--confirm`, репозиторий стандарта, битый JSON без записи, повтор без изменений, `--check`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:26-27` — шаг после `GitignoreBlock`, `.claude/settings.json` в файлах первого коммита

- [x] 1. Падающий тест.
  `PluginSettingsSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.Git
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import io.mockk.every
  import io.mockk.mockk
  import java.io.File

  class PluginSettingsSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val git = mockk<Git>()
          val root = makeProjectRoot()
          every { git.root() } returns root.path
          val step = PluginSettings(git = git, pluginRoot = makeProjectRoot())

          shouldReturnTest(makeSettingsAddedFindings()) {
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeFullSettings()) {
              step.run(mode = makeApplyMode())
              readSettings(root = root)
          }

          shouldReturnTest(makeProjectSettingsWithStandard()) {
              writeSettings(root = root, text = makeProjectSettings())
              step.run(mode = makeApplyMode())
              readSettings(root = root)
          }

          shouldReturnTest(makeSettingsMissingFindings()) {
              step.run(mode = makeCheckMode())
          }

          shouldReturnTest(false) {
              step.run(mode = makeCheckMode())
              File(root, ".claude/settings.json").exists()
          }

          shouldReturnTest(makeDisabledFindings()) {
              writeSettings(root = root, text = makeDisabledSettings())
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeDisabledSettings()) {
              writeSettings(root = root, text = makeDisabledSettings())
              step.run(mode = makeApplyMode())
              readSettings(root = root)
          }

          shouldReturnTest(makeOtherSourceFindings()) {
              writeSettings(root = root, text = makeOtherSourceSettings())
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeOtherSourceSettings()) {
              writeSettings(root = root, text = makeOtherSourceSettings())
              step.run(mode = makeApplyMode())
              readSettings(root = root)
          }

          shouldReturnTest(makeFullSettings()) {
              writeSettings(root = root, text = makeOtherSourceSettings())
              step.run(mode = makeConfirmedMode())
              readSettings(root = root)
          }

          shouldReturnTest(makeStandardRepositoryFindings()) {
              PluginSettings(git = git, pluginRoot = root).run(mode = makeApplyMode())
          }

          shouldReturnTest(makePermissionOnlySettings()) {
              PluginSettings(git = git, pluginRoot = root).run(mode = makeApplyMode())
              readSettings(root = root)
          }

          shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json:3: ожидается ключ в кавычках") {
              writeSettings(root = root, text = makeBrokenSettings())
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeBrokenSettings()) {
              writeSettings(root = root, text = makeBrokenSettings())
              runCatching { step.run(mode = makeApplyMode()) }
              readSettings(root = root)
          }

          shouldReturnTest(makeSettingsUnchangedFindings()) {
              step.run(mode = makeApplyMode())
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeFullSettings()) {
              writeSettings(root = root, text = "")
              step.run(mode = makeApplyMode())
              readSettings(root = root)
          }

          shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json: ожидается объект {…} на верхнем уровне") {
              writeSettings(root = root, text = "[]\n")
              step.run(mode = makeApplyMode())
          }

          shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json: permissions.allow — ожидается массив […]") {
              writeSettings(root = root, text = "{\"permissions\": {\"allow\": \"Bash(task:*)\"}}\n")
              step.run(mode = makeApplyMode())
          }

          shouldThrowWithMessageTest<TaskError.Failure>(".claude/settings.json: enabledPlugins — ожидается объект {…}") {
              writeSettings(root = root, text = "{\"enabledPlugins\": []}\n")
              step.run(mode = makeApplyMode())
          }
      })
  ```
  `PluginSettingsSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import java.io.File

  private const val SETTINGS = ".claude/settings.json"

  fun writeSettings(
      root: File,
      text: String,
  ) {
      val file = File(root, SETTINGS)
      file.parentFile.mkdirs()
      file.writeText(text)
  }

  fun readSettings(root: File): String {
      return File(root, SETTINGS).readText()
  }

  fun makeSettingsFinding(
      status: Finding.Status,
      text: String,
  ): Finding {
      return Finding(status = status, area = "подключение", text = "$SETTINGS: $text")
  }

  fun makeFullSettings(): String {
      return makeStandardKeys(enabled = "true", source = makeGitHubSource())
  }

  fun makeDisabledSettings(): String {
      return makeStandardKeys(enabled = "false", source = makeGitHubSource())
  }

  fun makeOtherSourceSettings(): String {
      return makeStandardKeys(enabled = "true", source = listOf("\"source\": \"directory\"", "\"path\": \"/tmp/standards\""))
  }

  private fun makeGitHubSource(): List<String> {
      return listOf("\"source\": \"github\"", "\"repo\": \"Nakulov/engineering-standards\"")
  }

  /** Три ключа стандарта в формате печати; [source] — две строки источника маркетплейса. */
  private fun makeStandardKeys(
      enabled: String,
      source: List<String>,
  ): String {
      val lines = listOf(
          "{",
          "  \"extraKnownMarketplaces\": {",
          "    \"engineering-standards\": {",
          "      \"source\": {",
          "        ${source[0]},",
          "        ${source[1]}",
          "      }",
          "    }",
          "  },",
          "  \"enabledPlugins\": {",
          "    \"engineering-standards@engineering-standards\": $enabled",
          "  },",
          "  \"permissions\": {",
          "    \"allow\": [",
          "      \"Bash(task:*)\"",
          "    ]",
          "  }",
          "}",
      )
      return lines.joinToString(separator = "\n", postfix = "\n")
  }

  /** Ключи проекта: модель и своё правило разрешения. */
  fun makeProjectSettings(): String {
      return """
          {
            "model": "opus",
            "permissions": {
              "allow": [
                "Bash(./gradlew:*)"
              ]
            }
          }
      """.trimIndent() + "\n"
  }

  /** Ключи проекта на месте, ключи стандарта — в конце объекта, разрешение — в массиве проекта. */
  fun makeProjectSettingsWithStandard(): String {
      return """
          {
            "model": "opus",
            "permissions": {
              "allow": [
                "Bash(./gradlew:*)",
                "Bash(task:*)"
              ]
            },
            "extraKnownMarketplaces": {
              "engineering-standards": {
                "source": {
                  "source": "github",
                  "repo": "Nakulov/engineering-standards"
                }
              }
            },
            "enabledPlugins": {
              "engineering-standards@engineering-standards": true
            }
          }
      """.trimIndent() + "\n"
  }

  fun makePermissionOnlySettings(): String {
      return """
          {
            "permissions": {
              "allow": [
                "Bash(task:*)"
              ]
            }
          }
      """.trimIndent() + "\n"
  }

  fun makeBrokenSettings(): String {
      return "{\n  \"model\": \"opus\",\n  oops\n}\n"
  }

  fun makeSettingsAddedFindings(): List<Finding> {
      return listOf(
          makeSettingsFinding(
              status = Finding.Status.FIXED,
              text = "маркетплейс engineering-standards добавлен — закоммитьте через задачу",
          ),
          makeSettingsFinding(
              status = Finding.Status.FIXED,
              text = "плагин engineering-standards@engineering-standards включён — закоммитьте через задачу",
          ),
          makeSettingsFinding(
              status = Finding.Status.FIXED,
              text = "разрешение Bash(task:*) добавлено — закоммитьте через задачу",
          ),
      )
  }

  fun makeSettingsMissingFindings(): List<Finding> {
      return listOf(
          makeSettingsFinding(status = Finding.Status.DEVIATION, text = "нет маркетплейса engineering-standards"),
          makeSettingsFinding(
              status = Finding.Status.DEVIATION,
              text = "плагин engineering-standards@engineering-standards не включён",
          ),
          makeSettingsFinding(status = Finding.Status.DEVIATION, text = "нет разрешения Bash(task:*)"),
      )
  }

  fun makeSettingsUnchangedFindings(): List<Finding> {
      return listOf(
          makeSettingsFinding(status = Finding.Status.OK, text = "маркетплейс engineering-standards"),
          makeSettingsFinding(
              status = Finding.Status.OK,
              text = "плагин engineering-standards@engineering-standards включён",
          ),
          makeSettingsFinding(status = Finding.Status.OK, text = "разрешение Bash(task:*)"),
      )
  }

  fun makeDisabledFindings(): List<Finding> {
      val unchanged = makeSettingsUnchangedFindings()
      val disabled = makeSettingsFinding(
          status = Finding.Status.DEVIATION,
          text = "плагин engineering-standards@engineering-standards выключен проектом (false) — " +
              "стандарт не переписывает решение проекта",
      )
      return listOf(unchanged[0], disabled, unchanged[2])
  }

  fun makeOtherSourceFindings(): List<Finding> {
      val unchanged = makeSettingsUnchangedFindings()
      val other = makeSettingsFinding(
          status = Finding.Status.NEEDS_CONFIRM,
          text = "источник маркетплейса engineering-standards отличается: " +
              "{ \"source\": { \"source\": \"directory\", \"path\": \"/tmp/standards\" } } — " +
              "заменит на github Nakulov/engineering-standards",
      )
      return listOf(other, unchanged[1], unchanged[2])
  }

  fun makeStandardRepositoryFindings(): List<Finding> {
      return listOf(
          makeSettingsFinding(
              status = Finding.Status.NOTE,
              text = "репозиторий стандарта — маркетплейс и enabledPlugins не пишутся: " +
                  "подключение через claude --plugin-dir .",
          ),
          makeSettingsFinding(
              status = Finding.Status.FIXED,
              text = "разрешение Bash(task:*) добавлено — закоммитьте через задачу",
          ),
      )
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*PluginSettingsSpec*'` — падает: `Unresolved reference 'PluginSettings'`.
- [x] 3. Минимальный код.
  `PluginSettings.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.standards.task.cli.TaskError
  import io.github.nakulov.standards.task.git.Git
  import java.io.File

  /**
   * Подключение плагина в `.claude/settings.json` проекта (#43, решения 3 и 5): стандарт владеет тремя ключами —
   * маркетплейс, `enabledPlugins` и разрешение `Bash(task:*)`; остальные ключи и правила проекта не трогаются. Явный
   * `false` проекта не переписывается — стандарт не отменяет решение проекта (#1). Другой источник маркетплейса
   * заменяется только с `--confirm`. В репозитории самого стандарта плагин подключается через `--plugin-dir` —
   * пишется только разрешение (решение 2).
   */
  class PluginSettings(
      private val git: Git,
      private val pluginRoot: File,
  ) : Setup.Step {
      override fun run(mode: Setup.Mode): List<Finding> {
          val root = File(git.root())
          val file = File(root, SETTINGS)
          // Пустой файл (`touch`) — как пустой объект: ключи стандарта дописываются, а не сбой.
          val text = if (file.isFile) file.readText() else ""
          val parsed = if (text.isBlank()) JsonObject(fields = emptyMap()) else JsonParser(text = text, file = SETTINGS).parse()
          val settings = parsed as? JsonObject
              ?: throw TaskError.Failure(message = "$SETTINGS: ожидается объект {…} на верхнем уровне")
          val fields = LinkedHashMap(settings.fields)
          val plugin = if (pluginRoot.canonicalFile == root.canonicalFile) {
              listOf(finding(status = Finding.Status.NOTE, text = STANDARD_REPOSITORY))
          } else {
              listOf(marketplace(fields = fields, mode = mode), enabled(fields = fields, mode = mode))
          }
          val findings = plugin + permission(fields = fields, mode = mode)
          if (fields != settings.fields) {
              file.parentFile.mkdirs()
              file.writeText(JsonObject(fields = fields).toText() + "\n")
          }
          return findings
      }

      private fun marketplace(
          fields: MutableMap<String, JsonValue>,
          mode: Setup.Mode,
      ): Finding {
          val marketplaces = objectAt(fields = fields, key = MARKETPLACES)
          val current = marketplaces.fields[NAME]
          if (current == MARKETPLACE) {
              return finding(status = Finding.Status.OK, text = "маркетплейс $NAME")
          }
          val was = current?.let { value -> compact(value = value) }
          val (status, text) = when {
              was == null && !mode.apply -> Finding.Status.DEVIATION to "нет маркетплейса $NAME"
              was == null -> Finding.Status.FIXED to "маркетплейс $NAME добавлен$COMMIT"
              !mode.apply -> Finding.Status.DEVIATION to "источник маркетплейса $NAME отличается: $was"
              !mode.confirmed -> Finding.Status.NEEDS_CONFIRM to
                  "источник маркетплейса $NAME отличается: $was — заменит на github $REPOSITORY"
              else -> Finding.Status.FIXED to "источник маркетплейса $NAME заменён, было: $was$COMMIT"
          }
          if (status == Finding.Status.FIXED) {
              fields[MARKETPLACES] = JsonObject(fields = marketplaces.fields + (NAME to MARKETPLACE))
          }
          return finding(status = status, text = text)
      }

      private fun enabled(
          fields: MutableMap<String, JsonValue>,
          mode: Setup.Mode,
      ): Finding {
          val plugins = objectAt(fields = fields, key = ENABLED)
          return when {
              plugins.fields[PLUGIN] == JsonBoolean(value = true) ->
                  finding(status = Finding.Status.OK, text = "плагин $PLUGIN включён")
              plugins.fields[PLUGIN] == JsonBoolean(value = false) -> finding(
                  status = Finding.Status.DEVIATION,
                  text = "плагин $PLUGIN выключен проектом (false) — стандарт не переписывает решение проекта",
              )
              !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "плагин $PLUGIN не включён")
              else -> {
                  fields[ENABLED] = JsonObject(fields = plugins.fields + (PLUGIN to JsonBoolean(value = true)))
                  finding(status = Finding.Status.FIXED, text = "плагин $PLUGIN включён$COMMIT")
              }
          }
      }

      private fun permission(
          fields: MutableMap<String, JsonValue>,
          mode: Setup.Mode,
      ): Finding {
          val permissions = objectAt(fields = fields, key = PERMISSIONS)
          val allow = permissions.fields[ALLOW] ?: JsonArray(items = emptyList())
          val items = (allow as? JsonArray)?.items
              ?: throw TaskError.Failure(message = "$SETTINGS: $PERMISSIONS.$ALLOW — ожидается массив […]")
          return when {
              JsonString(value = RULE) in items -> finding(status = Finding.Status.OK, text = "разрешение $RULE")
              !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "нет разрешения $RULE")
              else -> {
                  val updated = JsonArray(items = items + JsonString(value = RULE))
                  fields[PERMISSIONS] = JsonObject(fields = permissions.fields + (ALLOW to updated))
                  finding(status = Finding.Status.FIXED, text = "разрешение $RULE добавлено$COMMIT")
              }
          }
      }

      private fun objectAt(
          fields: Map<String, JsonValue>,
          key: String,
      ): JsonObject {
          val value = fields[key] ?: return JsonObject(fields = emptyMap())
          return value as? JsonObject ?: throw TaskError.Failure(message = "$SETTINGS: $key — ожидается объект {…}")
      }

      private fun compact(value: JsonValue): String {
          return value.toText().lines().joinToString(separator = " ") { line -> line.trim() }
      }

      private fun finding(
          status: Finding.Status,
          text: String,
      ): Finding {
          return Finding(status = status, area = AREA, text = "$SETTINGS: $text")
      }

      private companion object {
          const val AREA = "подключение"
          const val SETTINGS = ".claude/settings.json"
          const val NAME = "engineering-standards"
          const val PLUGIN = "engineering-standards@engineering-standards"
          const val REPOSITORY = "Nakulov/engineering-standards"
          const val MARKETPLACES = "extraKnownMarketplaces"
          const val ENABLED = "enabledPlugins"
          const val PERMISSIONS = "permissions"
          const val ALLOW = "allow"
          const val RULE = "Bash(task:*)"
          const val COMMIT = " — закоммитьте через задачу"
          const val STANDARD_REPOSITORY =
              "репозиторий стандарта — маркетплейс и enabledPlugins не пишутся: подключение через claude --plugin-dir ."
          val MARKETPLACE = JsonObject(
              fields = mapOf(
                  "source" to JsonObject(
                      fields = mapOf("source" to JsonString(value = "github"), "repo" to JsonString(value = REPOSITORY)),
                  ),
              ),
          )
      }
  }
  ```
  `SetupMain.kt` — в списке шагов после `GitignoreBlock(...)`:
  ```kotlin
              GitignoreBlock(git = git, block = File(pluginRoot, GITIGNORE).readText()),
              PluginSettings(git = git, pluginRoot = pluginRoot),
              InitialBranch(
                  gitHub = gitHub,
                  git = git,
                  files = listOf(".github", ".gitignore", ".editorconfig", ".claude/settings.json"),
              ),
  ```
  и в KDoc `main` после «Порядок шагов: сначала файлы» дописать «(в том числе `.claude/settings.json` — подключение плагина, #43)».
- [x] 4. Запустить `./gradlew :scripts:test --tests '*PluginSettingsSpec*' --tests '*JsonSpec*'` — `BUILD SUCCESSFUL`.
- [x] 5. Коммит — тест и код вместе

### Задача 8: `settings.json` этого репозитория
- Создать: `.claude/settings.json` — разрешение `Bash(task:*)`, пишет `setup` (репозиторий стандарта — без маркетплейса)

Тест: не применим — файл пишет шаг из задачи 7; проверка — `scripts/setup --check`.
- [x] 1. Запустить `scripts/setup --check`. Отклонения должны быть только по `.claude/settings.json`. Если есть другие — ⚠️ в «Прогресс», остановиться и спросить пользователя: полный `setup` записал бы их в GitHub.
- [x] 2. Запустить `scripts/setup` в корне репозитория. Ожидается в выводе:
  - `[заметка] подключение: .claude/settings.json: репозиторий стандарта — …`
  - `[исправлено] подключение: .claude/settings.json: разрешение Bash(task:*) добавлено — закоммитьте через задачу`
- [x] 3. Сверить файл: `cat .claude/settings.json` — ровно объект `permissions.allow` с `"Bash(task:*)"` (как `makePermissionOnlySettings()`).
- [x] 4. Запустить `scripts/setup --check` — код 0, по «подключение» — `[заметка]` и `[ok]`.
- [x] 5. Коммит — только `.claude/settings.json`. `.claude/skills/` и `.claude/agents/` всё ещё в `.git/info/exclude` и в коммит не попадают.

### Приёмка
- [x] Что даёт этот PR — проверено: `scripts/setup --check` здесь — код 0; спеки `PluginSettingsSpec`, `JsonSpec` зелёные.
- [x] Крайние случаи обработаны: нет файла, чужие ключи, `false`, другой источник ± `--confirm`, репозиторий стандарта, битый JSON без записи, комментарии, повторный запуск.
- [x] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`.

## PR 4: обёртка `task` для терминала
**Объём:** `scripts/task-wrapper`, `TaskWrapperScriptSpec.kt`, `TaskWrapperScriptSpecData.kt`, `TaskWrapper.kt`, `TaskWrapperSpec.kt`, `TaskWrapperSpecData.kt`, `SetupMain.kt`; 7 файлов

### Задача 9: скрипт обёртки
- Создать: `scripts/task-wrapper` — источник `~/.local/bin/task`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/plugin/TaskWrapperScriptSpec.kt`, `TaskWrapperScriptSpecData.kt` — репозиторий стандарта, запись, нет записи → 2, папка версии удалена → 2, ворктри → тот же ключ, не git → 2

- [x] 1. Падающий тест. Используются `makeTempDir`, `runGit`, `makeGitProject`, `makeRootKey` из `SessionStartHookSpecData.kt` (тот же пакет).
  `TaskWrapperScriptSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.standards.task.cli.LauncherRun
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import java.io.File

  // Вне стандарта: спека проверяет не класс, а bash-скрипт `scripts/task-wrapper` — запускает его копию процессом в
  // разных папках (#43, решение 6). Подготовка репозиториев и запуск — в TaskWrapperScriptSpecData.
  class TaskWrapperScriptSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val wrapper = makeWrapperCopy()
          val cache = makeTempDir(prefix = "cache")
          val plugin = makeStubPlugin()
          val project = makeGitProject()

          shouldReturnTest(LauncherRun(code = 0, out = "стандарт --help\n", err = "")) {
              runWrapper(wrapper = wrapper, directory = makeStandardRepo(), cache = cache)
          }

          shouldReturnTest(makePluginRun()) {
              writeRootRecord(cache = cache, project = project, root = plugin)
              runWrapper(wrapper = wrapper, directory = project, cache = cache)
          }

          shouldReturnTest(makeNoRecordRun()) {
              runWrapper(wrapper = wrapper, directory = project, cache = cache)
          }

          shouldReturnTest(makeNoRecordRun()) {
              writeRootRecord(cache = cache, project = project, root = File(plugin, "removed"))
              runWrapper(wrapper = wrapper, directory = project, cache = cache)
          }

          shouldReturnTest(makePluginRun()) {
              writeRootRecord(cache = cache, project = project, root = plugin)
              runWrapper(wrapper = wrapper, directory = makeWorktree(project = project), cache = cache)
          }

          shouldReturnTest(makeNotGitRun()) {
              runWrapper(wrapper = wrapper, directory = makeTempDir(prefix = "plain"), cache = cache)
          }
      })
  ```
  `TaskWrapperScriptSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.plugin

  import io.github.nakulov.standards.task.cli.LauncherRun
  import java.io.File

  /** Копия `scripts/task-wrapper` под именем `task`. */
  fun makeWrapperCopy(): File {
      val wrapper = File(makeTempDir(prefix = "bin"), "task")
      File(System.getProperty("task.repoRoot"), "scripts/task-wrapper").copyTo(target = wrapper)
      wrapper.setExecutable(true)
      return wrapper
  }

  private fun writeScript(
      file: File,
      text: String,
  ) {
      file.parentFile.mkdirs()
      file.writeText(text)
      file.setExecutable(true)
  }

  /** Репозиторий стандарта: `plugin.json` нашего плагина и свой `scripts/task`. */
  fun makeStandardRepo(): File {
      val repo = makeGitProject()
      val manifest = File(repo, ".claude-plugin/plugin.json")
      manifest.parentFile.mkdirs()
      manifest.writeText("{\n  \"name\": \"engineering-standards\"\n}\n")
      writeScript(file = File(repo, "scripts/task"), text = "#!/bin/sh\necho \"стандарт \$*\"\n")
      return repo
  }

  /** Корень установленного плагина: `scripts/task` печатает аргументы и выходит с кодом 3. */
  fun makeStubPlugin(): File {
      val plugin = makeTempDir(prefix = "installed")
      writeScript(file = File(plugin, "scripts/task"), text = "#!/bin/sh\necho \"плагин \$*\"\nexit 3\n")
      return plugin
  }

  fun writeRootRecord(
      cache: File,
      project: File,
      root: File,
  ) {
      val record = File(cache, "engineering-standards/roots/${makeRootKey(project = project)}")
      record.parentFile.mkdirs()
      record.writeText("${root.path}\n")
  }

  /** Копия задачи: пустой коммит и `git worktree add` во временную папку. */
  fun makeWorktree(project: File): File {
      runGit(directory = project, "-c", "user.name=t", "-c", "user.email=t@t", "commit", "--allow-empty", "-qm", "init")
      val worktree = File(makeTempDir(prefix = "worktree"), "copy")
      runGit(directory = project, "worktree", "add", "-q", worktree.path)
      return worktree
  }

  fun makePluginRun(): LauncherRun {
      return LauncherRun(code = 3, out = "плагин --help\n", err = "")
  }

  fun makeNoRecordRun(): LauncherRun {
      return LauncherRun(
          code = 2,
          out = "",
          err = "task: плагин для этого проекта не найден — запустите claude в папке проекта\n",
      )
  }

  fun makeNotGitRun(): LauncherRun {
      return LauncherRun(code = 2, out = "", err = "task: не репозиторий git — перейдите в папку проекта\n")
  }

  /** `task --help` через обёртку в [directory]; git не поднимается выше кеша тестов — там репозиторий стандарта. */
  fun runWrapper(
      wrapper: File,
      directory: File,
      cache: File,
  ): LauncherRun {
      val builder = ProcessBuilder(wrapper.path, "--help").directory(directory)
      builder.environment()["XDG_CACHE_HOME"] = cache.path
      builder.environment()["GIT_CEILING_DIRECTORIES"] = System.getProperty("task.cache")
      val process = builder.start()
      val out = process.inputStream.bufferedReader().readText()
      val err = process.errorStream.bufferedReader().readText()
      return LauncherRun(code = process.waitFor(), out = out, err = err)
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*TaskWrapperScriptSpec*'` — падает: `NoSuchFileException: …/scripts/task-wrapper` в `makeWrapperCopy`.
- [x] 3. Минимальный код. `scripts/task-wrapper` (исполняемый, `chmod +x`):
  ```bash
  #!/bin/bash
  # engineering-standards task-wrapper
  # Обёртка ~/.local/bin/task (#43, решение 6): ставит и обновляет setup, вторая строка — метка, по ней setup узнаёт
  # свою обёртку. Находит scripts/task плагина для репозитория текущей папки. Совместим с bash 3.2 (macOS).
  set -u

  top=$(git rev-parse --show-toplevel 2>/dev/null)
  if [ -z "$top" ]; then
    echo "task: не репозиторий git — перейдите в папку проекта" >&2
    exit 2
  fi
  # Репозиторий самого стандарта: task своей рабочей копии, в ворктри — копии.
  if grep -qs '"name": "engineering-standards"' "$top/.claude-plugin/plugin.json"; then
    exec "$top/scripts/task" "$@"
  fi
  # Проект: корень плагина записал хук SessionStart последней сессии Claude в этом репозитории.
  common=$(git rev-parse --path-format=absolute --git-common-dir)
  key=$(dirname "$common" | tr '/' '-')
  root=$(cat "${XDG_CACHE_HOME:-$HOME/.cache}/engineering-standards/roots/$key" 2>/dev/null)
  if [ -z "$root" ] || [ ! -x "$root/scripts/task" ]; then
    echo "task: плагин для этого проекта не найден — запустите claude в папке проекта" >&2
    exit 2
  fi
  exec "$root/scripts/task" "$@"
  ```
- [x] 4. Запустить `./gradlew :scripts:test --tests '*TaskWrapperScriptSpec*'` — `BUILD SUCCESSFUL`.
- [x] 5. Коммит — тест и код вместе

### Задача 10: шаг «обёртка»
- Создать: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/TaskWrapper.kt` — ставит `~/.local/bin/task`
- Создать: `scripts/src/test/kotlin/io/github/nakulov/standards/setup/TaskWrapperSpec.kt`, `TaskWrapperSpecData.kt` — временный `HOME`: нет, наша отличается, наша совпадает, чужая, не в PATH, `--check`
- Изменить: `scripts/src/main/kotlin/io/github/nakulov/standards/setup/SetupMain.kt:34` — шаг последним

- [x] 1. Падающий тест.
  `TaskWrapperSpec.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.kotestkit.shouldReturnTest
  import io.github.nakulov.kotestkit.shouldThrowWithMessageTest
  import io.github.nakulov.standards.task.cli.TaskError
  import io.kotest.core.spec.IsolationMode
  import io.kotest.core.spec.style.BehaviorSpec
  import java.io.File

  class TaskWrapperSpec :
      BehaviorSpec({
          isolationMode = IsolationMode.InstancePerRoot

          val home = makeProjectRoot()
          val plugin = makeWrapperPlugin()
          val step = TaskWrapper(home = home, pluginRoot = plugin, path = "${File(home, ".local/bin").path}:/usr/bin")

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.FIXED, text = makeInstalledText()))) {
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeWrapperSource() to true) {
              step.run(mode = makeApplyMode())
              readWrapper(home = home) to File(home, ".local/bin/task").canExecute()
          }

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.NOTE, text = makeMissingText()))) {
              step.run(mode = makeCheckMode())
          }

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.FIXED, text = makeUpdatedText()))) {
              writeWrapper(home = home, text = makeOldWrapper())
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.DEVIATION, text = makeDifferentText()))) {
              writeWrapper(home = home, text = makeOldWrapper())
              step.run(mode = makeCheckMode())
          }

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.OK, text = "обёртка по стандарту"))) {
              writeWrapper(home = home, text = makeWrapperSource())
              step.run(mode = makeCheckMode())
          }

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.NOTE, text = makeForeignText(plugin = plugin)))) {
              writeWrapper(home = home, text = makeForeignTask())
              step.run(mode = makeApplyMode())
          }

          shouldReturnTest(makeForeignTask()) {
              writeWrapper(home = home, text = makeForeignTask())
              step.run(mode = makeApplyMode())
              readWrapper(home = home)
          }

          shouldReturnTest(listOf(makeWrapperFinding(status = Finding.Status.OK, text = "обёртка по стандарту"), makePathNote())) {
              writeWrapper(home = home, text = makeWrapperSource())
              TaskWrapper(home = home, pluginRoot = plugin, path = "/usr/bin").run(mode = makeCheckMode())
          }

          shouldThrowWithMessageTest<TaskError.Failure>("~/.local/bin/task: не записана — проверьте права на ~/.local/bin") {
              makeReadOnlyBin(home = home)
              step.run(mode = makeApplyMode())
          }
      })
  ```
  `TaskWrapperSpecData.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import java.io.File

  fun makeWrapperSource(): String {
      return "#!/bin/bash\n# engineering-standards task-wrapper\nexec true\n"
  }

  /** Наша обёртка прошлой версии: метка есть, текст другой. */
  fun makeOldWrapper(): String {
      return "#!/bin/bash\n# engineering-standards task-wrapper\nexec false\n"
  }

  /** Чужой `task` (например go-task): метки нет. */
  fun makeForeignTask(): String {
      return "#!/bin/sh\necho go-task\n"
  }

  /** Папка плагина с источником обёртки `scripts/task-wrapper`. */
  fun makeWrapperPlugin(): File {
      val plugin = makeProjectRoot()
      val source = File(plugin, "scripts/task-wrapper")
      source.parentFile.mkdirs()
      source.writeText(makeWrapperSource())
      return plugin
  }

  fun writeWrapper(
      home: File,
      text: String,
  ) {
      val file = File(home, ".local/bin/task")
      file.parentFile.mkdirs()
      file.writeText(text)
  }

  fun readWrapper(home: File): String {
      return File(home, ".local/bin/task").readText()
  }

  fun makeWrapperFinding(
      status: Finding.Status,
      text: String,
  ): Finding {
      return Finding(status = status, area = "машина", text = "~/.local/bin/task: $text")
  }

  fun makeInstalledText(): String {
      return "обёртка установлена — запись вне репозитория, одна на машину"
  }

  fun makeUpdatedText(): String {
      return "обёртка обновлена — запись вне репозитория, одна на машину"
  }

  fun makeMissingText(): String {
      return "обёртки нет — task из терминала не найдётся, поставит setup"
  }

  fun makeDifferentText(): String {
      return "обёртка отличается от стандарта — обновит setup"
  }

  fun makeForeignText(plugin: File): String {
      return "чужая программа, не трогаю — task из терминала: ${plugin.path}/scripts/task"
  }

  fun makePathNote(): Finding {
      val text = "~/.local/bin нет в PATH — добавьте в ~/.zshrc: export PATH=\"\$HOME/.local/bin:\$PATH\""
      return Finding(status = Finding.Status.NOTE, area = "машина", text = text)
  }

  /** `~/.local/bin` только для чтения: обёртку в ней не записать. */
  fun makeReadOnlyBin(home: File) {
      val bin = File(home, ".local/bin")
      bin.mkdirs()
      bin.setWritable(false)
  }
  ```
- [x] 2. Запустить `./gradlew :scripts:test --tests '*TaskWrapperSpec*'` — падает: `Unresolved reference 'TaskWrapper'`.
- [x] 3. Минимальный код.
  `TaskWrapper.kt`:
  ```kotlin
  package io.github.nakulov.standards.setup

  import io.github.nakulov.standards.task.cli.TaskError
  import java.io.File
  import java.io.IOException

  /**
   * Обёртка `~/.local/bin/task` для терминала (#43, решение 6): одна на машину, единственная запись `setup` вне
   * репозитория. Свою обёртку узнаёт по метке и обновляет без `--confirm`; чужой `task` (например go-task) не трогает.
   * `--check` без обёртки — заметка: на CI и чужих машинах её нет, это не отклонение репозитория.
   */
  class TaskWrapper(
      private val home: File,
      private val pluginRoot: File,
      private val path: String,
  ) : Setup.Step {
      override fun run(mode: Setup.Mode): List<Finding> {
          val source = File(pluginRoot, SOURCE).readText()
          val target = File(home, TARGET)
          val current = if (target.isFile) target.readText() else null
          val wrapper = when {
              current == source -> finding(status = Finding.Status.OK, text = "обёртка по стандарту")
              current != null && MARKER !in current -> finding(
                  status = Finding.Status.NOTE,
                  text = "чужая программа, не трогаю — task из терминала: ${File(pluginRoot, "scripts/task").path}",
              )
              !mode.apply && current == null ->
                  finding(status = Finding.Status.NOTE, text = "обёртки нет — task из терминала не найдётся, поставит setup")
              !mode.apply -> finding(status = Finding.Status.DEVIATION, text = "обёртка отличается от стандарта — обновит setup")
              else -> {
                  install(target = target, source = source)
                  val action = if (current == null) "установлена" else "обновлена"
                  finding(status = Finding.Status.FIXED, text = "обёртка $action — запись вне репозитория, одна на машину")
              }
          }
          val bin = File(home, BIN).path
          if (path.split(':').any { entry -> entry.trimEnd('/') == bin }) {
              return listOf(wrapper)
          }
          val note = "~/$BIN нет в PATH — добавьте в ~/.zshrc: export PATH=\"\$HOME/$BIN:\$PATH\""
          return listOf(wrapper, Finding(status = Finding.Status.NOTE, area = AREA, text = note))
      }

      private fun install(
          target: File,
          source: String,
      ) {
          try {
              target.parentFile.mkdirs()
              target.writeText(source)
          } catch (@Suppress("SwallowedException") error: IOException) {
              throw TaskError.Failure(message = "~/$TARGET: не записана — проверьте права на ~/$BIN")
          }
          // Ветка только кодом, без теста: снять право на chmod у владельца файла в тесте нельзя.
          if (!target.setExecutable(true)) {
              throw TaskError.Failure(message = "~/$TARGET: не выставлены права на запуск — chmod +x ~/$TARGET")
          }
      }

      private fun finding(
          status: Finding.Status,
          text: String,
      ): Finding {
          return Finding(status = status, area = AREA, text = "~/$TARGET: $text")
      }

      private companion object {
          const val AREA = "машина"
          const val BIN = ".local/bin"
          const val TARGET = ".local/bin/task"
          const val SOURCE = "scripts/task-wrapper"
          const val MARKER = "# engineering-standards task-wrapper"
      }
  }
  ```
  `SetupMain.kt` — последним в списке шагов, после `BoardSetup(...)`:
  ```kotlin
              BoardSetup(gitHub = gitHub, template = board, scratch = scratch),
              TaskWrapper(
                  home = File(System.getProperty("user.home")),
                  pluginRoot = pluginRoot,
                  path = System.getenv("PATH").orEmpty(),
              ),
  ```
  и в KDoc `main` дописать: «последним — обёртка `~/.local/bin/task`: шаг машины, не репозитория (#43)».
- [x] 4. Запустить `./gradlew :scripts:test --tests '*TaskWrapperSpec*'` — `BUILD SUCCESSFUL`.
- [x] 5. Коммит — тест и код вместе

### Приёмка
- [ ] Что даёт этот PR — проверено: `scripts/setup` здесь пишет `~/.local/bin/task` (`[исправлено] машина: …`, при отсутствии `~/.local/bin` в PATH — заметка со строкой для `~/.zshrc`); из терминала в корне репозитория `~/.local/bin/task --help` — справка `task`.
- [ ] Крайние случаи обработаны: чужой `task`, своя прошлой версии, `--check` без обёртки — заметка, не в PATH, нет записи / удалённая версия → код 2, ворктри, не git.
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`.

## PR 5: `.gitignore` и README
**Объём:** `templates/project/gitignore`, `.gitignore`, `README.md`; 3 файла

### Задача 11: `plans/done/` и `plans/journal/` в общем блоке
- Изменить: `templates/project/gitignore` — 2 строки после `.claude/worktrees/`
- Изменить: `.gitignore` — общий блок по шаблону, пишет `setup`

Тест: не применим — шаблон без логики; проверка формата — `scripts/setup --check` (сверяет блок с шаблоном).
- [ ] 1. В `templates/project/gitignore` после `.claude/worktrees/` добавить строки `plans/done/` и `plans/journal/`. `plans/<slug>.md` не скрывается: его коммитит `task plan` (#53).
- [ ] 2. Запустить `scripts/setup --check`. Отклонения должны быть только по `.gitignore`. Если есть другие — ⚠️ в «Прогресс», остановиться и спросить пользователя: полный `setup` записал бы их в GitHub.
- [ ] 3. Запустить `scripts/setup` — в выводе `[исправлено] файлы: .gitignore: общий блок отличается от стандарта — блок записан, закоммитьте через задачу`.
- [ ] 4. Запустить `scripts/setup --check` — код 0; `git status --short` не показывает `plans/done/` и `plans/journal/`.
- [ ] 5. Коммит — `templates/project/gitignore` и `.gitignore`

### Задача 12: раздел README «Подключение»
- Изменить: `README.md:39-42` — как подключить проект и как работать в этом репозитории

Тест: не применим — текст без логики.
- [ ] 1. Заменить абзац раздела «Подключение» (`README.md:41-42`) на:
  ```markdown
  Проект подключает плагин Claude Code из этого репозитория. Правила не копируются в проект, поэтому изменения стандартов
  доходят до всех проектов без ручной синхронизации.

  Подключает шаг `setup` (`scripts/setup` из клона этого репозитория, запуск в папке проекта):

  - `.claude/settings.json` — маркетплейс `engineering-standards`, включённый плагин и разрешение `Bash(task:*)`.
    Остальные ключи проекта не меняются. После слияния PR с этим файлом `claude` в папке проекта предложит установить
    плагин.
  - `~/.local/bin/task` — обёртка для терминала, одна на машину: находит `task` плагина текущего проекта. Если
    `~/.local/bin` нет в PATH, `setup` подскажет строку для `~/.zshrc`.

  Обновление плагина — `/plugin update` в Claude Code; новая версия действует со следующей сессии.

  В этом репозитории плагин не устанавливается: сессия запускается с рабочей копией, правка скилла видна новой сессии
  без переустановки.

      alias claude-std='claude --plugin-dir .'

  Скиллы вызываются как <форма по итогу опыта 6: `/groom` или `/engineering-standards:groom`>. В копии задачи
  (`.claude/worktrees/…`) правка видна только новой сессии, запущенной из папки копии.
  ```
  Подставить форму команд по строке «опыт 6» в «Прогресс». Угловые скобки в README не оставлять.
- [ ] 2. Сверить: `grep -n "plugin-dir" README.md` — строка с алиасом есть; разделы «Подключение» и «Изменение стандартов» идут подряд.
- [ ] 3. Коммит

### Приёмка
- [ ] Что даёт этот PR — проверено: `scripts/setup --check` — код 0; README описывает `setup`, обёртку и алиас.
- [ ] Крайние случаи обработаны: собственная `plans/` проекта — скрываются только `done/` и `journal/`.
- [ ] Полный набор тестов зелёный: `./gradlew :scripts:test` → `BUILD SUCCESSFUL`.
- [ ] Последний блок, критерии цели из «Обзора»:
  - `claude --plugin-dir .` — `rules/`, скиллы и агенты в сессии (проверка из приёмки PR 1 и PR 2);
  - `task --help` из сессии и из терминала;
  - `scripts/setup --check` — код 0;
  - `./gradlew :scripts:test` — зелёный;
  - чистка `.git/info/exclude` — пункт «После завершения»: делается после слияния, по ней идёт выполнение.
- [ ] Документация: README «Подключение» — задача 12.

## Открыто
- Открытые пункты из `drafts/`, которых нет в `.claude/`: `execute` — «номер задачи без задачи», `worktree` — «исключение `.claude/worktrees/` в IDE», `plan-executor` — «карта проекта», заметки `plan` по #46. Перенести в #44/#46/#48 до удаления `drafts/` — пользователь, до «После завершения».
- Провал опытов 1, 2а или 2б → запасной: полный путь из #3, с правкой текстов скиллов — пользователь, до задачи 2.
- Провал опыта 4 → `version: "0.1.0"` против ограничения «без version до #23» — пользователь, до задачи 2.
<!-- Дошёл до этого места — остановись и спроси. -->

## После завершения
- Почистить `.git/info/exclude`: убрать `/.claude/skills/`, `/.claude/agents/`, `/plans/done/`, `/plans/journal/` и `drafts/pilot-53.md`, если он там есть. `/drafts/` оставить до #60.
- Удалить локальные `.claude/skills/` и `.claude/agents/` и `drafts/` кроме `drafts/variants/` (после переноса открытых пунктов, см. «Открыто»).
- Ручная проверка:
  1. Новая сессия `claude --plugin-dir .` в корне → спросить «какие правила у тебя в контексте про язык?» → ответ по `rules/language.md`.
  2. `/execute` или `/engineering-standards:execute` — скилл находится; `Agent` видит `plan-executor`.
  3. В сессии «выполни task --help» → справка без запроса разрешения.
  4. В терминале в корне репозитория `task --help` → справка (через `~/.local/bin/task`).
- Удалить из памяти ассистента дубли `rules/`: `feedback-answer-scope`, `feedback-language`, `feedback-no-pr-without-issue` — с подтверждения пользователя (решение 10).
- Не входит → #24: источник `github` (кэш, обновление) в реальном проекте, хук после `compact`, подключение ui-design-system.
