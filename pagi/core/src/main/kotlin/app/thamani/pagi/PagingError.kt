package app.thamani.pagi

/**
 * Represents an error that occurred during paging.
 *
 * Currently has a single variant — [Source] — for errors that happen
 * when fetching data from a [PageSource]. The library doesn't classify
 * errors further (network vs parse vs etc.); that's the consumer's concern.
 *
 * Kept as a sealed interface so new variants can be added in the future
 * without breaking existing code — the compiler will flag unhandled branches.
 *
 * Usage:
 * ```
 * when (error) {
 *     is PagingError.Source -> showError(error.message)
 * }
 * ```
 */
sealed interface PagingError {

    /** Human-readable description of what went wrong. */
    val message: String

    /**
     * An error that occurred while fetching data from a [PageSource].
     *
     * @property message what went wrong, in human-readable form
     * @property cause the underlying exception, if any, for debugging/logging
     */
    data class Source(
        override val message: String,
        val cause: Throwable? = null,
    ) : PagingError
}
