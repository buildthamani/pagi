package app.thamani.pagi

/**
 * The result of a [PageSource.load] call — either data or an error.
 *
 * This type is only produced by [PageSource] and consumed by the [Pager].
 *
 * @param Key  the pagination token type (Int for offset, String for cursor, etc.)
 * @param Value the item type
 */
sealed interface PageSourceResult<out Key : Any, out Value : Any> {

    /**
     * The load succeeded.
     *
     * @property items   the loaded items, in display order
     * @property prevKey key to load the previous page, or null if this is the first page
     * @property nextKey key to load the next page, or null if this is the last page
     */
    data class Success<Key : Any, Value : Any>(
        val items: List<Value>,
        val prevKey: Key?,
        val nextKey: Key?,
    ) : PageSourceResult<Key, Value>

    /**
     * The load failed.
     *
     * Uses [Nothing] for Key/Value so it can be assigned to any
     * `PageSourceResult<K, V>` — errors don't carry items.
     *
     * @property error what went wrong
     */
    data class Error(
        val error: PagingError,
    ) : PageSourceResult<Nothing, Nothing>
}
