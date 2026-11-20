# Импорты, псевдонимы, комментарии, KDoc — KT-DOC

Формат правил и префиксы — [README.md](README.md).

### KT-DOC-1. Импорт со звёздочкой запрещён
Почему: по `import …*` не видно, откуда пришло имя, а новое имя в пакете может молча перекрыть другое.
Проверка: detekt `WildcardImport`
Источник: #16

IntelliJ сама сворачивает импорты в `*` — это выключено в `.editorconfig` (`ij_kotlin_name_count_to_use_star_import`).

```kotlin
// Разрешено
import java.io.File
import java.time.Instant

// Запрещено
import java.io.*
import java.time.*
```

### KT-DOC-2. KDoc — только у публичных классов и функций с нетривиальной логикой
Почему: KDoc нужен тому, кто использует класс снаружи, и там, где код сам не объясняет, что делает; остальное объясняют имена (KT-NAME-5).
Проверка: ревью
Источник: #16

Публичный — не `internal` и не `private`.

```kotlin
// Разрешено
/**
 * Разбирает ответ сервера в модель аккаунта.
 */
class AccountParser

// Запрещено
/**
 * Возвращает имя.
 */
private fun getName(): String {
    // …
}
```

### KT-DOC-3. Псевдоним импорта — только при совпадении имён
Почему: псевдоним прячет настоящее имя типа — поиск по имени класса не находит места использования.
Проверка: ревью
Источник: #16

```kotlin
// Разрешено — два разных Result в одном файле
import com.example.network.Result as NetworkResult
import com.example.domain.Result

// Запрещено
import com.example.domain.AccountModel as Account
```

### KT-DOC-4. `typealias` — для длинных функциональных и обобщённых типов
Почему: псевдоним класса — второе имя одной сущности, читатель ищет два класса; псевдоним длинного типа, наоборот, даёт ему имя.
Проверка: ревью
Источник: #16

`typealias` объявляется на верхнем уровне файла, там же, где приватные константы (KT-CLASS-1). Имя функционального типа — с суффиксом по KT-NAME-11.

```kotlin
// Разрешено — файл AccountScreen.kt
typealias AccountConsumer = (AccountModel) -> Unit

private const val ACCOUNT_ID = "id"

class AccountScreen(
    private val accountConsumer: AccountConsumer,
)

// Запрещено — переименование класса
typealias Account = AccountModel
```

### KT-DOC-5. Комментарий объясняет «почему», а не «что»
Почему: что делает код, говорит имя (KT-NAME-5); комментарий «что» устаревает при первой правке кода.
Проверка: ревью
Источник: #16

Закомментированный код не оставляется: его история — в git.

```kotlin
// Разрешено
// Сервер отдаёт пустой список вместо 404 — проверяем размер, а не код ответа.
if (accounts.isEmpty()) return

// Запрещено
// Проверяем, пустой ли список
if (accounts.isEmpty()) return

// val oldAccounts = loadOldAccounts()
```

### KT-DOC-6. Заглушки запрещены
Почему: заглушка выглядит как готовый код, и ошибка всплывает только при запуске.
Проверка: detekt `ForbiddenComment` (`TODO`, `FIXME`), `ForbiddenMethodCall` (`kotlin.TODO`), `EmptyFunctionBlock` (пустые функции)
Источник: #16

Заглушка — комментарий `TODO` или `FIXME`, функция `TODO()`, «not yet implemented» в тексте исключения, пустая функция.

Исключение — пустая реализация по умолчанию в интерфейсе и пустой `override`: реализация не нужна там, где делать нечего. В теле — комментарий, почему пусто (KT-DOC-5).

```kotlin
// Разрешено
interface ScreenListener {

    fun onShow() {
        // Большинству экранов реакция на показ не нужна.
    }
}

class AccountScreen : ScreenListener {

    override fun onShow() {
        // Аккаунт загружается при создании, на показ реагировать нечем.
    }
}

// Запрещено
fun loadAccount(): AccountModel {
    TODO()
}

fun saveAccount(account: AccountModel) {
    // TODO: сохранить
}

fun deleteAccount() {
    throw NotImplementedError("not yet implemented")
}

fun refresh() {}
```
