package app.thamani.pagi.testing

import app.thamani.pagi.LoadParams
import app.thamani.pagi.PageSource
import app.thamani.pagi.PageSourceResult
import app.thamani.pagi.PagingError

/**
 * A pre-built [PageSource] fake for testing paging consumers.
 *
 * Configure it with pages of data and it serves them by key.
 * No mocking framework needed.
 *
 * Features:
 * - [loadHistory] tracks every load call for assertion
 * - [nextError] injects a one-shot error into the next load
 *
 * @param pages map of key → page data. Use `null` key for the initial page.
 */
class FakePageSource<Key : Any, Value : Any>(
    private val pages: Map<Key?, FakePage<Key, Value>>,
) : PageSource<Key, Value> {

    private val _loadHistory = mutableListOf<LoadParams<Key>>()

    /** Every [LoadParams] received, in order. */
    val loadHistory: List<LoadParams<Key>> get() = _loadHistory.toList()

    /**
     * If non-null, the next [load] call returns this error, then clears it.
     * Useful for testing retry flows without complex state management.
     */
    var nextError: PagingError? = null

    override suspend fun load(params: LoadParams<Key>): PageSourceResult<Key, Value> {
        _loadHistory.add(params)

        nextError?.let { error ->
            nextError = null
            return PageSourceResult.Error(error)
        }

        val page = pages[params.key]
            ?: return PageSourceResult.Error(
                PagingError.Source("No fake page configured for key: ${params.key}"),
            )

        return PageSourceResult.Success(
            items = page.items,
            prevKey = page.prevKey,
            nextKey = page.nextKey,
        )
    }
}

/**
 * A page of fake data for use with [FakePageSource].
 */
data class FakePage<Key : Any, Value : Any>(
    val items: List<Value>,
    val prevKey: Key? = null,
    val nextKey: Key? = null,
)

/**
 * Builder for constructing a [FakePageSource] with a DSL.
 *
 * ```
 * val source = fakePageSource<Int, String> {
 *     page(key = null, items = listOf("a", "b"), nextKey = 2)
 *     page(key = 2, items = listOf("c", "d"), nextKey = null)
 * }
 * ```
 */
class FakePageSourceBuilder<Key : Any, Value : Any> {
    private val pages = mutableMapOf<Key?, FakePage<Key, Value>>()

    fun page(
        key: Key?,
        items: List<Value>,
        prevKey: Key? = null,
        nextKey: Key? = null,
    ) {
        pages[key] = FakePage(items, prevKey, nextKey)
    }

    fun build(): FakePageSource<Key, Value> = FakePageSource(pages)
}

/**
 * Creates a [FakePageSource] using a builder DSL.
 */
fun <Key : Any, Value : Any> fakePageSource(
    block: FakePageSourceBuilder<Key, Value>.() -> Unit,
): FakePageSource<Key, Value> = FakePageSourceBuilder<Key, Value>().apply(block).build()
