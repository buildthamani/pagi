package app.thamani.pagi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import app.thamani.pagi.Pager
import app.thamani.pagi.PagingState
import kotlinx.coroutines.flow.StateFlow

/**
 * Collects the [Pager]'s state as a Compose [State].
 *
 * Use this in a composable to observe paging state changes:
 * ```
 * val pagingState by pager.collectAsPagingState()
 * ```
 */
@Composable
fun <Key : Any, Value : Any> Pager<Key, Value>.collectAsPagingState(): State<PagingState<Key, Value>> {
    return state.collectAsState()
}

/**
 * Collects a [StateFlow] of [PagingState] as a Compose [State].
 *
 * Useful when the ViewModel exposes the flow directly:
 * ```
 * val pagingState by viewModel.pagingState.collectAsPagingState()
 * ```
 */
@Composable
fun <Key : Any, Value : Any> StateFlow<PagingState<Key, Value>>.collectAsPagingState(): State<PagingState<Key, Value>> {
    return collectAsState()
}
