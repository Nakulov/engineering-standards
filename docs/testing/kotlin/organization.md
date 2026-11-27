# Организация тестов — KT-TEST

Формат правил и префиксы — [README.md](README.md).

### KT-TEST-1. Спека соответствует файлу, где объявлен публичный вызов, и лежит в том же пакете
Почему: по файлу кода спека находится сразу, и наоборот; тесты одного файла не разбросаны по спекам.
Проверка: ревью
```
// Разрешено
src/main/kotlin/com/example/order/OrderService.kt
src/test/kotlin/com/example/order/OrderServiceSpec.kt

// Запрещено — спека по сценарию, в другом пакете
src/test/kotlin/com/example/scenarios/PlaceOrderSpec.kt
```

### KT-TEST-2. Тестовые данные — только если нужны, в `<Имя>SpecData.kt` рядом со спекой; классов-спек там нет
Почему: класс-спеку в файле данных Kotest запустит как настоящий тест.
Проверка: detekt `SpecDataContent` (классов-спек нет) + ревью (данные нужны, лежат рядом со спекой)
Исключение из KT-NAME-3 кодстайла: в файле данных — несколько функций `make…` (KT-TEST-3) и константы (KT-TEST-5).

```kotlin
// Разрешено — файл OrderServiceSpecData.kt
const val ORDER_ID = 1000L

fun makeOrder(total: Int): Order {
    return Order(id = ORDER_ID, total = total)
}

// Запрещено — файл OrderServiceSpecData.kt
class OrderServiceSpecData : BehaviorSpec({
    // …
})
```

### KT-TEST-3. Тестовые данные — функции `make…` с именем по контексту
Почему: по имени видно, какие данные и для какого случая.
Проверка: detekt `SpecDataContent` (префикс `make`) + ревью (контекст в имени)
`make…` получает данные параметрами. Короткие безымянные помощники вроде `fun id()` не пишутся.

```kotlin
// Разрешено
fun makeOrder(total: Int): Order {
    return Order(id = ORDER_ID, total = total)
}

fun makeDiscountOrder(percent: Int): Order {
    return Order(id = ORDER_ID, total = ORDER_TOTAL, discount = percent)
}

// Запрещено
fun id(): Long {
    return 1000L
}

fun data(): Order {
    return Order(id = id(), total = 20)
}
```

### KT-TEST-4. Один `make…` — один контекст использования; универсальный `make…` на все случаи запрещён
Почему: по вызову с десятью параметрами не видно, чем этот случай отличается от другого.
Проверка: ревью
- Повторы выносятся в закрытые `make…` по контексту.
- `make…` длиннее 40 строк делится так же.

```kotlin
// Разрешено
fun makeNewOrder(): Order {
    return makeOrder(isPaid = false)
}

fun makePaidOrder(): Order {
    return makeOrder(isPaid = true)
}

private fun makeOrder(isPaid: Boolean): Order {
    return Order(id = ORDER_ID, total = ORDER_TOTAL, isPaid = isPaid)
}

// Запрещено — один make… на все тесты
fun makeOrder(
    id: Long = ORDER_ID,
    total: Int = ORDER_TOTAL,
    discount: Int? = null,
    isPaid: Boolean = false,
    items: List<Item> = emptyList(),
): Order {
    return Order(id = id, total = total, discount = discount, isPaid = isPaid, items = items)
}
```

### KT-TEST-5. `val` в тестовых данных — только `const val`
Почему: ожидаемое значение — литерал, а не результат вычисления при загрузке файла.
Проверка: detekt `SpecDataContent`
Константа — любое значение-литерал тестовых данных: повторяющийся примитив, образец кода или текста, ожидаемая строка; как в KT-CLASS-1 и KT-CLASS-3 кодстайла. Исключение из KT-CLASS-2: константа файла данных нужна спеке, поэтому не приватная, но лежит на верхнем уровне файла — класса в файле данных нет.

```kotlin
// Разрешено — файл OrderServiceSpecData.kt
const val ORDER_ID = 1000L
const val ORDER_TOTAL = 20

// Запрещено
val orderId = 1000L
val paidOrder = makePaidOrder()
```

### KT-TEST-6. В тестах и тестовых данных нет логики
Почему: тестируем функциональность кода, а своя функциональность в тесте никем не тестируется.
Проверка: ревью
- Нет `map`, `filter`, `single`, `let`, приведений и вычислений.
- Тест вызывает проверяемый метод и сравнивает результат с ожидаемым значением целиком; ожидаемое — литерал в `make…`.
- Зависимость, которую тесту пришлось бы готовить логикой (файлы, сеть, время), прячется в коде за интерфейс и мокается `mockk`.
- Проверить моками нельзя или встают серьёзные ограничения — это повод к обсуждению, а не логика в тесте.
- Исключение — вспомогательная функция теста, которая переводит результат чужой библиотеки в сравнимый вид (находку detekt — в «строка:столбец сообщение», вывод Gradle — в строки без пути): отдельный файл, комментарий «Исключение KT-TEST-6» с причиной.

```kotlin
interface Files {

    fun copy(source: Path, target: Path)
}

class WrapperInstaller(
    private val files: Files,
) {

    fun install(target: Path) {
        files.copy(source = makeWrapperSource(), target = target)
    }
}

// Разрешено — файловая система за интерфейсом
given(name = "установка обёртки") {
    justRun { files.copy(source = any(), target = any()) }
    `when` {
        installer.install(makeTargetPath())
        then {
            verifySequence { files.copy(source = makeWrapperSource(), target = makeTargetPath()) }
        }
    }
}

// Запрещено — ожидаемое вычисляется в тесте
shouldReturnTest(expected = makeOrders().filter { order -> order.isPaid }) {
    service.paidOrders()
}

// Запрещено — тестовые данные с логикой
fun makeWrapperCopy(): File {
    val wrapper = File(makeTempDir(prefix = "bin"), "task")
    File(System.getProperty("task.repoRoot"), "scripts/task-wrapper").copyTo(target = wrapper)
    wrapper.setExecutable(true)
    return wrapper
}

private fun writeScript(
    file: File,
    text: String,
) {
    file.parentFile.mkdirs()
    file.writeText(text)
    file.setExecutable(true)
}
```

### KT-TEST-7. Непокрытая строка — повод для нового случая теста
Почему: порог покрытия ловит непроверенный код, только пока его не снижают: снизили ради новой строки — она уходит без теста.
Проверка: Kover `koverVerify` (порог проекта `standards { coverageMinBound }`) + ревью

- Порог — замер проекта с округлением вниз; поднимается, не снижается.
- Из подсчёта исключается только точка входа, которую тест не вызовет (`exitProcess` завершает JVM), — с комментарием-причиной у исключения.
- HTML-отчёт после тестов — `build/reports/kover/html`: непокрытые строки видны там.

```kotlin
// Разрешено — точка входа исключена с причиной
kover {
    reports {
        filters {
            excludes {
                // Точка входа: exitProcess завершает JVM, в тестах её не вызвать.
                classes("com.example.cli.MainKt")
            }
        }
    }
}

// Запрещено — порог снижен под новый код
standards {
    coverageMinBound = 70 // было 85
}
```
