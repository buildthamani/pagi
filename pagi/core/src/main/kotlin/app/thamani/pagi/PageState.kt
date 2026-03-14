package app.thamani.pagi

/**
 * The state of a page being loaded in a given direction (refresh, prepend, or append).
 */
sealed interface PageState {

    /** No load in progress, no error. Ready to load if triggered. */
    data object Idle : PageState

    /** A load is currently in progress. */
    data object Loading : PageState

    /** No more data available in this direction. */
    data object Complete : PageState

    /** The load failed. Carries the error for display or retry. */
    data class Error(val error: PagingError) : PageState
}
