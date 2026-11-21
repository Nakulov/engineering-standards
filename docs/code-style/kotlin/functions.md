# Функции — KT-FUN

Формат правил и префиксы — [README.md](README.md).

### KT-FUN-1. Расширение — только на верхнем уровне файла, не внутри класса
Почему: расширение внутри класса неявно видит и меняет состояние этого класса — по вызову `foo.doSome()` не видно, что меняется `Bar`.
Проверка: ревью
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
Проверка: detekt `NamedLiteralArgument` (своё правило)
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
Проверка: detekt `NoImplicitIt` (своё правило)
Правило действует везде, включая лямбды тестовых DSL.

```kotlin
val names: List<String>

// Разрешено
names.map { name -> name.uppercase() }

// Запрещено
names.map { it.uppercase() }
```

### KT-FUN-7. Функция — не больше 40 строк и не больше двух уровней вложенных блоков
Почему: осознанное ограничение ради чистоты и читаемости — функция читается целиком, без прокрутки; третий уровень вложенности держат в голове, его выносят в функцию с именем.
Проверка: detekt `LongMethod`, `NestedBlockDepth`

```kotlin
// Запрещено — третий уровень вложенности
fun paid(orders: List<Order>): List<Order> {
    val result = mutableListOf<Order>()
    for (order in orders) {
        if (order.isPaid) {
            if (order.total > 0) {
                result.add(order)
            }
        }
    }
    return result
}

// Разрешено
fun paid(orders: List<Order>): List<Order> {
    return orders.filter { order -> order.isPaid && order.total > 0 }
}
```

### KT-FUN-8. Не больше двух `return`, ранний выход не считается
Почему: осознанное ограничение ради чистоты и читаемости — выход из функции виден в двух местах: в начале (ранний выход) и в конце; return посреди логики прячет ветки.
Проверка: detekt `ReturnCount`

```kotlin
// Разрешено — ранний выход в начале не считается
fun discount(order: Order?): Int {
    if (order == null) {
        return 0
    }
    if (order.isVip) {
        return VIP_DISCOUNT
    }
    return BASE_DISCOUNT
}

// Запрещено — три return в ветках when
fun discount(order: Order): Int {
    when {
        order.total > LARGE -> return LARGE_DISCOUNT
        order.isVip -> return VIP_DISCOUNT
        else -> return BASE_DISCOUNT
    }
}
```
