# Классы, объекты, интерфейсы, константы — KT-CLASS

Формат правил и префиксы — [README.md](README.md).

### KT-CLASS-1. Приватная константа — на верхнем уровне файла, над классом
Почему: константа нужна только этому файлу — `companion object` ради неё не заводится, и она видна сразу при открытии файла.
Проверка: ревью
```kotlin
// Разрешено — файл AccountParser.kt
private const val ACCOUNT_ID = "id"

class AccountParser

// Запрещено
class AccountParser {

    private companion object {

        const val ACCOUNT_ID = "id"
    }
}
```

### KT-CLASS-2. Публичная константа — в `companion object`
Почему: константа читается через имя класса (`AccountParser.ACCOUNT_ID`) — видно, к чему она относится.
Проверка: ревью
```kotlin
// Разрешено
class AccountParser {

    companion object {

        const val ACCOUNT_ID = "id"
    }
}

// Запрещено — файл AccountParser.kt
const val ACCOUNT_ID = "id"

class AccountParser
```

### KT-CLASS-3. Значение, которое встречается больше одного раза, — в константу
Почему: одно значение в двух местах расходится при первой же правке одного из них.
Проверка: detekt `MagicNumber` (числа) + ревью
Для пустой строки константа не нужна: `""` и так однозначна.

```kotlin
// Разрешено
private const val ACCOUNT_ID = "id"

fun parse(json: JsonObject): String {
    return json.getString(ACCOUNT_ID)
}

fun write(json: JsonObject, id: String) {
    json.put(ACCOUNT_ID, id)
}

// Запрещено
fun parse(json: JsonObject): String {
    return json.getString("id")
}

fun write(json: JsonObject, id: String) {
    json.put("id", id)
}

private const val EMPTY = ""
```

### KT-CLASS-4. `companion object` — первым в теле класса
Почему: константы и фабрики класса видны сразу при открытии файла.
Проверка: ревью (detekt `ClassOrdering` не подходит — ставит `companion object` в конец)
Объявление класса, пустая строка (KT-FMT-3), `companion object`.

```kotlin
// Разрешено
class AccountParser {

    companion object {

        const val ACCOUNT_ID = "id"
    }

    val format: String = "json"
}

// Запрещено
class AccountParser {

    val format: String = "json"

    companion object {

        const val ACCOUNT_ID = "id"
    }
}
```

### KT-CLASS-5. Порядок тела класса
Почему: класс читается сверху вниз одинаково в любом файле — нужное находится без поиска.
Проверка: ревью
0. `companion object` (KT-CLASS-4).
1. Неизменяемые публичные свойства (`val`).
2. Изменяемые публичные свойства (`var`).
3. Неизменяемые приватные свойства.
4. Изменяемые приватные свойства.
5. Блоки `init`.
6. Абстрактные функции.
7. Функции `override`.
8. Публичные функции.
9. Приватные функции.

Вложенных классов в теле нет — KT-CLASS-9.

`internal` и `protected` идут вместе с публичными. Внутри одной группы функция стоит ближе к месту первого использования.

```kotlin
// Разрешено
interface Bar {

    fun barFun()
}

abstract class Foo : Bar {

    companion object {

        const val DEFAULT_ID = "default"
    }

    val globalId: String = DEFAULT_ID
    val globalAccountId: String = DEFAULT_ID

    var globalMutableId: String = DEFAULT_ID
    var globalMutableAccountId: String = DEFAULT_ID

    private val internalId: String = DEFAULT_ID
    private val internalAccountId: String = DEFAULT_ID

    private var internalMutableId: String = DEFAULT_ID
    private var internalMutableAccountId: String = DEFAULT_ID

    init {
        // …
    }

    abstract fun fooAbstractFun1()

    abstract fun fooAbstractFun2()

    override fun barFun() {
        // …
    }

    fun globalFooFun1() {
        // …
    }

    fun globalFooFun2() {
        internalFooFun1()
        internalFooFun2()
    }

    private fun internalFooFun1() {
        // …
    }

    private fun internalFooFun2() {
        // …
    }
}
```

### KT-CLASS-6. Класс только для своего модуля — `internal`
Почему: что не нужно снаружи, не становится API модуля и меняется без оглядки на других.
Проверка: ревью
```kotlin
// Разрешено
internal class Foo

// Запрещено — Foo используется только внутри модуля
class Foo
```

### KT-CLASS-7. Nullable-поле data-класса — в конце и со значением `= null`
Почему: обязательные поля идут первыми, а при создании необязательные можно не перечислять.
Проверка: ревью
```kotlin
// Разрешено
data class Foo(
    val property1: String,
    val property2: String,
    val property3: String? = null,
)

// Запрещено
data class Foo(
    val property3: String?,
    val property1: String,
    val property2: String,
)
```

### KT-CLASS-8. Data-класс создаётся с именами всех аргументов
Почему: при перестановке полей одного типа вызов по позициям молча меняет значения местами.
Проверка: ревью
Правило строже KT-FUN-5 («больше трёх аргументов — все с именами»): для data-класса имена нужны всегда.

```kotlin
// Разрешено
val foo = Foo(
    property1 = "value1",
    property2 = "value2",
    property3 = "value3",
)

val foo = Foo(
    property1 = "value1",
    property2 = "value2",
)

// Запрещено
val foo = Foo("value1", "value2")
```

### KT-CLASS-9. Вложенные классы запрещены
Почему: вложенный класс — вторая декларация в файле, его не найти по имени файла (KT-NAME-3).
Проверка: ревью

Исключения: `companion object` (KT-CLASS-4) и наследники `sealed`-класса или интерфейса (KT-NAME-3).

```kotlin
// Запрещено
class Account {

    class Address
}

// Разрешено — файл Address.kt
class Address

// Разрешено — наследники sealed внутри него
sealed interface DrawingState {

    data object Idle : DrawingState
}
```
