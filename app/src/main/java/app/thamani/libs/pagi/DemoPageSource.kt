package app.thamani.libs.pagi

import app.thamani.pagi.LoadParams
import app.thamani.pagi.PageSource
import app.thamani.pagi.PageSourceResult
import kotlinx.coroutines.delay

/**
 * A fake page source that generates numbered items with an artificial delay
 * to simulate network latency. Produces 200 total items across pages.
 */
class DemoPageSource : PageSource<Int, DemoItem> {
    override suspend fun load(params: LoadParams<Int>): PageSourceResult<Int, DemoItem> {
        delay(10000) // simulate network

        val pageIndex = params.key ?: 0
        val start = pageIndex * params.loadSize
        val end = minOf(start + params.loadSize, TOTAL_ITEMS)

        if (start >= TOTAL_ITEMS) {
            return PageSourceResult.Success(
                items = emptyList(),
                prevKey = if (pageIndex > 0) pageIndex - 1 else null,
                nextKey = null,
            )
        }

        val items =
            (start until end).map { i ->
                DemoItem(id = i, title = "Item #$i", subtitle = "Page ${pageIndex + 1}")
            }

        return PageSourceResult.Success(
            items = items,
            prevKey = if (pageIndex > 0) pageIndex - 1 else null,
            nextKey = if (end < TOTAL_ITEMS) pageIndex + 1 else null,
        )
    }

    companion object {
        private const val TOTAL_ITEMS = 200
    }
}

data class DemoItem(
    val id: Int,
    val title: String,
    val subtitle: String,
)
