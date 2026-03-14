package app.thamani.pagi

/**
 * Aggregates [PageState] for all three paging directions.
 */
data class PageStates(
    val refresh: PageState = PageState.Idle,
    val prepend: PageState = PageState.Idle,
    val append: PageState = PageState.Idle,
)
