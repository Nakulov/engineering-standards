# Форматирование — KT-FMT

Формат правил и префиксы — [README.md](README.md).

### KT-FMT-1. Параметры конструктора — каждый с новой строки
Почему: параметры читаются столбцом, а новый параметр — одна строка в диффе.
Проверка: detekt `ClassSignature` (ktlint, основной код) + ревью (тесты: правило там выключено, чтобы спека писалась `class … : BehaviorSpec({` одной строкой)
Правило действует и для одного параметра. Перед `@Inject constructor` — перенос строки. Параметры функции — KT-FMT-10.

```kotlin
// Разрешено
class Foo(
    private val firstArgument: String,
    private val secondArgument: String,
)

class Bar(
    private val argument: String,
)

class Baz
@Inject constructor(
    private val firstArgument: String,
    private val secondArgument: String,
)

// Запрещено
class Foo(private val firstArgument: String, private val secondArgument: String)

class Bar(private val argument: String)

class Baz @Inject constructor(
    private val argument: String,
)
```

### KT-FMT-2. Цепочка вызовов — каждый следующий вызов с новой строки
Почему: каждый шаг преобразования — своя строка, цепочка читается по шагам.
Проверка: ревью
Правило — для цепочки от двух звеньев: вызовов или свойств (`a.b.c`, `a?.b?.c`). Первое звено остаётся на строке с получателем, каждое следующее — с новой строки.

```kotlin
// Разрешено
listOf("first", "second").map { value -> value.length }
    .filter { length -> length > 1 }

// Запрещено
listOf("first", "second").map { value -> value.length }.filter { length -> length > 1 }

listOf("first", "second")
    .map { value -> value.length }
    .filter { length -> length > 1 }
```

### KT-FMT-3. Тело класса, интерфейса и `companion object` начинается с пустой строки
Почему: объявление визуально отделено от тела, особенно когда у объявления длинный список параметров.
Проверка: ревью (правило ktlint `no-empty-first-line-in-class-body` из стиля `ktlint_official` требует обратного — выключается)
Исключение — `enum`: значения идут сразу после объявления.

```kotlin
// Разрешено
class Foo {

    fun bar() {
        // …
    }
}

interface Bar {

    fun foo()
}

class Account {

    companion object {

        fun login() {
            // …
        }
    }
}

enum class Color {
    RED,
    GREEN,
}

// Запрещено
class Foo {
    fun bar() {
        // …
    }
}
```

### KT-FMT-4. Каждая аннотация — на своей строке
Почему: аннотации читаются списком, а добавление аннотации — одна строка в диффе.
Проверка: detekt `AnnotationOnSeparateLine` (набор formatting)
```kotlin
// Разрешено
@Inject
@JvmStatic
fun annotatedFunction() {
    // …
}

@Inject
lateinit var injectedObject: String

// Запрещено
@Inject @JvmStatic fun annotatedFunction() {
    // …
}

@Inject lateinit var injectedObject: String
```

### KT-FMT-5. Функция, которая возвращает `Unit`, не объявляется через `=`
Почему: `=` читается как «возвращает значение»; у функции-действия тело в `{}`.
Проверка: ревью
```kotlin
val account = Account()

// Разрешено
fun doSome() {
    account.invalidate()
}

// Запрещено
fun doSome() = account.invalidate()
```

### KT-FMT-6. У функции с телом-выражением тип результата указан явно
Почему: тип результата виден без вывода по выражению, и правка выражения не меняет тип функции молча.
Проверка: ревью
```kotlin
// Разрешено
fun getAccountId(): Int = account.id

// Запрещено
fun getAccountId() = account.id
```

### KT-FMT-7. `with`, `also`, `run`, `apply` запрещены
Почему: они вносят слишком много синтаксического сахара и захватывают контекст — особенно вложенные друг в друга. Это осознанное ограничение, чтобы один и тот же код не писался везде по-разному.
Проверка: detekt `ForbiddenMethodCall`
`let` и `takeIf` разрешены.

```kotlin
// Разрешено
val account = Account()
account.name = name
account.email = email

val length = name?.let { value -> value.length }
val validName = name.takeIf { value -> value.isNotBlank() }

// Запрещено
val account = Account().apply {
    name = name // какое name — свойство или параметр?
    email = email
}

with(account) {
    invalidate()
}
```

### KT-FMT-8. Ветки `if` / `else` — с новой строки
Почему: ветка в фигурных скобках на своих строках не путается с соседним кодом, а её правка — отдельная строка в диффе.
Проверка: ревью
Однострочный `if` разрешён только для выхода (`return`, `throw`, `continue`, `break`) или присваивания.

```kotlin
// Разрешено
if (isValid) {
    doSomething()
} else {
    doAnother()
}

if (!isValid) return

val label = if (isValid) "ok" else "error"

// Запрещено
if (isValid) doSomething() else doAnother()

if (isValid) doSomething()
```

### KT-FMT-9. Простая двоичная проверка — `if` / `else`, а не `when`
Почему: `when` обещает несколько вариантов; для «да / нет» он длиннее и прячет, что вариантов два.
Проверка: ревью
Простая двоичная проверка — `when` по `Boolean` или `when` с одним условием и `else`. Исключение — `enum`, `sealed class`, `sealed interface`: для них `when` проверяет, что разобраны все варианты.

```kotlin
// Разрешено
if (isValid) {
    doSomething()
} else {
    doAnother()
}

when (state) {
    is DrawingState.Idle -> doSomething()
    is DrawingState.Drawing -> doAnother()
}

// Запрещено
when (isValid) {
    true -> doSomething()
    false -> doAnother()
}

when {
    isValid -> doSomething()
    else -> doAnother()
}
```

### KT-FMT-10. Параметры функции — каждый с новой строки, если их три и больше
Почему: длинный список параметров читается столбцом, а новый параметр — одна строка в диффе; один-два параметра читаются и одной строкой.
Проверка: detekt `FunctionSignature` (ktlint)
Один-два параметра — одной строкой, пока сигнатура помещается в 120 символов (`max_line_length` в `.editorconfig`); не помещается — каждый параметр с новой строки. Правило действует и в тестах. Параметры конструктора — KT-FMT-1, аргументы вызова — KT-FMT-11.

```kotlin
// Разрешено
fun updatePassword(password: String) {
    // …
}

fun createAccountRequest(user: UserModel, password: String): AccountRequest {
    // …
}

fun createAccount(
    name: String,
    email: String,
    password: String,
): AccountModel {
    // …
}

// Запрещено
fun createAccountRequest(
    user: UserModel,
    password: String,
): AccountRequest {
    // …
}

fun createAccount(name: String, email: String, password: String): AccountModel {
    // …
}
```

### KT-FMT-11. Аргументы вызова — каждый с новой строки, если их три и больше
Почему: длинный список аргументов читается столбцом, а новый аргумент — одна строка в диффе; один-два аргумента читаются и одной строкой.
Проверка: ревью (порог по числу аргументов) + detekt `ArgumentListWrapping`, `MaximumLineLength` (ktlint: вызов длиннее 120 символов)
Один-два аргумента — одной строкой, пока вызов помещается в 120 символов (`max_line_length` в `.editorconfig`); не помещается — каждый аргумент с новой строки. Считаются аргументы в скобках: позиционные, именованные, `*spread`, лямбда внутри скобок. Лямбда после скобок и аргументы типа не считаются. Правило действует и в тестах. Параметры объявления — KT-FMT-1 и KT-FMT-10.

```kotlin
// Разрешено
updatePassword(password)

createAccountRequest(user, password)

createAccount(
    name,
    email,
    password,
)

val names = listOf(
    "first",
    "second",
    "third",
)

retry(times, delay) { attempt ->
    // …
}

// Запрещено
createAccountRequest(
    user,
    password,
)

createAccount(name, email, password)

val names = listOf("first", "second", "third")
```
