# Нейминг — KT-NAME

Формат правил и префиксы — [README.md](README.md).

### KT-NAME-1. Модули — `kebab-case`
Почему: так советует Gradle — «Use lower case hyphenation for all project names» ([Multi-project builds, Naming recommendations](https://docs.gradle.org/current/userguide/multi_project_builds.html)).
Проверка: ревью
```text
// Разрешено
some-new-module
module

// Запрещено
some_new_module
Some-New-Module
Module
```

### KT-NAME-2. Пакеты — нижний регистр, слова слитно, без `_` и `-`
Почему: соглашение Java о пакетах; `-` в имени пакета не компилируется, `_` и заглавные выбиваются из соглашения.
Проверка: detekt `PackageNaming`
```kotlin
// Разрешено
package com.example.feature.read
package com.example.feature.readfile

// Запрещено
package com.example.feature.read_file
package com.example.feature.readFile
package com.example.feature.read-file // не компилируется
```

### KT-NAME-3. Одна верхнеуровневая декларация на файл, имя файла — её имя
Почему: класс находится по имени файла; файл с несколькими классами прячет их от поиска.
Проверка: detekt `MatchingDeclarationName`
Рядом с декларацией в том же файле допустимы: приватные константы (KT-CLASS-1), `typealias` (KT-DOC-4), расширения этого класса (KT-FUN-1). Наследники `sealed`-класса или интерфейса объявляются внутри него, а не рядом.

```kotlin
// Разрешено — файл Foo.kt
class Foo

// Разрешено — файл DrawingState.kt
sealed interface DrawingState {

    data object Idle : DrawingState

    data class Drawing(
        val points: Int,
    ) : DrawingState
}

// Запрещено — файл Foo.kt
class Foo

class Bar

// Запрещено — файл DrawingState.kt
sealed interface DrawingState

data object Idle : DrawingState
```

### KT-NAME-4. Суффикс модели — по слою
Почему: суффикс показывает слой, и модель сети не попадает в бизнес-слой незаметно.
Проверка: ревью
Правило применяется, если в проекте есть такой слой.

| Модель | Суффикс |
|--------|---------|
| ответ сетевого слоя, верхний уровень | `Response` |
| запрос сетевого слоя, верхний уровень | `Request` |
| вложенная модель сетевого слоя | `Dto` |
| модель бизнес-слоя | `Model` |
| состояние | `State` |
| остальные | имя по смыслу и контексту использования |

```kotlin
// Разрешено
// Сетевой слой
data class UserDto(
    val name: String,
)

data class AccountResponse(
    val user: UserDto,
)

data class AccountRequest(
    val user: UserDto,
)

// Бизнес-слой
data class UserModel(
    val name: String,
)

data class AccountModel(
    val user: UserModel,
)

// Состояние
sealed interface DrawingState

// Остальные
data class Refresh(
    val count: Int,
)

// Запрещено
data class AccountModel(
    val user: UserDto, // модель сети в бизнес-слое
)
```

### KT-NAME-5. Имя говорит, что делает
Почему: по имени читатель решает, открывать ли код; неточное имя заставляет читать реализацию.
Проверка: detekt `ForbiddenClassName` (для классов) + ревью
- Имя однозначное и отвечает одной зоне ответственности.
- Без пустых слов: `data`, `tmp`, `helper`, `Utils`, `Manager`.
- Имя не расходится с поведением: `read…` не пишет, `is…` не меняет состояние.

```kotlin
// Разрешено
val isValueValid: Boolean
var memberCounter: Int

fun updatePassword(password: String) {
    // …
}

// Запрещено
class AccountManager
object StringUtils
val data: String
val tmp: Int

fun readConfig() {
    file.writeText(text = defaultConfig) // read… пишет
}
```

### KT-NAME-6. Функции, свойства и переменные — `camelCase`
Почему: соглашение Kotlin; константы — исключение, `UPPER_SNAKE_CASE` (KT-CLASS-1).
Проверка: detekt `FunctionNaming`, `VariableNaming`, `TopLevelPropertyNaming`
```kotlin
// Разрешено
val someValue: String
var someVariable: Int

fun someFunction() {
    // …
}

// Запрещено
val some_value: String
fun SomeFunction() {
    // …
}
```

### KT-NAME-7. Булево значение — с префиксом `is`, `are`, `has`, `can`, `should`
Почему: имя читается как вопрос с ответом «да / нет», и в условии видно, что проверяется.
Проверка: detekt `BooleanPropertyNaming` (свойства) + ревью (функции)
Правило — для свойств, переменных и функций, которые возвращают `Boolean`.

```kotlin
// Разрешено
val hasSomeValue: Boolean
var isLoading: Boolean

fun canSubmit(): Boolean {
    // …
}

// Запрещено
val loading: Boolean
fun submitAllowed(): Boolean {
    // …
}
```

### KT-NAME-8. Имя-предложение — повод разбить функцию
Почему: длинное имя с «и», «если» описывает несколько действий — функцию пора разбить или пересмотреть подход к задаче.
Проверка: ревью
```kotlin
// Запрещено
fun updateNameAndPasswordIfFieldsValid() {
    // …
}

// Разрешено
fun updateAccount(name: String, password: String) {
    if (!areFieldsValid(name, password)) return
    setName(name)
    setPassword(password)
}

private fun areFieldsValid(name: String, password: String): Boolean {
    // …
}

private fun setName(name: String) {
    // …
}

private fun setPassword(password: String) {
    // …
}
```

### KT-NAME-9. Имя функции с результатом говорит, что она возвращает значение
Почему: по имени видно, что вызов даёт значение, а не только меняет состояние.
Проверка: ревью
Префиксы: `get`, `create`, `make`, `produce`, `map`, `to`, `as`, `find`, `parse` и другие — по смыслу «возвращает».

```kotlin
// Разрешено
fun getAccount(): AccountModel {
    // …
}

fun createAccountRequest(user: UserModel, password: String): AccountRequest {
    // …
}

fun toModel(): AccountModel {
    // …
}

// Запрещено
fun account(): AccountModel {
    // …
}
```

### KT-NAME-10. Функция без результата не берёт префиксы функций с результатом
Почему: `get…` без результата обманывает читателя — он ждёт значение.
Проверка: ревью
```kotlin
// Запрещено
fun getAccount() {
    // …
}

fun getAccount(): Unit {
    // …
}

// Разрешено
fun loadAccount() {
    // …
}
```

### KT-NAME-11. Свойство или параметр функционального типа — с суффиксом по типу
Почему: тип лямбды читается из имени; одно соглашение вместо разных в каждом файле.
Проверка: ревью
Словарь — из `java.util.function`.

| Тип | Суффикс | Пример |
|-----|---------|--------|
| `() -> Unit` | `Action` | `onClickAction: () -> Unit` |
| `(A) -> Unit`, `(A, B) -> Unit` | `Consumer` | `accountConsumer: (AccountModel) -> Unit` |
| `() -> R` | `Supplier` | `accountSupplier: () -> AccountModel` |
| `(A) -> R` | `Function` | `accountFunction: (UserModel) -> AccountModel` |
| `(A) -> Boolean` | `Predicate` | `emailPredicate: (String) -> Boolean` |

```kotlin
// Разрешено
class AccountScreen(
    private val onClickAction: () -> Unit,
    private val emailPredicate: (String) -> Boolean,
)

// Запрещено
class AccountScreen(
    private val onClick: () -> Unit,
    private val validate: (String) -> Boolean,
)
```
