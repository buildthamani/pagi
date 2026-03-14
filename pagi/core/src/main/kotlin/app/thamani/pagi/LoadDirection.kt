package app.thamani.pagi

/**
 * Why a load is being requested.
 *
 * Most [PageSource] implementations can ignore this — they just fetch by key.
 * It's available for sources that need to behave differently on refresh
 * (e.g., clearing a cache) vs appending more data.
 */
sealed interface LoadDirection

/** Full refresh or initial load. */
data object Refresh : LoadDirection

/** Loading more items before the current first item. */
data object Prepend : LoadDirection

/** Loading more items after the current last item. */
data object Append : LoadDirection
