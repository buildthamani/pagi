# Pagi

A lightweight, coroutine-first pagination library for Kotlin.

[![](https://jitpack.io/v/AzizAfworker/Pagi.svg)](https://jitpack.io/#AzizAfworker/Pagi)

**3 concepts, zero boilerplate:** `PageSource` loads data. `Pager` orchestrates. `PagingState`
drives the UI.

---

## Installation

Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependencies:

```kotlin
dependencies {
    // Core — pure Kotlin, no Android dependency
    implementation("com.github.buildthamani.pagi:pagi-core:<version>")

    // Compose — LazyColumn/LazyRow integration (optional)
    implementation("com.github.buildthamani.pagi:pagi-compose:<version>")
}
```

---

## Quick Start

### 1. Define a PageSource

> A `PageSource` is a single function that fetches one page of data. Use a lambda for simple sources:

```kotlin
val source = PageSource<Int, User> { params ->
    val response = api.getUsers(page = params.key ?: 1, limit = params.loadSize)

    PageSourceResult.Success(
        items = response.users,
        prevKey = null,
        nextKey = response.nextPage,
    )
}
```

Or implement the interface for more complex sources:

```kotlin
class UserPageSource(private val api: UserApi) : PageSource<Int, User> {
    override suspend fun load(params: LoadParams<Int>): PageSourceResult<Int, User> {
        return try {
            val response = api.getUsers(page = params.key ?: 1, limit = params.loadSize)
            PageSourceResult.Success(
                items = response.users,
                prevKey = if ((params.key ?: 1) > 1) (params.key ?: 1) - 1 else null,
                nextKey = response.nextPage,
            )
        } catch (e: Exception) {
            PageSourceResult.Error(PagingError.Source(e.message ?: "Unknown error", e))
        }
    }
}
```

### 2. Create a Pager in the Repository

> Orchestrates paged data loading from a [PageSource].

```kotlin
class UserRepository(private val api: UserApi) {

    fun getUsers(scope: CoroutineScope): Pager<Int, User> {
        return Pager(
            config = PagerConfig(pageSize = 20),
            source = UserPageSource(api),
            scope = scope,
        )
    }
}
```

```kotlin
class UserViewModel(private val repository: UserRepository) : ViewModel() {

    val pager = repository.getUsers(viewModelScope)

    val pagingState: StateFlow<PagingState<Int, User>> = pager.state

    fun refresh() = pager.refresh()
    fun retry() = pager.retry()
    fun onItemAccessed(index: Int) = pager.onItemAccessed(index)
}
```

### 3. Using the Pager

```kotlin
@Composable
fun UserList(viewModel: UserViewModel = viewModel()) {
    val state by viewModel.pagingState.collectAsPagingState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    when {
        state.isInitialLoading -> CircularProgressIndicator()
        state.isInitialError -> ErrorScreen(onRetry = { viewModel.retry() })
        else -> {
            LazyColumn {
                pagiItems(
                    pager = viewModel.pager,
                    pagingState = state,
                    key = { it.id },
                    appendLoadingContent = { CircularProgressIndicator() },
                    appendErrorContent = { error ->
                        TextButton(onClick = { viewModel.retry() }) { Text("Retry") }
                    },
                ) { _, user ->
                    UserRow(user)
                }
            }
        }
    }
}
```

---

## Core Concepts

### PageSource

A `fun interface` with a single `suspend fun load()`. Returns `PageSourceResult.Success` with items
and navigation keys, or `PageSourceResult.Error`.

The `Key` type is generic — use `Int` for offset-based pagination, `String` for cursors, or any type
that fits your API.

### Pager

The orchestrator. It manages load state, prevents duplicate requests with per-direction mutexes,
handles prefetching, and trims pages when `maxSize` is exceeded.

```kotlin
val pager = Pager(
    config = PagerConfig(
        pageSize = 20,
        prefetchDistance = 20,  // how close to the edge before loading more
        maxSize = 200,          // optional: drop old pages to save memory
    ),
    source = mySource,
    scope = viewModelScope,     // defaults to SupervisorJob() for failure isolation
)

pager.refresh()                 // start loading
pager.onItemAccessed(index)     // trigger prefetch when user scrolls
pager.retry()                   // retry all failed directions
```

### PagingState

An immutable snapshot of everything the UI needs:

```kotlin
val state: PagingState<Key, Value> = pager.state.value

state.items              // List<Value> — the flattened items across all pages
state.pageStates         // PageStates — refresh/prepend/append status
state.isInitialLoading   // true when empty + refresh is Loading
state.isInitialError     // true when empty + refresh is Error
state.hasItems           // true when items is not empty
state.isEmpty            // true when no items and not loading
```

### PageState

Per-direction load status — a sealed interface:

```kotlin
when (state.pageStates.append) {
    is PageState.Idle -> { /* waiting */ }
    is PageState.Loading -> { /* show spinner */ }
    is PageState.Complete -> { /* no more pages */ }
    is PageState.Error -> { /* show retry */ }
}
```

---

## Caching

Pagi includes a built-in caching layer with two strategies:

### CacheFirst (default)

Check cache first, fall back to network on miss. Best for data that doesn't change often.

### NetworkFirst

Always hit the network, fall back to cache on error. Best for data that changes frequently or
offline support.

```kotlin
val source = CachedPageSource(
    network = UserPageSource(api),
    cache = InMemoryPageCache(),        // or implement PageCache for Room, etc.
    strategy = NetworkFirst,            // or CacheFirst (default)
)

val pager = Pager(
    config = PagerConfig(pageSize = 20),
    source = source,
    scope = viewModelScope,
)
```

Both strategies clear the cache on refresh. Errors are never cached — a retry always hits the
network.

### Custom Cache

Implement `PageCache` for your own storage backend:

```kotlin
interface PageCache<Key : Any, Value : Any> {
    suspend fun get(key: Key?): PageSourceResult.Success<Key, Value>?
    suspend fun put(key: Key?, page: PageSourceResult.Success<Key, Value>)
    suspend fun clear()
}
```

---

## Compose Integration

Add `pagi-compose` for Jetpack Compose support.

### collectAsPagingState

Collect the Pager's `StateFlow` as Compose state:

```kotlin
val state by pager.collectAsPagingState()
// or
val state by viewModel.pagingState.collectAsPagingState()
```

### pagiItems

Drop-in replacement for `items()` inside `LazyColumn` or `LazyRow`. Automatically calls
`onItemAccessed()` for prefetching and renders loading/error slots:

```kotlin
LazyColumn {
    pagiItems(
        pager = pager,
        pagingState = state,
        key = { it.id },
        contentType = { "user" },
        prependLoadingContent = { LoadingHeader() },
        prependErrorContent = { error -> ErrorHeader(error) },
        appendLoadingContent = { LoadingFooter() },
        appendErrorContent = { error -> ErrorFooter(error) { pager.retry() } },
    ) { index, item ->
        UserRow(item)
    }
}
```

---

## Testing

Pagi ships test utilities so you never need a mocking framework.

### FakePageSource

Configure pages, inject errors, and track load history:

```kotlin
val source = fakePageSource<Int, String> {
    page(key = null, items = listOf("a", "b"), nextKey = 2)
    page(key = 2, items = listOf("c", "d"), nextKey = null)
}

// Inject a one-shot error
source.nextError = PagingError.Source("network timeout")

// Check what was loaded
assertEquals(2, source.loadHistory.size)
assertEquals(Refresh, source.loadHistory[0].direction)
```

### PagingAssertions

Readable one-liner assertions on `PagingState`:

```kotlin
@Test
fun `refresh loads items from the first page`() = runTest {
        val source = fakePageSource<Int, String> {
            page(key = null, items = listOf("a", "b"), nextKey = 2)
        }
        val pager = Pager(
            config = PagerConfig(pageSize = 2),
            source = source,
            scope = this,
        )

        pager.refresh().join()

        pager.state.value.assertItems(listOf("a", "b"))
        pager.state.value.assertNoErrors()
        pager.state.value.assertItemCount(2)
    }
```

Available assertions:

- `assertItemCount`
- `assertItems`
- `assertNoErrors`
- `assertRefreshLoading`
- `assertRefreshError`
- `assertAppendError`
- `assertAppendComplete`
- `assertPrependComplete`
- `assertPrependError`

---

## API Reference

| Type                      | Module         | Description                                                |
|---------------------------|----------------|------------------------------------------------------------|
| `PageSource<Key, Value>`  | core           | Data source — single `load()` function                     |
| `Pager<Key, Value>`       | core           | Orchestrator — `refresh()`, `onItemAccessed()`, `retry()`  |
| `PagingState<Key, Value>` | core           | Immutable state snapshot — `items`, `pageStates`, helpers  |
| `PagerConfig`             | core           | Config — `pageSize`, `prefetchDistance`, `maxSize`         |
| `PageSourceResult`        | core           | `Success(items, prevKey, nextKey)` or `Error(PagingError)` |
| `PageState`               | core           | Per-direction: `Idle`, `Loading`, `Complete`, `Error`      |
| `PageStates`              | core           | Aggregate: `refresh`, `prepend`, `append`                  |
| `LoadParams<Key>`         | core           | Load request: `key`, `loadSize`, `direction`               |
| `LoadDirection`           | core           | `Refresh`, `Prepend`, `Append`                             |
| `PagingError`             | core           | `Source(message, cause?)`                                  |
| `CachedPageSource`        | core           | Network + cache coordination                               |
| `FetchStrategy`           | core           | `CacheFirst` or `NetworkFirst`                             |
| `PageCache<Key, Value>`   | core           | Cache interface: `get()`, `put()`, `clear()`               |
| `InMemoryPageCache`       | core           | In-memory `PageCache` implementation                       |
| `FakePageSource`          | core (testing) | Configurable fake for tests                                |
| `PagingAssertions`        | core (testing) | Extension functions for test assertions                    |
| `collectAsPagingState()`  | compose        | Collect `StateFlow<PagingState>` as Compose `State`        |
| `pagiItems()`             | compose        | `LazyListScope` DSL with auto-prefetch                     |

---

## License

```
Copyright 2025 Thamani

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
