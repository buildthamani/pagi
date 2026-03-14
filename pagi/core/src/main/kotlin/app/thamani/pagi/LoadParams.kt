package app.thamani.pagi

/**
 * Describes what to load from a [PageSource].
 *
 * @param Key the pagination token type
 * @property key      the key for the page to load; null means "load the initial page"
 * @property loadSize the requested number of items
 * @property direction why this load is being requested
 */
data class LoadParams<out Key : Any>(
    val key: Key?,
    val loadSize: Int,
    val direction: LoadDirection,
)
