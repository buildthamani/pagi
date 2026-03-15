package app.thamani.pagi.compose

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.thamani.pagi.PageState
import app.thamani.pagi.Pager
import app.thamani.pagi.PagingError
import app.thamani.pagi.PagingState

/**
 * Adds paged items to a [LazyColumn][androidx.compose.foundation.lazy.LazyColumn]
 * or [LazyRow][androidx.compose.foundation.lazy.LazyRow].
 *
 * Automatically triggers [Pager.onItemAccessed] as items become visible,
 * and renders optional loading/error slots for prepend and append directions.
 *
 * ```kotlin
 * LazyColumn {
 *     pagiItems(
 *         pager = pager,
 *         pagingState = state,
 *         key = { it.id },
 *         appendLoadingContent = { LoadingFooter() },
 *         appendErrorContent = { error -> ErrorFooter(error) { pager.retry() } },
 *     ) { index, item ->
 *         ItemRow(item)
 *     }
 * }
 * ```
 *
 * @param pager the [Pager] instance to trigger prefetch on
 * @param pagingState the current [PagingState] snapshot (collect via [collectAsPagingState])
 * @param key optional stable key for each item, used for efficient diffing
 * @param contentType optional content type for each item, used for view recycling
 * @param prependLoadingContent composable shown when prepend is loading
 * @param prependErrorContent composable shown when prepend has an error
 * @param appendLoadingContent composable shown when append is loading
 * @param appendErrorContent composable shown when append has an error
 * @param itemContent composable for each item, receives the index and value
 */
fun <Key : Any, Value : Any> LazyListScope.pagiItems(
    pager: Pager<Key, Value>,
    pagingState: PagingState<Key, Value>,
    key: ((Value) -> Any)? = null,
    contentType: ((Value) -> Any?)? = null,
    prependLoadingContent: (@Composable LazyItemScope.() -> Unit)? = null,
    prependErrorContent: (@Composable LazyItemScope.(PagingError) -> Unit)? = null,
    appendLoadingContent: (@Composable LazyItemScope.() -> Unit)? = null,
    appendErrorContent: (@Composable LazyItemScope.(PagingError) -> Unit)? = null,
    itemContent: @Composable LazyItemScope.(index: Int, value: Value) -> Unit,
) {
    // Prepend loading slot
    if (prependLoadingContent != null && pagingState.pageStates.prepend is PageState.Loading) {
        item(key = "pagi_prepend_loading", contentType = "pagi_prepend_loading") {
            prependLoadingContent()
        }
    }

    // Prepend error slot
    if (prependErrorContent != null && pagingState.pageStates.prepend is PageState.Error) {
        val error = (pagingState.pageStates.prepend as PageState.Error).error
        item(key = "pagi_prepend_error", contentType = "pagi_prepend_error") {
            prependErrorContent(error)
        }
    }

    // Main items
    itemsIndexed(
        items = pagingState.items,
        key = if (key != null) { index, item -> key(item) } else null,
        contentType = if (contentType != null) { index, item -> contentType(item) } else { _, _ -> null },
    ) { index, item ->
        LaunchedEffect(index) {
            pager.onItemAccessed(index)
        }
        itemContent(index, item)
    }

    // Append loading slot
    if (appendLoadingContent != null && pagingState.pageStates.append is PageState.Loading) {
        item(key = "pagi_append_loading", contentType = "pagi_append_loading") {
            appendLoadingContent()
        }
    }

    // Append error slot
    if (appendErrorContent != null && pagingState.pageStates.append is PageState.Error) {
        val error = (pagingState.pageStates.append as PageState.Error).error
        item(key = "pagi_append_error", contentType = "pagi_append_error") {
            appendErrorContent(error)
        }
    }
}
