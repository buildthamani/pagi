package app.thamani.pagi.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import app.thamani.pagi.PageState
import app.thamani.pagi.Pager
import app.thamani.pagi.PagingError
import app.thamani.pagi.PagingState

/**
 * A full-screen paging composable that handles initial loading, initial error,
 * empty state, the scrollable list/grid, and inline prepend/append states.
 *
 * Auto-refreshes on first composition. Every slot is replaceable.
 *
 * **Minimal usage (3 lines):**
 * ```kotlin
 * PagiContent(pager = viewModel.pager) { _, item ->
 *     Text(item.name)
 * }
 * ```
 *
 * **Fully customized:**
 * ```kotlin
 * PagiContent(
 *     pager = viewModel.pager,
 *     modifier = Modifier.fillMaxSize(),
 *     layout = PagiLayout.Grid(GridCells.Fixed(2)),
 *     contentPadding = PaddingValues(16.dp),
 *     initialLoading = { MyShimmer() },
 *     initialError = { error, onRetry -> MyErrorScreen(error, onRetry) },
 *     emptyContent = { MyEmptyState() },
 *     prependLoading = { SmallSpinner() },
 *     prependError = { error, onRetry -> InlineRetryBanner(error, onRetry) },
 *     appendLoading = { SmallSpinner() },
 *     appendError = { error, onRetry -> InlineRetryBanner(error, onRetry) },
 *     key = { it.id },
 *     contentType = { "item" },
 * ) { index, item ->
 *     ItemCard(item)
 * }
 * ```
 *
 * @param pager the [Pager] instance to drive
 * @param modifier modifier for the root container
 * @param layout list or grid layout strategy. Defaults to [PagiLayout.List].
 * @param refreshOnComposition whether to call [Pager.refresh] on first composition. Default true.
 * @param contentPadding padding for the lazy list/grid
 * @param initialLoading full-screen composable shown during initial load
 * @param initialError full-screen composable shown on initial load failure. Receives the error and a retry callback.
 * @param emptyContent full-screen composable shown when refresh succeeds with zero items
 * @param prependLoading inline composable shown when prepend is loading
 * @param prependError inline composable shown when prepend fails. Receives the error and a retry callback.
 * @param appendLoading inline composable shown when append is loading
 * @param appendError inline composable shown when append fails. Receives the error and a retry callback.
 * @param key optional stable key for each item, used for efficient diffing
 * @param contentType optional content type for each item, used for view recycling
 * @param itemContent composable for each item, receives the index and value
 */
@Composable
fun <Key : Any, Value : Any> PagiContent(
    pager: Pager<Key, Value>,
    modifier: Modifier = Modifier,
    layout: PagiLayout = PagiLayout.List,
    refreshOnComposition: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(),
    initialLoading: @Composable () -> Unit = PagiDefaults.InitialLoading,
    initialError: @Composable (error: PagingError, onRetry: () -> Unit) -> Unit = PagiDefaults.InitialError,
    emptyContent: @Composable () -> Unit = PagiDefaults.EmptyContent,
    prependLoading: @Composable () -> Unit = PagiDefaults.InlineLoading,
    prependError: @Composable (error: PagingError, onRetry: () -> Unit) -> Unit = PagiDefaults.InlineError,
    appendLoading: @Composable () -> Unit = PagiDefaults.InlineLoading,
    appendError: @Composable (error: PagingError, onRetry: () -> Unit) -> Unit = PagiDefaults.InlineError,
    key: ((Value) -> Any)? = null,
    contentType: ((Value) -> Any?)? = null,
    itemContent: @Composable (index: Int, value: Value) -> Unit,
) {
    if (refreshOnComposition) {
        LaunchedEffect(pager) {
            pager.refresh()
        }
    }

    val pagingState by pager.collectAsPagingState()

    when {
        // Show loading during initial load, or while waiting for LaunchedEffect to fire
        pagingState.isInitialLoading ||
            (
                pagingState.items.isEmpty() &&
                    pagingState.pageStates.refresh is PageState.Idle &&
                    refreshOnComposition
            ) -> {
            initialLoading()
        }

        pagingState.isInitialError -> {
            val error = (pagingState.pageStates.refresh as PageState.Error).error
            initialError(error) { pager.retry() }
        }

        pagingState.isEmpty -> {
            emptyContent()
        }

        else -> {
            when (layout) {
                is PagiLayout.List -> {
                    PagiListContent(
                        pager = pager,
                        pagingState = pagingState,
                        modifier = modifier,
                        contentPadding = contentPadding,
                        prependLoading = prependLoading,
                        prependError = prependError,
                        appendLoading = appendLoading,
                        appendError = appendError,
                        key = key,
                        contentType = contentType,
                        itemContent = itemContent,
                    )
                }

                is PagiLayout.Grid -> {
                    PagiGridContent(
                        pager = pager,
                        pagingState = pagingState,
                        modifier = modifier,
                        columns = layout.columns,
                        contentPadding = contentPadding,
                        prependLoading = prependLoading,
                        prependError = prependError,
                        appendLoading = appendLoading,
                        appendError = appendError,
                        key = key,
                        contentType = contentType,
                        itemContent = itemContent,
                    )
                }
            }
        }
    }
}

// --- Internal layout composables ---

@Composable
private fun <Key : Any, Value : Any> PagiListContent(
    pager: Pager<Key, Value>,
    pagingState: PagingState<Key, Value>,
    modifier: Modifier,
    contentPadding: PaddingValues,
    prependLoading: @Composable () -> Unit,
    prependError: @Composable (PagingError, () -> Unit) -> Unit,
    appendLoading: @Composable () -> Unit,
    appendError: @Composable (PagingError, () -> Unit) -> Unit,
    key: ((Value) -> Any)?,
    contentType: ((Value) -> Any?)?,
    itemContent: @Composable (Int, Value) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        pagiItems(
            pager = pager,
            pagingState = pagingState,
            key = key,
            contentType = contentType,
            prependLoadingContent = { prependLoading() },
            prependErrorContent = { error -> prependError(error) { pager.retry() } },
            appendLoadingContent = { appendLoading() },
            appendErrorContent = { error -> appendError(error) { pager.retry() } },
        ) { index, item ->
            itemContent(index, item)
        }
    }
}

@Composable
private fun <Key : Any, Value : Any> PagiGridContent(
    pager: Pager<Key, Value>,
    pagingState: PagingState<Key, Value>,
    modifier: Modifier,
    columns: androidx.compose.foundation.lazy.grid.GridCells,
    contentPadding: PaddingValues,
    prependLoading: @Composable () -> Unit,
    prependError: @Composable (PagingError, () -> Unit) -> Unit,
    appendLoading: @Composable () -> Unit,
    appendError: @Composable (PagingError, () -> Unit) -> Unit,
    key: ((Value) -> Any)?,
    contentType: ((Value) -> Any?)?,
    itemContent: @Composable (Int, Value) -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        // Prepend loading
        if (pagingState.pageStates.prepend is PageState.Loading) {
            item(
                key = "pagi_prepend_loading",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                prependLoading()
            }
        }

        // Prepend error
        if (pagingState.pageStates.prepend is PageState.Error) {
            val error = (pagingState.pageStates.prepend as PageState.Error).error
            item(
                key = "pagi_prepend_error",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                prependError(error) { pager.retry() }
            }
        }

        // Main items
        items(
            count = pagingState.items.size,
            key = if (key != null) { index -> key(pagingState.items[index]) } else null,
            contentType =
                if (contentType != null) {
                    { index -> contentType(pagingState.items[index]) }
                } else {
                    { null }
                },
        ) { index ->
            LaunchedEffect(index) {
                pager.onItemAccessed(index)
            }
            itemContent(index, pagingState.items[index])
        }

        // Append loading
        if (pagingState.pageStates.append is PageState.Loading) {
            item(
                key = "pagi_append_loading",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                appendLoading()
            }
        }

        // Append error
        if (pagingState.pageStates.append is PageState.Error) {
            val error = (pagingState.pageStates.append as PageState.Error).error
            item(
                key = "pagi_append_error",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                appendError(error) { pager.retry() }
            }
        }
    }
}
