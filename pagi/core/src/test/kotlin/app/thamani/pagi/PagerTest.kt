package app.thamani.pagi

import app.thamani.pagi.testing.assertAppendComplete
import app.thamani.pagi.testing.assertAppendError
import app.thamani.pagi.testing.assertItemCount
import app.thamani.pagi.testing.assertItems
import app.thamani.pagi.testing.assertNoErrors
import app.thamani.pagi.testing.assertPrependComplete
import app.thamani.pagi.testing.assertRefreshError
import app.thamani.pagi.testing.fakePageSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PagerTest {
    // ==================== Refresh ====================

    @Test
    fun `first refresh populates items from the initial page`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a", "b", "c"), nextKey = 2)
                }

            val pager = Pager(config = PagerConfig(pageSize = 3), source = source, scope = this)
            pager.refresh().join()

            val state = pager.state.value
            state.assertItems(listOf("a", "b", "c"))
            state.assertNoErrors()
            state.assertPrependComplete()
            assertEquals(PageState.Idle, state.pageStates.append)
            assertEquals(1, state.pages.size)
        }

    @Test
    fun `when source returns no prev or next key both directions are marked complete`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("only"))
                }

            val pager = Pager(config = PagerConfig(pageSize = 1), source = source, scope = this)
            pager.refresh().join()

            pager.state.value.assertAppendComplete()
            pager.state.value.assertPrependComplete()
        }

    @Test
    fun `calling refresh again replaces old items with fresh data`() =
        runTest {
            var callCount = 0
            val source =
                PageSource<Int, String> { _ ->
                    callCount++
                    PageSourceResult.Success(listOf("batch-$callCount"), null, null)
                }

            val pager = Pager(config = PagerConfig(pageSize = 1), source = source, scope = this)

            pager.refresh().join()
            pager.state.value.assertItems(listOf("batch-1"))

            pager.refresh().join()
            pager.state.value.assertItems(listOf("batch-2"))
            assertEquals(1, pager.state.value.pages.size)
        }

    // ==================== Error handling ====================

    @Test
    fun `when the source returns an error the refresh state becomes Error`() =
        runTest {
            val source = fakePageSource<Int, String> {}

            val pager = Pager(config = PagerConfig(pageSize = 10), source = source, scope = this)
            pager.refresh().join()

            pager.state.value.assertRefreshError()
            pager.state.value.assertItemCount(0)
        }

    @Test
    fun `when the source throws an exception it is wrapped as a PagingError`() =
        runTest {
            val source =
                PageSource<Int, String> { _ ->
                    throw RuntimeException("boom")
                }

            val pager = Pager(config = PagerConfig(pageSize = 10), source = source, scope = this)
            pager.refresh().join()

            val error = pager.state.value.pageStates.refresh
            assertTrue(error is PageState.Error)
            assertTrue((error as PageState.Error).error.message.contains("boom"))
        }

    @Test
    fun `cancelling a refresh does not produce an error state`() =
        runTest {
            val source =
                PageSource<Int, String> { _ ->
                    delay(10_000)
                    PageSourceResult.Success(listOf("x"), null, null)
                }

            val pager = Pager(config = PagerConfig(pageSize = 1), source = source, scope = this)
            val job = pager.refresh()
            yield() // Let the coroutine start and set state to Loading before cancelling
            job.cancelAndJoin()

            val refreshState = pager.state.value.pageStates.refresh
            assertTrue(
                "Expected Loading after cancellation, got $refreshState",
                refreshState is PageState.Loading,
            )
        }

    // ==================== Append ====================

    @Test
    fun `scrolling near the end of the list loads the next page`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a", "b"), nextKey = 2)
                    page(key = 2, items = listOf("c", "d"), prevKey = 1)
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 2, prefetchDistance = 1),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.onItemAccessed(1)
            advanceUntilIdle()

            pager.state.value.assertItems(listOf("a", "b", "c", "d"))
            pager.state.value.assertAppendComplete()
        }

    @Test
    fun `when appending fails the append state becomes Error`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a"), nextKey = 2)
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 1, prefetchDistance = 1),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.onItemAccessed(0)
            advanceUntilIdle()

            pager.state.value.assertAppendError()
        }

    @Test
    fun `scrolling through the list loads pages one after another`() =
        runTest {
            val source =
                fakePageSource<String, String> {
                    page(key = null, items = listOf("1"), nextKey = "b")
                    page(key = "b", items = listOf("2"), prevKey = "a", nextKey = "c")
                    page(key = "c", items = listOf("3"), prevKey = "b")
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 1, prefetchDistance = 1),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.state.value.assertItems(listOf("1"))

            pager.onItemAccessed(0)
            advanceUntilIdle()
            pager.state.value.assertItems(listOf("1", "2"))

            pager.onItemAccessed(1)
            advanceUntilIdle()
            pager.state.value.assertItems(listOf("1", "2", "3"))
            pager.state.value.assertAppendComplete()
        }

    // ==================== Prepend ====================

    @Test
    fun `scrolling near the start of the list loads the previous page`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = 5, items = listOf("e", "f"), prevKey = 4)
                    page(key = 4, items = listOf("c", "d"), nextKey = 5)
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 2, prefetchDistance = 1),
                    initialKey = 5,
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.state.value.assertItems(listOf("e", "f"))

            pager.onItemAccessed(0)
            advanceUntilIdle()

            pager.state.value.assertItems(listOf("c", "d", "e", "f"))
            pager.state.value.assertPrependComplete()
        }

    // ==================== Retry ====================

    @Test
    fun `calling retry after a failed refresh loads the page successfully`() =
        runTest {
            var callCount = 0
            val source =
                PageSource<Int, String> { _ ->
                    callCount++
                    if (callCount == 1) {
                        PageSourceResult.Error(PagingError.Source("fail"))
                    } else {
                        PageSourceResult.Success(listOf("ok"), null, null)
                    }
                }

            val pager = Pager(config = PagerConfig(pageSize = 1), source = source, scope = this)

            pager.refresh().join()
            pager.state.value.assertRefreshError()

            pager.retry()
            advanceUntilIdle()

            pager.state.value.assertItems(listOf("ok"))
            pager.state.value.assertNoErrors()
        }

    @Test
    fun `calling retry after a failed append loads the next page successfully`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a"), nextKey = 2)
                    page(key = 2, items = listOf("b"))
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 1, prefetchDistance = 1),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()

            source.nextError = PagingError.Source("temporary failure")
            pager.onItemAccessed(0)
            advanceUntilIdle()
            pager.state.value.assertAppendError()

            pager.retry()
            advanceUntilIdle()

            pager.state.value.assertItems(listOf("a", "b"))
            pager.state.value.assertNoErrors()
            pager.state.value.assertAppendComplete()
        }

    // ==================== Load history ====================

    @Test
    fun `each load records the correct key and direction in load history`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a"), nextKey = 2)
                    page(key = 2, items = listOf("b"))
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 1, prefetchDistance = 1),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.onItemAccessed(0)
            advanceUntilIdle()

            assertEquals(2, source.loadHistory.size)
            assertEquals(null, source.loadHistory[0].key)
            assertEquals(Refresh, source.loadHistory[0].direction)
            assertEquals(2, source.loadHistory[1].key)
            assertEquals(Append, source.loadHistory[1].direction)
        }

    // ==================== maxSize trimming ====================

    @Test
    fun `when total items exceed maxSize the oldest pages are dropped`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a", "b"), nextKey = 2)
                    page(key = 2, items = listOf("c", "d"), prevKey = 1, nextKey = 3)
                    page(key = 3, items = listOf("e", "f"), prevKey = 2)
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 2, prefetchDistance = 1, maxSize = 4),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.onItemAccessed(1)
            advanceUntilIdle()
            pager.onItemAccessed(3)
            advanceUntilIdle()

            val state = pager.state.value
            state.assertItems(listOf("c", "d", "e", "f"))
            assertEquals(2, state.pages.size)
            assertEquals(PageState.Idle, state.pageStates.prepend)
        }

    // ==================== Boundary conditions ====================

    @Test
    fun `a source that returns zero items results in an empty complete state`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = emptyList())
                }

            val pager = Pager(config = PagerConfig(pageSize = 10), source = source, scope = this)
            pager.refresh().join()

            pager.state.value.assertItemCount(0)
            pager.state.value.assertAppendComplete()
            pager.state.value.assertPrependComplete()
        }

    @Test
    fun `pages with only one item each are loaded and appended correctly`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("solo-1"), nextKey = 2)
                    page(key = 2, items = listOf("solo-2"), prevKey = 1)
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 1, prefetchDistance = 1),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()
            pager.state.value.assertItems(listOf("solo-1"))

            pager.onItemAccessed(0)
            advanceUntilIdle()

            pager.state.value.assertItems(listOf("solo-1", "solo-2"))
            pager.state.value.assertAppendComplete()
        }

    @Test
    fun `with prefetchDistance zero only accessing the very last item triggers a load`() =
        runTest {
            val source =
                fakePageSource<Int, String> {
                    page(key = null, items = listOf("a", "b", "c"), nextKey = 2)
                    page(key = 2, items = listOf("d", "e", "f"))
                }

            val pager =
                Pager(
                    config = PagerConfig(pageSize = 3, prefetchDistance = 0),
                    source = source,
                    scope = this,
                )

            pager.refresh().join()

            pager.onItemAccessed(0)
            pager.onItemAccessed(1)
            advanceUntilIdle()
            assertEquals(1, pager.state.value.pages.size)

            pager.onItemAccessed(2)
            advanceUntilIdle()

            pager.state.value.assertItems(listOf("a", "b", "c", "d", "e", "f"))
        }
}
