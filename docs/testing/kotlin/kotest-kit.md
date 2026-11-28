# Формы тестов Kotest и KotestKit — KT-KIT

Формат правил и префиксы — [README.md](README.md).
Написано по KotestKit `v2.2.0`; переедет в README KotestKit.

### KT-KIT-1. Спеки — только `BehaviorSpec`
Почему: один стиль тестов во всех проектах — тесты не смешивают разные стили Kotest.
Проверка: detekt `BehaviorSpecOnly` (своё правило из engineering-standards)
```kotlin
// Разрешено
class OrderServiceSpec : BehaviorSpec({
    // …
})

// Запрещено
class OrderServiceSpec : FunSpec({
    test(name = "заказ сохраняется") {
        // …
    }
})
```

### KT-KIT-2. Функциональность с побочным эффектом — тест из `given`, `when`, `then`; в `given` один `when`, в `when` один `then`
Почему: тест — один сценарий проверяемой функциональности; второй сценарий — второй `given`.
Проверка: ревью
Побочный эффект — вызовы моков, запись. Функция с результатом — KT-KIT-8, с исключением — KT-KIT-9.
`when` и `then` без имени — из KotestKit.

```kotlin
data class Order(
    val id: Long,
    val total: Int,
)

interface OrderRepository {

    suspend fun save(order: Order)
}

class OrderService(
    private val repository: OrderRepository,
) {

    suspend fun place(order: Order) {
        repository.save(order)
    }
}

// Разрешено
class OrderServiceSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<OrderRepository>()
    val service = OrderService(repository = repository)

    given(name = "новый заказ") {
        val order = Order(id = 1000L, total = 20)
        coJustRun { repository.save(order) }
        `when` {
            service.place(order)
            then {
                coVerifySequence { repository.save(order) }
            }
        }
    }
})

// Запрещено — два сценария в одном given
given(name = "новый заказ") {
    `when` {
        service.place(order)
        then {
            // …
        }
    }
    `when` {
        service.place(order.copy(total = 0))
        then {
            // …
        }
    }
}
```

### KT-KIT-3. Моки — `mockk`, заглушки вызовов — `every`, `coEvery`, `justRun`, `coJustRun`
Почему: один способ подменить зависимость — тест читается одинаково во всех проектах; мок `mockk` проверяется `verifySequence` (KT-KIT-4), ручной фейк — нет.
Проверка: ревью
```kotlin
// Разрешено — мок без заглушек: каждый вызов без every падает
val gitHub = mockk<GitHub>()

// Разрешено — заглушки при создании мока
val gitHub = mockk<GitHub> {
    every { read(makeAdminArgs()) } returns "true"
}

// Разрешено — функции с Unit не требуют заглушек
val gitHub = mockk<GitHub>(relaxUnitFun = true)

// Разрешено — заглушка вызова на один случай; общие заглушки уровня спеки — KT-KIT-13
given(name = "администратор") {
    every { gitHub.read(makeAdminArgs()) } returns "true"
    coEvery { gitHub.load(makeAdminArgs()) } returns "true"
    // …
}

// Разрешено — заглушка вызова с Unit на один случай
given(name = "пауза") {
    justRun { pause.invoke(any()) }
    coJustRun { pause.invoke(any()) }
    // …
}

// Запрещено — ручной фейк
class FakeGitHub : GitHub {

    override fun read(args: List<String>): String {
        return "true"
    }
}

// Запрещено — every … just Runs вместо justRun
every { pause.invoke(any()) } just Runs
```

### KT-KIT-4. Проверка вызовов — `verifySequence`; `verify` — только для крайних случаев
Почему: `verifySequence` — самая строгая проверка: все вызовы мока, их порядок и отсутствие лишних.
Проверка: ревью
- `verify` (`coVerify`) — только для крайних случаев поведения, которое уже проверено `verifySequence` в другом тесте.
- `verifyOrder` (`coVerifyOrder`) запрещён: `verifySequence` проверяет порядок строже.
- `confirmVerified` без предшествующего `verify` — проверка, что при условии мок не вызывался. `verify { … wasNot Called }` для этого не используется — одна форма.

```kotlin
// Разрешено
then {
    coVerifySequence {
        repository.find(id = 1000L)
        repository.save(order)
    }
}

// Разрешено — при условии мок не вызывается
given(name = "пустой заказ") {
    `when` {
        service.place(order.copy(total = 0))
        then {
            confirmVerified(repository)
        }
    }
}

// Запрещено — verify основной проверкой: лишний вызов save не заметен
then {
    coVerify { repository.save(order) }
}

// Запрещено
then {
    coVerifyOrder {
        repository.find(id = 1000L)
        repository.save(order)
    }
}
```

### KT-KIT-5. `mockkStatic`, `mockkObject`, `mockkConstructor` — только с блоком, в пределах теста
Почему: после блока мок снимается сам — подмена не утекает в другие тесты.
Проверка: ревью
- Java-статики — через класс: `mockkStatic(URI::class) { … }`.
- Функции верхнего уровня и расширения — через ссылку на функцию: `mockkStatic(::formatPrice) { … }`. Исключение из KT-FUN-2 кодстайла: у такой функции нет класса, на который можно сослаться в коде.
- Строковая форма `mockkStatic("com.example.PriceKt")` запрещена: ломается при переименовании файла без ошибки компиляции.

```kotlin
// Разрешено
given(name = "адрес сервера") {
    mockkStatic(URI::class) {
        every { URI.create(any()) } returns makeServerUri()
        `when` {
            // …
            then {
                // …
            }
        }
    }
}

// Разрешено — функция верхнего уровня
mockkStatic(::formatPrice) {
    // …
}

// Запрещено — мок без блока остаётся на всю спеку
mockkStatic(URI::class)

// Запрещено — строковая форма
mockkStatic("com.example.PriceKt") {
    // …
}
```

### KT-KIT-6. Спека с моками ставит `isolationMode = IsolationMode.InstancePerRoot`; `clearMocks` и `beforeTest` для сброса не пишутся
Почему: каждый корневой `given` получает новый экземпляр спеки и новые моки; ручной сброс легко забыть — заглушки одного теста влияют на другой.
Проверка: ревью
```kotlin
// Разрешено
class OrderServiceSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<OrderRepository>()
    // …
})

// Запрещено
class OrderServiceSpec : BehaviorSpec({
    val repository = mockk<OrderRepository>()

    beforeTest {
        clearMocks(repository)
    }
    // …
})
```

### KT-KIT-7. Параметризованные тесты — только `withGivens`
Почему: своя надстройка KotestKit — один стиль параметризованных тестов во всех проектах; новый случай набора — строка `case`, а не копия теста.
Проверка: ревью
Два и больше однотипных случая одной функции — один `withGivens`. Однотипные — различаются только входом и ожидаемым значением, тело теста одно. Случаи с разной проверкой — например, один возвращает значение, другой бросает исключение — отдельные тесты. Общая подготовка — не повод писать однотипные случаи отдельно: `withGivens` — только в корне спеки, подготовка — в его теле.
Имя случая: `nameFn` показывает вход. `TestOptions(name = …)` — только когда имя теста по умолчанию (`300 -> 270`) нечитаемо; тогда в нём смысл случая.

```kotlin
// Разрешено
withGivens(
    nameFn = { case -> "гитхаб аргументы: ${case.arg1}" },
    case(arg1 = makeAdminArgs(), arg2 = "true"),
    case(arg1 = makeGuestArgs(), arg2 = "false"),
) { args, expected ->
    every { gitHub.read(args) } returns expected
    // …
}

// Запрещено — withData из Kotest
withData(makeAdminArgs(), makeGuestArgs()) { args ->
    // …
}

// Запрещено — цикл по случаям
listOf(makeAdminArgs(), makeGuestArgs()).forEach { args ->
    given(name = "гитхаб аргументы: $args") {
        // …
    }
}

// Разрешено — вход нечитаем в имени по умолчанию: смысл случая в TestOptions
withGivens(
    nameFn = { case -> "заказ: ${case.arg2.id}" },
    case(arg1 = "пустая корзина", arg2 = makeEmptyOrder(), arg3 = 0),
    case(arg1 = "скидка на всё", arg2 = makeDiscountedOrder(), arg3 = 90),
) { name, order, expected ->
    shouldReturnTest(expected, options = TestOptions(name)) {
        checkout.total(order)
    }
}

// Запрещено — однотипные случаи отдельными тестами
shouldReturnTest(expected = 270, options = TestOptions(name = "цена: 300")) {
    discount.priceFor(price = 300)
}
shouldReturnTest(expected = 360, options = TestOptions(name = "цена: 400")) {
    discount.priceFor(price = 400)
}
```

### KT-KIT-8. Функция с результатом без побочного эффекта — `shouldReturnTest` в корне спеки
Почему: результат проверяется одним вызовом без `when` и `then`; имя теста строится из ожидаемого значения и входа.
Проверка: ревью
`shouldReturnTest` — самостоятельный тест. Контейнер вокруг нужен только для набора случаев (`withGivens`) или общей подготовки (`given`); в `given` с общей подготовкой тестов может быть несколько.

```kotlin
// Разрешено — значение, с заглушкой мока
shouldReturnTest(expected = 20, options = TestOptions(name = "налог по ставке страны")) {
    every { rates.rateFor(country = "RU") } returns RU_TAX_RATE
    calculator.tax(price = 100, country = "RU")
}

// Разрешено — значение со входом: вход передаётся в блок и попадает в имя теста, здесь 100 -> 90
shouldReturnTest(expected = 90, input = 100) { price ->
    discount.priceFor(price)
}

// Разрешено — настройки теста: TestOptions задаёт имя и TestConfig из Kotest
shouldReturnTest(
    expected = 180,
    options = TestOptions(name = "цена со скидкой", config = TestConfig(timeout = 1.seconds)),
) {
    discount.priceFor(price = 200)
}

// Разрешено — набор случаев
withGivens(
    nameFn = { case -> "цена: ${case.arg2}" },
    case(arg1 = 270, arg2 = 300),
    case(arg1 = 360, arg2 = 400),
) { expected, price ->
    shouldReturnTest(expected) {
        discount.priceFor(price)
    }
}

// Разрешено — общая подготовка, разные функции
given(name = "скидка 50%") {
    val half = Discount(percent = 50)
    shouldReturnTest(expected = 50) {
        half.priceFor(price = 100)
    }
    shouldReturnTest(expected = "−50%") {
        half.label()
    }
}

// Разрешено — матчер; в наборе случаев имя даёт nameFn
withGivens(
    nameFn = { case -> "цена: ${case.arg2}, вес: ${case.arg3}" },
    case(arg1 = 2.5, arg2 = 5.0, arg3 = 2.0),
) { expected, price, kilos ->
    shouldReturnTest(expected plusOrMinus PRICE_TOLERANCE) {
        getPricePerKilo(price, kilos)
    }
}

// Запрещено — given, when, then для функции без побочного эффекта
given(name = "скидка 10%") {
    `when` {
        val price = discount.priceFor(price = 100)
        then {
            price shouldBe 90
        }
    }
}
```

### KT-KIT-9. Функция с исключением — `shouldThrow*Test` в корне спеки
Почему: исключение проверяется одним вызовом без `when` и `then`, как результат в KT-KIT-8.
Проверка: ревью
Контейнер — как в KT-KIT-8. Форма — по умолчанию самая строгая:
- `shouldThrowExactlyTest` — по умолчанию: точный тип исключения;
- `shouldThrowWithMessageTest` — сообщение — часть поведения: его видит пользователь или по нему разбирают ошибку;
- `shouldThrowTest` — подходит и наследник: точный тип не важен или зависит от платформы.

```kotlin
// Разрешено
shouldThrowExactlyTest<IllegalArgumentException> {
    Discount(percent = 150)
}

// Разрешено — сообщение — часть поведения
shouldThrowWithMessageTest<IllegalArgumentException>(message = "Скидка должна быть от 0 до 100: 150") {
    Discount(percent = 150)
}

// Разрешено — точный тип зависит от платформы
shouldThrowTest<IOException> {
    reader.read(makeMissingPath())
}

// Запрещено — try/catch
given(name = "скидка 150%") {
    `when` {
        val error = try {
            Discount(percent = 150)
            null
        } catch (exception: IllegalArgumentException) {
            exception
        }
        then {
            error shouldNotBe null
        }
    }
}

// Запрещено — shouldThrow из Kotest внутри then
then {
    shouldThrow<IllegalArgumentException> {
        Discount(percent = 150)
    }
}
```

### KT-KIT-10. Соответствие DTO формату сервера — `shouldDecodeTest` и `shouldEncodeTest` внутри `withJson`
Почему: расхождение DTO с форматом сервера ловится до запуска приложения: образец ответа → ожидаемый DTO и обратно.
Проверка: ревью
- Образцы — ресурсы `.json` в `src/test/resources`, путь задаётся от этого корня.
- `shouldDecodeTest` разбирает образец и сравнивает с DTO через `shouldBe`: при несовпадении Kotest называет поле.
- `shouldEncodeTest` кодирует DTO и сравнивает с образцом через `shouldEqualJson`: порядок ключей и пробелы не важны, отчёт называет путь поля.
- Имя теста — путь образца и тип, например `dto/user/full.json -> UserDto`. Своё имя и `TestConfig` задаются через `options`, как у `shouldReturnTest`.

```kotlin
@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val nickname: String? = null,
)

// Разрешено
class UserDtoSpec : BehaviorSpec({
    withJson {
        shouldDecodeTest(
            resource = "dto/user/full.json",
            expected = UserDto(id = 1, name = "Аня", nickname = "anya"),
        )
        shouldDecodeTest(
            resource = "dto/user/without_nickname.json",
            expected = UserDto(id = 1, name = "Аня"),
        )
        shouldEncodeTest(
            value = UserDto(id = 1, name = "Аня", nickname = "anya"),
            resource = "dto/user/full.json",
        )
    }
})

// Запрещено — разбор и сравнение вручную
given(name = "ответ сервера") {
    `when` {
        val user = Json.decodeFromString<UserDto>(makeUserJson())
        then {
            user shouldBe makeUser()
        }
    }
}
```

### KT-KIT-11. `Json` приложения — с флагами `DefaultTestJson`; другие флаги или `SerializersModule` — свой экземпляр в `withJson`
Почему: старый DTO читает ответ нового сервера; тест с `Json`, которым приложение не работает, проверяет не тот формат.
Проверка: ревью
По умолчанию `withJson` берёт `DefaultTestJson`. Его флаги — требования к `Json` приложения:

| Флаг | Зачем |
|------|-------|
| `ignoreUnknownKeys = true` | Сервер добавил поле — старый DTO читается |
| `explicitNulls = false` | Сервер не прислал необязательное поле — `null` |
| `coerceInputValues = true` | Новое значение enum или `null` — значение поля по умолчанию |
| `encodeDefaults = true` | В запрос идут все поля, без надежды на значения по умолчанию сервера |
| `prettyPrint = true` | Читаемый JSON в отчёте о падении, на сравнение не влияет |

У `Json` приложения другие флаги или нужен `SerializersModule` (для `@Contextual` и полиморфизма) — в `withJson` передаётся свой экземпляр. `Json(DefaultTestJson) { … }` сохраняет флаги по умолчанию.

```kotlin
// Разрешено — у приложения свой serializersModule
withJson(json = Json(DefaultTestJson) { serializersModule = appModule }) {
    shouldDecodeTest(
        resource = "dto/payment/card.json",
        expected = PaymentDto(id = 7, amount = "199.90".toBigDecimal()),
    )
    shouldEncodeTest(
        value = PaymentDto(id = 7, amount = "199.90".toBigDecimal()),
        resource = "dto/payment/card.json",
    )
}

// Запрещено — приложение работает с appModule, тест — с DefaultTestJson
withJson {
    shouldDecodeTest(
        resource = "dto/payment/card.json",
        expected = PaymentDto(id = 7, amount = "199.90".toBigDecimal()),
    )
}
```

### KT-KIT-12. Ожидаемого значения нет — `shouldRoundTripTest<T>`; ожидаемое есть — `shouldDecodeTest` и `shouldEncodeTest`
Почему: `shouldRoundTripTest` проверяет, что разбор и запись согласованы и ничего не теряют, но не значение: симметричную ошибку сериализатора (делит при разборе и умножает при записи на 1000 вместо 100) он не ловит — ловит только ожидаемое значение.
Проверка: ревью
- Типичный случай — образцов много, а ожидаемых DTO писать некому: модели генерируются, тесты пишет генератор. Эталоном служит сам образец.
- В дополнение к `shouldDecodeTest` `shouldRoundTripTest` разрешён — например, на много образцов одного DTO.
- `shouldRoundTripTest<T>(resource)` разбирает образец `Json` группы, записывает результат полностью (копией этого `Json` с `encodeDefaults = true` и `explicitNulls = true`) и проверяет, что образец — подмножество записи. Значения по умолчанию и явные `null` в образце не дают ложного падения, лишние ключи записи допускаются, `1` и `1.0` равны. Разбор потерял поле — отчёт называет путь: `At 'vertical.gap' expected 's' but was 'zero'`. Имя теста — путь образца и тип, например `dto/user/full.json <-> UserDto`.
- Полиморфный корень берёт сериализатор из `serializersModule` группы: `shouldRoundTripTest<Component>(…)`. `serializer<T>()` не даёт нужного правила (`@file:UseSerializers`, `@Contextual` у корня) — сериализатор передаётся явно: `shouldRoundTripTest(resource = "samples/event/click.json", serializer = EventSerializer)`; имя теста тогда заканчивается `serialName`.
- Граница проверки: значение в образце равно значению по умолчанию — потерю этого поля при разборе тест не заметит. Её ловят образцы со значением не по умолчанию.

```kotlin
// Разрешено — сгенерированные DTO, ожидаемых значений нет
class UserSamplesSpec : BehaviorSpec({
    withJson {
        shouldRoundTripTest<UserDto>(resource = "dto/user/full.json")
        shouldRoundTripTest<UserDto>(resource = "dto/user/without_nickname.json")
    }
})

// Запрещено — DTO со своим сериализатором проверен только round trip
@Serializable
data class PriceDto(
    @Serializable(with = KopecksSerializer::class)
    val price: BigDecimal,
)

withJson {
    shouldRoundTripTest<PriceDto>(resource = "dto/price/full.json")
}
```

### KT-KIT-13. Заглушки мока верхнего уровня спеки — в блоке при создании мока
Почему: мок и его поведение читаются в одном месте; отдельные `every` ниже объявления разрывают мок — читатель собирает его по кускам среди других объявлений.
Проверка: detekt `StubsInMockBlock` (своё правило из engineering-standards)
- Верхний уровень спеки — прямые выражения лямбды `BehaviorSpec({ … })`. Заглушки там — `every`, `coEvery`, `justRun`, `coJustRun` — только в блоке `mockk<T> { … }`.
- Заглушка на один случай — отдельной строкой внутри `given`, `when` или `withGivens`; мок, объявленный внутри `given`, правило не затрагивает.
- Значение для заглушки объявляется выше мока.
- Мок из функции (`val repository = makeRepository()`) с заглушкой на верхнем уровне — нарушение: заглушки — в блоке при создании мока внутри функции или в `given`.

```kotlin
// Разрешено
class RepositoryServiceSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val root = makeRoot()
    val repository = mockk<Repository> {
        every { root() } returns root.path
        justRun { save(any()) }
    }
    val service = RepositoryService(repository = repository)

    given(name = "пустой репозиторий") {
        every { repository.files() } returns emptyList()
        // …
    }
})

// Запрещено — заглушки мока отдельными строками на верхнем уровне спеки
class RepositoryServiceSpec : BehaviorSpec({
    isolationMode = IsolationMode.InstancePerRoot

    val repository = mockk<Repository>()
    val service = RepositoryService(repository = repository)
    val root = makeRoot()
    every { repository.root() } returns root.path
    justRun { repository.save(any()) }
    // …
})
```
