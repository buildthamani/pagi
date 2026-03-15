package app.thamani.pagi.compose

import androidx.compose.foundation.lazy.grid.GridCells

/**
 * Defines how [PagiContent] lays out its items.
 *
 * Use [List] for a vertical scrolling list ([LazyColumn][androidx.compose.foundation.lazy.LazyColumn])
 * or [Grid] for a vertical scrolling grid ([LazyVerticalGrid][androidx.compose.foundation.lazy.grid.LazyVerticalGrid]).
 */
sealed interface PagiLayout {

    /**
     * Vertical scrolling list (LazyColumn).
     */
    data object List : PagiLayout

    /**
     * Vertical scrolling grid (LazyVerticalGrid).
     *
     * Loading/error slots automatically span the full grid width.
     *
     * @param columns the grid column configuration (e.g. `GridCells.Fixed(2)` or `GridCells.Adaptive(120.dp)`)
     */
    data class Grid(val columns: GridCells) : PagiLayout
}
