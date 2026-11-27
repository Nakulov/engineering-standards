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
