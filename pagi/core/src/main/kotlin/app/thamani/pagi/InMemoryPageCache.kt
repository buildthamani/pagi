package app.thamani.pagi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A thread-safe in-memory [PageCache] backed by a [HashMap].
 *
 * Useful for simple caching where persistence isn't needed. Pages are
 * held in memory and cleared on [clear] or when the process dies.
 *
 * ```kotlin
 * val cache = InMemoryPageCache<Int, Product>()
 * val source = CachedPageSource(
 *     network = productApiSource,
 *     cache = cache,
 * )
 * ```
 */
class InMemoryPageCache<Key : Any, Value : Any> : PageCache<Key, Value> {

    private val store = HashMap<Key?, PageSourceResult.Success<Key, Value>>()
    private val mutex = Mutex()

    override suspend fun get(key: Key?): PageSourceResult.Success<Key, Value>? =
        mutex.withLock { store[key] }

    override suspend fun put(key: Key?, page: PageSourceResult.Success<Key, Value>) {
        mutex.withLock { store[key] = page }
    }

    override suspend fun clear() {
        mutex.withLock { store.clear() }
    }

    /** The number of pages currently cached. Useful for testing. */
    suspend fun size(): Int = mutex.withLock { store.size }
}
