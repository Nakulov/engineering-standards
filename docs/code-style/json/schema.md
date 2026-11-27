# Схемы — JSON-SCHEMA

Формат правил и префиксы — [README.md](README.md).

### JSON-SCHEMA-1. У файла данных есть описание формата и проверка по нему
Почему: файл без проверки ломается молча — опечатку в ключе или неверное значение находит уже программа, которая его читает.
Проверка: ревью — новый файл данных без описания формата или вне проверки JSON
По умолчанию описание — JSON Schema. Свой формат проекта тоже подходит: например, файлы-образцы сверяются кодом по другому файлу проекта, который их описывает. Тогда формат описан в документе проекта, а сверяет его код проверки JSON. Связи между файлами — ссылка из одного файла на существующий другой — схема не выражает, их всегда проверяет код.

```text
# Запрещено — файл данных без описания формата
config.json

# Разрешено
config.json         ← config.schema.json, проверка JSON сверяет файл по схеме
```

### JSON-SCHEMA-2. Схема начинается с `"$schema": "https://json-schema.org/draft/2020-12/schema"`
Почему: 2020-12 — последняя версия JSON Schema, её поддерживают валидаторы и IDE JetBrains. Без `$schema` версию выбирает валидатор, и одна схема в разных инструментах значит разное.
Проверка: проверка JSON — валидатор 2020-12 сверяет саму схему (`check_schema`) + ревью
```jsonc
// Запрещено — версия не указана
{
  "type": "object"
}

// Разрешено
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object"
}
```

### JSON-SCHEMA-3. У корня схемы — `title` и `description`, у неочевидного поля — `description`; на русском
Почему: IDE показывает их в подсказке при наведении на ключ — документация там, где файл правят. Описание поля, понятного из имени, — шум.
Проверка: ревью
```jsonc
// Запрещено — что за файл и что значит 0, не сказано
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": {
    "retries": {
      "type": "integer"
    }
  }
}

// Разрешено
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "title": "Настройки сборки",
  "description": "Файл config.json в корне проекта: что и куда собирать.",
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "retries": {
      "type": "integer",
      "description": "Сколько раз повторить загрузку при сетевой ошибке; 0 — не повторять."
    }
  }
}
```

### JSON-SCHEMA-4. Объекты закрыты: `"additionalProperties": false`, а у объекта, собранного через `allOf`, — `"unevaluatedProperties": false`
Почему: без закрытия опечатка `"retires"` вместо `"retries"` проходит проверку, а поле молча не читается. `additionalProperties` видит только `properties` своего уровня и отвергает поля, добавленные через `allOf`; `unevaluatedProperties` учитывает их ([Understanding JSON Schema: unevaluatedProperties](https://json-schema.org/understanding-json-schema/reference/object#unevaluatedproperties)).
Проверка: ревью
Словарь — объект, ключи которого данные, — не закрывается, а описывает свои значения: `"additionalProperties": { "type": "string" }`.

```jsonc
// Запрещено — лишний ключ проходит
{
  "type": "object",
  "properties": {
    "retries": {
      "type": "integer"
    }
  }
}

// Запрещено — additionalProperties отвергнет поле name из базовой схемы
{
  "allOf": [
    {
      "$ref": "#/$defs/base"
    }
  ],
  "properties": {
    "retries": {
      "type": "integer"
    }
  },
  "additionalProperties": false
}

// Разрешено
{
  "allOf": [
    {
      "$ref": "#/$defs/base"
    }
  ],
  "properties": {
    "retries": {
      "type": "integer"
    }
  },
  "unevaluatedProperties": false
}
```

### JSON-SCHEMA-5. Повторяющаяся часть — в `$defs`, ссылка — `$ref`
Почему: правило описано один раз; поменял — поменялось везде.
Проверка: ревью
```jsonc
// Запрещено — одно правило имени дважды
{
  "properties": {
    "name": {
      "type": "string",
      "pattern": "^[a-z][A-Za-z0-9]*$"
    },
    "parent": {
      "type": "string",
      "pattern": "^[a-z][A-Za-z0-9]*$"
    }
  }
}

// Разрешено
{
  "properties": {
    "name": {
      "$ref": "#/$defs/camelCaseName"
    },
    "parent": {
      "$ref": "#/$defs/camelCaseName"
    }
  },
  "$defs": {
    "camelCaseName": {
      "type": "string",
      "pattern": "^[a-z][A-Za-z0-9]*$"
    }
  }
}
```

### JSON-SCHEMA-6. Файл схемы — `<что описывает>.schema.json`
Почему: по имени видно, что это схема и какой файл она описывает; маска `*.schema.json` отделяет схемы от данных в проверке и в настройках IDE.
Проверка: ревью
```text
# Запрещено
schemas/config.json
config-schema.json

# Разрешено
config.schema.json
```

Не требуется: `description` у каждого поля (шум у понятных из имени) и `$id` — абсолютный адрес нужен схеме, которую публикуют наружу, а наши живут в репозитории.
