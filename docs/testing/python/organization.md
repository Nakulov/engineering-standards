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
