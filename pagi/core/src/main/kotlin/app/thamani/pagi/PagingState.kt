package app.thamani.pagi

/**
 * The complete, immutable snapshot of paging state.
 *
 * This is what consumers observe via `StateFlow`. It contains everything
 * needed to render a paged list: the items, the load status per direction,
 * and the raw pages for internal bookkeeping.
 *
 * @param Key   the pagination token type
 * @param Value the item type
 * @property items      all loaded items in display order
 * @property pageStates load status per direction (refresh, prepend, append)
 * @property pages      the raw loaded pages — used by the Pager for navigation keys
 */
data class PagingState<Key : Any, Value : Any>(
    val items: List<Value> = emptyList(),
    val pageStates: PageStates = PageStates(),
    val pages: List<PageSourceResult.Success<Key, Value>> = emptyList(),
) {

    /** True when there are no items and a refresh is in progress. */
    val isInitialLoading: Boolean
        get() = items.isEmpty() && pageStates.refresh is PageState.Loading

    /** True when there are no items and the refresh failed. */
    val isInitialError: Boolean
        get() = items.isEmpty() && pageStates.refresh is PageState.Error

    /** True when there is at least one item. */
    val hasItems: Boolean
        get() = items.isNotEmpty()

    /** True when there are no items and no refresh in progress. */
    val isEmpty: Boolean
        get() = items.isEmpty() && pageStates.refresh !is PageState.Loading
}
