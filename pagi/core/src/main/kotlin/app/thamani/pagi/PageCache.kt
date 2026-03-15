package app.thamani.pagi

/**
 * Cache storage for paged data.
 *
 * Implement this to provide your own storage backend (Room, SQLDelight,
 * DataStore, etc.). For simple cases, use [InMemoryPageCache].
 *
 * @param Key   the pagination token type
 * @param Value the item type
 */
interface PageCache<Key : Any, Value : Any> {

    /**
     * Retrieve a cached page for the given [key].
     *
     * Returns the cached [PageSourceResult.Success] if available, or null
     * if the page is not in the cache.
     *
     * @param key the page key, or null for the initial page
     */
    suspend fun get(key: Key?): PageSourceResult.Success<Key, Value>?

    /**
     * Store a successfully loaded page in the cache.
     *
     * @param key    the page key, or null for the initial page
     * @param page   the page data to cache
     */
    suspend fun put(key: Key?, page: PageSourceResult.Success<Key, Value>)

    /**
     * Remove all cached pages.
     *
     * Called on refresh so stale data doesn't persist.
     */
    suspend fun clear()
}
