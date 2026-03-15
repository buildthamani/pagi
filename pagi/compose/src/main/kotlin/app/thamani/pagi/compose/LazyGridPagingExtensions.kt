package app.thamani.pagi.compose

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thamani.pagi.PageState
import app.thamani.pagi.Pager
import app.thamani.pagi.PagingError
import app.thamani.pagi.PagingState

/**
 * Adds paged items to a [LazyVerticalGrid][androidx.compose.foundation.lazy.grid.LazyVerticalGrid]
 * or [LazyHorizontalGrid][androidx.compose.foundation.lazy.grid.LazyHorizontalGrid].
 *
 * Grid-aware counterpart to [LazyListScope.pagiItems][pagiItems].
 * Loading/error slots span the full grid width by default.
 *
 * Automatically triggers [Pager.onItemAccessed] as items become visible.
 *
 * ```kotlin
 * LazyVerticalGrid(columns = GridCells.Fixed(2)) {
 *     pagiItems(
 *         pager = pager,
 *         pagingState = state,
 *         key = { it.id },
 *         appendLoadingContent = { LoadingFooter() },
 *         appendErrorContent = { error -> ErrorFooter(error) { pager.retry() } },
 *     ) { index, item ->
 *         ItemCard(item)
 *     }
 * }
 * ```
 *
 * @param pager the [Pager] instance to trigger prefetch on
 * @param pagingState the current [PagingState] snapshot
 * @param key optional stable key for each item
 * @param contentType optional content type for each item
 * @param spanFullWidth whether loading/error slots span the full grid width. Default true.
 * @param prependLoadingContent composable shown when prepend is loading
 * @param prependErrorContent composable shown when prepend has an error
 * @param appendLoadingContent composable shown when append is loading
 * @param appendErrorContent composable shown when append has an error
 * @param itemContent composable for each item, receives the index and value
 */
fun <Key : Any, Value : Any> LazyGridScope.pagiItems(
    pager: Pager<Key, Value>,
    pagingState: PagingState<Key, Value>,
    key: ((Value) -> Any)? = null,
    contentType: ((Value) -> Any?)? = null,
    spanFullWidth: Boolean = true,
    prependLoadingContent: (@Composable LazyGridItemScope.() -> Unit)? = null,
    prependErrorContent: (@Composable LazyGridItemScope.(PagingError) -> Unit)? = null,
    appendLoadingContent: (@Composable LazyGridItemScope.() -> Unit)? = null,
    appendErrorContent: (@Composable LazyGridItemScope.(PagingError) -> Unit)? = null,
    itemContent: @Composable LazyGridItemScope.(index: Int, value: Value) -> Unit,
) {
    val fullSpan: (androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() -> GridItemSpan)? =
        if (spanFullWidth) {
            { GridItemSpan(maxLineSpan) }
        } else {
            null
        }

    // Prepend loading slot
    if (prependLoadingContent != null && pagingState.pageStates.prepend is PageState.Loading) {
        item(
            key = "pagi_prepend_loading",
            contentType = "pagi_prepend_loading",
            span = fullSpan,
        ) {
            prependLoadingContent()
        }
    }

    // Prepend error slot
    if (prependErrorContent != null && pagingState.pageStates.prepend is PageState.Error) {
        val error = (pagingState.pageStates.prepend as PageState.Error).error
        item(
            key = "pagi_prepend_error",
            contentType = "pagi_prepend_error",
            span = fullSpan,
        ) {
            prependErrorContent(error)
        }
    }

    // Main items
    items(
        count = pagingState.items.size,
        key = if (key != null) { index -> key(pagingState.items[index]) } else null,
        contentType = if (contentType != null) {
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

    // Append loading slot
    if (appendLoadingContent != null && pagingState.pageStates.append is PageState.Loading) {
        item(
            key = "pagi_append_loading",
            contentType = "pagi_append_loading",
            span = fullSpan,
        ) {
            appendLoadingContent()
        }
    }

    // Append error slot
    if (appendErrorContent != null && pagingState.pageStates.append is PageState.Error) {
        val error = (pagingState.pageStates.append as PageState.Error).error
        item(
            key = "pagi_append_error",
            contentType = "pagi_append_error",
            span = fullSpan,
        ) {
            appendErrorContent(error)
        }
    }
}
