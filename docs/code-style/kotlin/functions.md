# Функции — KT-FUN

Формат правил и префиксы — [README.md](README.md).

### KT-FUN-1. Расширение — только на верхнем уровне файла, не внутри класса
Почему: расширение внутри класса неявно видит и меняет состояние этого класса — по вызову `foo.doSome()` не видно, что меняется `Bar`.
Проверка: ревью
Источник: #16

```kotlin
// Разрешено — файл Bar.kt
class Bar

private fun Bar.internalTransform() {
    // …
}

fun Bar.transform() {
    internalTransform()
}

// Запрещено — расширение чужого класса внутри класса
class Foo

class Bar {

    private var count = 0

    fun Foo.doSome() {
        count++ // не очевидно, что меняется Bar
    }
}

// Запрещено — приватное расширение внутри класса
class Bar {

    private fun String.toAccount(): AccountModel {
        // …
    }
}
```

### KT-FUN-2. Ссылка на функцию — только на функцию своего класса
Почему: в пределах своего класса видно, какой код вызывается; ссылка на чужую функцию или конструктор прячет это за `::`.
Проверка: ревью
Источник: #16

```kotlin
// Разрешено
data class Output(
    val value: String,
)

class Mapper {

    fun map(input: List<String>): List<Output> {
        return input.map(::transformStringToOutput)
    }

    private fun transformStringToOutput(value: String): Output {
        return Output(value = value)
    }
}

// Запрещено
class Mapper(
    private val formatter: Formatter,
) {

    fun map(input: List<String>): List<Output> {
        return input.map(formatter::format) // функция другого объекта
            .map(::Output) // конструктор
    }
}
```

### KT-FUN-3. Вложенные функции запрещены
Почему: вложенную функцию нельзя протестировать отдельно, и она прячет вторую ответственность внешней.
Проверка: ревью
Источник: #16

```kotlin
// Запрещено
fun foo() {
    fun bar() {}
    bar()
}

// Разрешено
fun foo() {
    bar()
}

private fun bar() {
    // …
}
```

### KT-FUN-4. Литерал в аргументе вызова — с именем параметра
Почему: по вызову `Property("size", null)` не понять, что значит `null`; имя параметра объясняет литерал на месте.
Проверка: detekt `NamedLiteralArgument` (своё правило, #17)
Источник: #16

- Литерал — число, `Boolean`, `Char`, строка, `null`.
- У вызова Java-метода имена параметров не стабильны: перед литералом пишется комментарий `/* имя = */`.
- Не затрагиваются аргументы аннотаций, `vararg` и инфиксные вызовы (`"a" to 1`).

```kotlin
data class Property(
    val name: String,
    val default: String? = null,
)

// Разрешено
Property(
    name = "size",
    default = null,
)
System.err.println(/* x = */ "ошибка")

// Запрещено
Property("size", null)
System.err.println("ошибка")
```

### KT-FUN-5. Больше трёх аргументов в вызове — все с именами
Почему: в длинном списке аргументов позиция не говорит, какой за что отвечает.
Проверка: detekt `NamedArguments`
Источник: #16

```kotlin
data class Property(
    val name: String,
    val age: Int,
    val weight: Int,
    val default: String? = null,
)

// Разрешено
Property(
    name = name,
    age = age,
    weight = weight,
    default = default,
)

// Запрещено
Property(name, age, weight, default)
```

### KT-FUN-6. Параметр лямбды — с именем, неявный `it` не пишется
Почему: имя говорит, что за значение; во вложенных лямбдах `it` неоднозначен.
Проверка: detekt `NoImplicitIt` (своё правило, #17)
Источник: #16

Правило действует везде, включая лямбды тестовых DSL.

```kotlin
val names: List<String>

// Разрешено
names.map { name -> name.uppercase() }

// Запрещено
names.map { it.uppercase() }
```
