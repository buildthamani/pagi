package app.thamani.pagi.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.thamani.pagi.PagingError

/**
 * Default composable factories for [PagiContent] slots.
 *
 * These use only Compose Foundation primitives (no Material dependency).
 * They are intentionally minimal — meant for prototyping and quick starts.
 * Replace them with your own design-system components for production use.
 */
object PagiDefaults {

    /**
     * Default full-screen loading indicator.
     * Shows centered "Loading..." text.
     */
    val InitialLoading: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("Loading...")
        }
    }

    /**
     * Default full-screen error display.
     * Shows the error message centered with a tap-to-retry hint.
     */
    val InitialError: @Composable (error: PagingError, onRetry: () -> Unit) -> Unit =
        { error, onRetry ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center,
            ) {
                BasicText("Error: ${error.message}\nTap to retry")
            }
        }

    /**
     * Default full-screen empty state.
     * Shows centered "No items" text.
     */
    val EmptyContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("No items")
        }
    }

    /**
     * Default inline loading indicator for prepend/append directions.
     * Shows a full-width centered "Loading..." text.
     */
    val InlineLoading: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("Loading...")
        }
    }

    /**
     * Default inline error display for prepend/append directions.
     * Shows the error message with a tap-to-retry hint.
     */
    val InlineError: @Composable (error: PagingError, onRetry: () -> Unit) -> Unit =
        { error, onRetry ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onRetry() },
                contentAlignment = Alignment.Center,
            ) {
                BasicText("Error: ${error.message} — Tap to retry")
            }
        }
}
