package app.thamani.pagi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates paged data loading from a [PageSource].
 *
 * The Pager does not create its own [CoroutineScope] — every method that
 * launches work takes a `scope` parameter. The caller owns the lifecycle.
 *
 * Observe [state] to render the paged list. Call [refresh] to start loading,
 * [onItemAccessed] to trigger prefetching, and [retry] to retry failed loads.
 *
 * @param config     paging configuration (page size, prefetch distance, max size)
 * @param initialKey the key for the first page, or null to let the source decide
 * @param source     the data source to load pages from
 */
class Pager<Key : Any, Value : Any>(
    private val config: PagerConfig,
    private val initialKey: Key? = null,
    private val source: PageSource<Key, Value>,
) {
    private val _state = MutableStateFlow(PagingState<Key, Value>())

    /** The current paging state. Observe this to render the paged list. */
    val state: StateFlow<PagingState<Key, Value>> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private val appendMutex = Mutex()
    private val prependMutex = Mutex()

    /**
     * Start a fresh load from [initialKey], clearing all existing data.
     *
     * Returns the [Job] so the caller can `join()` or cancel if needed.
     */
    fun refresh(scope: CoroutineScope): Job =
        scope.launch {
            refreshMutex.withLock {
                _state.update {
                    it.copy(pageStates = it.pageStates.copy(refresh = PageState.Loading))
                }

                val params =
                    LoadParams(
                        key = initialKey,
                        loadSize = config.pageSize,
                        direction = Refresh,
                    )

                when (val result = loadSafely(params)) {
                    is PageSourceResult.Success -> {
                        _state.update {
                            PagingState(
                                items = result.items,
                                pageStates =
                                    PageStates(
                                        refresh = PageState.Idle,
                                        prepend = if (result.prevKey == null) PageState.Complete else PageState.Idle,
                                        append = if (result.nextKey == null) PageState.Complete else PageState.Idle,
                                    ),
                                pages = listOf(result),
                            )
                        }
                    }

                    is PageSourceResult.Error -> {
                        _state.update {
                            it.copy(
                                pageStates =
                                    it.pageStates.copy(
                                        refresh = PageState.Error(result.error),
                                    ),
                            )
                        }
                    }
                }
            }
        }

    /**
     * Notify the pager that the item at [index] is visible.
     *
     * The pager checks whether this index is close enough to the edges
     * (within [PagerConfig.prefetchDistance]) to trigger an append or prepend.
     */
    fun onItemAccessed(
        index: Int,
        scope: CoroutineScope,
    ) {
        val current = _state.value
        val itemCount = current.items.size

        // Append: near the end
        if (index >= itemCount - config.prefetchDistance &&
            current.pageStates.append is PageState.Idle
        ) {
            loadAppend(scope)
        }

        // Prepend: near the start
        if (index <= config.prefetchDistance &&
            current.pageStates.prepend is PageState.Idle
        ) {
            loadPrepend(scope)
        }
    }

    /**
     * Retry all directions that are currently in an error state.
     */
    fun retry(scope: CoroutineScope) {
        val ps = _state.value.pageStates
        if (ps.refresh is PageState.Error) refresh(scope)
        if (ps.append is PageState.Error) loadAppend(scope)
        if (ps.prepend is PageState.Error) loadPrepend(scope)
    }

    // --- Internal load functions ---

    private fun loadAppend(scope: CoroutineScope): Job =
        scope.launch {
            appendMutex.withLock {
                // Re-check after acquiring the mutex — state may have changed
                val current = _state.value
                if (current.pageStates.append != PageState.Idle) return@withLock

                val lastPage = current.pages.lastOrNull() ?: return@withLock
                val nextKey = lastPage.nextKey ?: return@withLock

                _state.update {
                    it.copy(pageStates = it.pageStates.copy(append = PageState.Loading))
                }

                val params =
                    LoadParams(
                        key = nextKey,
                        loadSize = config.pageSize,
                        direction = Append,
                    )

                when (val result = loadSafely(params)) {
                    is PageSourceResult.Success -> {
                        _state.update { old ->
                            val newPages = old.pages + result
                            val (trimmedPages, trimmedItems) = applyMaxSize(newPages, TrimSide.FRONT)

                            old.copy(
                                items = trimmedItems,
                                pages = trimmedPages,
                                pageStates =
                                    old.pageStates.copy(
                                        append = if (result.nextKey == null) PageState.Complete else PageState.Idle,
                                        // If we trimmed from the front, prepend can load again
                                        prepend = if (trimmedPages.size < newPages.size) PageState.Idle else old.pageStates.prepend,
                                    ),
                            )
                        }
                    }

                    is PageSourceResult.Error -> {
                        _state.update {
                            it.copy(
                                pageStates =
                                    it.pageStates.copy(
                                        append = PageState.Error(result.error),
                                    ),
                            )
                        }
                    }
                }
            }
        }

    private fun loadPrepend(scope: CoroutineScope): Job =
        scope.launch {
            prependMutex.withLock {
                val current = _state.value
                if (current.pageStates.prepend != PageState.Idle) return@withLock

                val firstPage = current.pages.firstOrNull() ?: return@withLock
                val prevKey = firstPage.prevKey ?: return@withLock

                _state.update {
                    it.copy(pageStates = it.pageStates.copy(prepend = PageState.Loading))
                }

                val params =
                    LoadParams(
                        key = prevKey,
                        loadSize = config.pageSize,
                        direction = Prepend,
                    )

                when (val result = loadSafely(params)) {
                    is PageSourceResult.Success -> {
                        _state.update { old ->
                            val newPages = listOf(result) + old.pages
                            val (trimmedPages, trimmedItems) = applyMaxSize(newPages, TrimSide.BACK)

                            old.copy(
                                items = trimmedItems,
                                pages = trimmedPages,
                                pageStates =
                                    old.pageStates.copy(
                                        prepend = if (result.prevKey == null) PageState.Complete else PageState.Idle,
                                        // If we trimmed from the back, append can load again
                                        append = if (trimmedPages.size < newPages.size) PageState.Idle else old.pageStates.append,
                                    ),
                            )
                        }
                    }

                    is PageSourceResult.Error -> {
                        _state.update {
                            it.copy(
                                pageStates =
                                    it.pageStates.copy(
                                        prepend = PageState.Error(result.error),
                                    ),
                            )
                        }
                    }
                }
            }
        }

    // --- Helpers ---

    /**
     * Calls [source.load] safely:
     * - [CancellationException] is rethrown (never swallowed)
     * - Any other exception is wrapped in [PagingError.Source]
     */
    private suspend fun loadSafely(params: LoadParams<Key>): PageSourceResult<Key, Value> =
        try {
            source.load(params)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PageSourceResult.Error(
                PagingError.Source(
                    message = "Failed to load (${params.direction}): ${e.message}",
                    cause = e,
                ),
            )
        }

    private enum class TrimSide { FRONT, BACK }

    /**
     * Drops pages from [trimSide] until total item count is within [PagerConfig.maxSize].
     * Always keeps at least one page.
     */
    private fun applyMaxSize(
        pages: List<PageSourceResult.Success<Key, Value>>,
        trimSide: TrimSide,
    ): Pair<List<PageSourceResult.Success<Key, Value>>, List<Value>> {
        val max = config.maxSize ?: return pages to flattenItems(pages)

        val mutablePages = pages.toMutableList()
        var totalSize = mutablePages.sumOf { it.items.size }

        while (totalSize > max && mutablePages.size > 1) {
            val removed =
                when (trimSide) {
                    TrimSide.FRONT -> mutablePages.removeFirst()
                    TrimSide.BACK -> mutablePages.removeLast()
                }
            totalSize -= removed.items.size
        }

        return mutablePages to flattenItems(mutablePages)
    }

    /** Flattens pages into a single items list with one allocation. */
    private fun flattenItems(pages: List<PageSourceResult.Success<Key, Value>>): List<Value> =
        buildList {
            pages.forEach { addAll(it.items) }
        }
}
