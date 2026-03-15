package app.thamani.pagi

/**
 * A [PageSource] that coordinates between a network source and a [PageCache]
 * using the specified [FetchStrategy].
 *
 * - [CacheFirst] — check cache, fall back to network on miss
 * - [NetworkFirst] — hit network, fall back to cache on error
 *
 * **On refresh:** the cache is always cleared first so stale data doesn't persist,
 * regardless of strategy.
 *
 * **Errors are never cached.** A retry always hits the network.
 *
 * ```kotlin
 * // Cache-first (default) — minimizes network calls
 * val source = CachedPageSource(
 *     network = ProductApiSource(api),
 *     cache = InMemoryPageCache(),
 * )
 *
 * // Network-first — always fresh, offline fallback
 * val source = CachedPageSource(
 *     network = ProductApiSource(api),
 *     cache = InMemoryPageCache(),
 *     strategy = NetworkFirst,
 * )
 * ```
 *
 * @param Key      the pagination token type
 * @param Value    the item type
 * @param network  the underlying source that fetches data (API, database, etc.)
 * @param cache    the cache to store and retrieve pages from
 * @param strategy the fetch order — [CacheFirst] or [NetworkFirst]
 */
class CachedPageSource<Key : Any, Value : Any>(
    private val network: PageSource<Key, Value>,
    private val cache: PageCache<Key, Value>,
    private val strategy: FetchStrategy = CacheFirst,
) : PageSource<Key, Value> {

    override suspend fun load(params: LoadParams<Key>): PageSourceResult<Key, Value> {
        if (params.direction is Refresh) {
            cache.clear()
        }

        return when (strategy) {
            is CacheFirst -> loadCacheFirst(params)
            is NetworkFirst -> loadNetworkFirst(params)
        }
    }

    /**
     * Cache → Network.
     * Check cache first; on miss, fetch from network and cache the result.
     */
    private suspend fun loadCacheFirst(params: LoadParams<Key>): PageSourceResult<Key, Value> {
        val cached = cache.get(params.key)
        if (cached != null) return cached

        val result = network.load(params)

        if (result is PageSourceResult.Success) {
            cache.put(params.key, result)
        }

        return result
    }

    /**
     * Network → Cache.
     * Always hit network first; on error, fall back to cache. On success,
     * update the cache with fresh data.
     */
    private suspend fun loadNetworkFirst(params: LoadParams<Key>): PageSourceResult<Key, Value> {
        val result = network.load(params)

        if (result is PageSourceResult.Success) {
            cache.put(params.key, result)
            return result
        }

        // Network failed — try cache as fallback
        val cached = cache.get(params.key)
        if (cached != null) return cached

        // No cache fallback — return the original error
        return result
    }
}
