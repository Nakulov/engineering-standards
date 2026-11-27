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
