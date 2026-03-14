package app.thamani.pagi

/**
 * Configuration for a [Pager].
 *
 * @property pageSize         number of items to request per page
 * @property prefetchDistance  how many items from the edge before triggering the next load
 * @property maxSize           optional cap on total items in memory; null means unlimited
 */
data class PagerConfig(
    val pageSize: Int = 20,
    val prefetchDistance: Int = pageSize,
    val maxSize: Int? = null,
) {
    init {
        require(pageSize > 0) { "pageSize must be > 0, was $pageSize" }
        require(prefetchDistance >= 0) { "prefetchDistance must be >= 0, was $prefetchDistance" }
        maxSize?.let {
            require(it >= pageSize + 2 * prefetchDistance) {
                "maxSize ($it) must be >= pageSize + 2*prefetchDistance (${pageSize + 2 * prefetchDistance})"
            }
        }
    }
}
