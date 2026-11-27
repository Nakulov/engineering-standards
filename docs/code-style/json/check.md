# Проверка — JSON-CHECK

Формат правил и префиксы — [README.md](README.md).

### JSON-CHECK-1. JSON проверяется одной командой — локально и в CI
Почему: правило без проверки нарушается незаметно; одна команда — та же самая у человека перед коммитом и в CI.
Проверка: ревью — команда описана в README проекта и запускается в CI
Команда сверяет раскладку (JSON-FMT-1), саму схему (JSON-SCHEMA-2) и файл данных по его описанию (JSON-SCHEMA-1); при ошибке выходит с ненулевым кодом. Инструмент выбирает проект: Python-проект — скрипт по образцу ниже, Kotlin-проект — тест Kotest.

```text
# Запрещено — проверка есть только в CI: ошибку видно уже после пуша
# Разрешено — одна команда; CI вызывает её же
python3 tools/check_json.py
```

### JSON-CHECK-2. Ошибка называет файл и путь до места в нём
Почему: по «файл не прошёл схему» ошибку ищут глазами по всему файлу; путь ведёт сразу к полю.
Проверка: ревью кода проверки
```text
# Запрещено
config.json: invalid

# Разрешено — путь в формате JSONPath; лишний ключ назван в сообщении
config.json: $.retries: -1 is less than the minimum of 0
config.json: $: Additional properties are not allowed ('retires' was unexpected)
```

### JSON-CHECK-3. Файл с JSON Schema подключён к IDE через `.idea/jsonSchemas.xml` в git
Почему: IDE подсказывает ключи, подсвечивает ошибки и показывает `description` прямо при правке — ошибка видна до запуска проверки. Файлы данных при этом не меняются: ключ `"$schema"` в данных стал бы служебным полем, которое схема и программа-читатель должны допускать.
Проверка: ревью — новая схема без строки в `.idea/jsonSchemas.xml`
Шаблон `.gitignore` стандарта исключает `.idea/*`; проект добавляет ниже строку `!.idea/jsonSchemas.xml`. IDE JetBrains пишет этот файл сама: Settings → Languages & Frameworks → Schemas and DTDs → JSON Schema Mappings. Маски IDE не умеют исключать файлы — когда маски не хватает, файлы перечисляют поимённо или файл соответствий генерирует скрипт проекта.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!-- Разрешено — config.json сверяется с config.schema.json -->
<project version="4">
  <component name="JsonSchemaMappingsProjectConfiguration">
    <state>
      <map>
        <entry key="Настройки сборки">
          <value>
            <SchemaInfo>
              <option name="name" value="Настройки сборки" />
              <option name="relativePathToSchema" value="config.schema.json" />
              <option name="patterns">
                <list>
                  <Item>
                    <option name="path" value="config.json" />
                    <option name="mappingKind" value="File" />
                  </Item>
                </list>
              </option>
            </SchemaInfo>
          </value>
        </entry>
      </map>
    </state>
  </component>
</project>
```

## Образец проверки на Python

Нужен пакет `jsonschema`. Карта `SCHEMAS` — какой файл какой схемой проверять. Связи между файлами и свой формат проекта (JSON-SCHEMA-1) проект дописывает сам.

```python
"""Проверка JSON проекта: раскладка и соответствие схеме."""

import json
import sys
from pathlib import Path

from jsonschema import Draft202012Validator

ROOT = Path(__file__).resolve().parent

# Файл данных → его схема; пути от корня проекта.
SCHEMAS = {
    "config.json": "config.schema.json",
}


def canonical(data: object) -> str:
    """Возвращает каноническую раскладку JSON.

    Args:
        data: Разобранный JSON.

    Returns:
        Текст: отступ 2, кириллица без экранирования, перевод строки в конце.
    """
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def load(name: str, errors: list[str]) -> object | None:
    """Читает JSON и сверяет раскладку.

    Args:
        name: Путь к файлу от корня проекта.
        errors: Список, в который добавляются ошибки.

    Returns:
        Разобранный JSON или None, если файл не разобрался.
    """
    text = (ROOT / name).read_text(encoding="utf-8")
    try:
        data = json.loads(text)
    except json.JSONDecodeError as error:
        errors.append(f"{name}:{error.lineno}: не JSON: {error.msg}")
        return None
    if text != canonical(data):
        errors.append(f"{name}: раскладка не каноническая")
    return data


def main() -> int:
    """Проверяет все файлы из SCHEMAS и их схемы.

    Returns:
        0 — ошибок нет, 1 — есть.
    """
    errors: list[str] = []
    for data_name, schema_name in SCHEMAS.items():
        schema = load(schema_name, errors)
        data = load(data_name, errors)
        if schema is None or data is None:
            continue
        Draft202012Validator.check_schema(schema)
        validator = Draft202012Validator(schema)
        for error in sorted(validator.iter_errors(data), key=lambda e: e.json_path):
            errors.append(f"{data_name}: {error.json_path}: {error.message}")
    for error in errors:
        print(error, file=sys.stderr)
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
```
