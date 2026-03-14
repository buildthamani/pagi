package app.thamani.pagi

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagerTest {

    // --- Helper to build a simple PageSource from a map ---

    private fun <Key : Any, Value : Any> fakeSource(
        pages: Map<Key?, PageSourceResult<Key, Value>>,
    ): PageSource<Key, Value> = PageSource { params ->
        pages[params.key] ?: PageSourceResult.Error(
            PagingError.Source("No page for key: ${params.key}"),
        )
    }

    // --- Refresh ---

    @Test
    fun `refresh loads initial page`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = listOf("a", "b", "c"),
                    prevKey = null,
                    nextKey = 2,
                ),
            ),
        )

        val pager = Pager(
            config = PagerConfig(pageSize = 3),
            source = source,
        )

        pager.refresh(this).join()

        val state = pager.state.value
        assertEquals(listOf("a", "b", "c"), state.items)
        assertEquals(PageState.Idle, state.pageStates.refresh)
        assertEquals(PageState.Complete, state.pageStates.prepend) // no prevKey
        assertEquals(PageState.Idle, state.pageStates.append)     // has nextKey
        assertEquals(1, state.pages.size)
    }

    @Test
    fun `refresh with no next key marks append Complete`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = listOf("only"),
                    prevKey = null,
                    nextKey = null,
                ),
            ),
        )

        val pager = Pager(config = PagerConfig(pageSize = 1), source = source)
        pager.refresh(this).join()

        assertEquals(PageState.Complete, pager.state.value.pageStates.append)
        assertEquals(PageState.Complete, pager.state.value.pageStates.prepend)
    }

    @Test
    fun `refresh clears previous data`() = runTest {
        var callCount = 0
        val source = PageSource<Int, String> { _ ->
            callCount++
            PageSourceResult.Success(
                items = listOf("batch-$callCount"),
                prevKey = null,
                nextKey = null,
            )
        }

        val pager = Pager(config = PagerConfig(pageSize = 1), source = source)

        pager.refresh(this).join()
        assertEquals(listOf("batch-1"), pager.state.value.items)

        pager.refresh(this).join()
        assertEquals(listOf("batch-2"), pager.state.value.items)
        assertEquals(1, pager.state.value.pages.size) // old pages cleared
    }

    // --- Error handling ---

    @Test
    fun `source error is captured as PageState Error`() = runTest {
        val source = fakeSource<Int, String>(
            mapOf(
                null to PageSourceResult.Error(
                    PagingError.Source("network timeout"),
                ),
            ),
        )

        val pager = Pager(config = PagerConfig(pageSize = 10), source = source)
        pager.refresh(this).join()

        val state = pager.state.value
        assertTrue(state.items.isEmpty())
        assertTrue(state.pageStates.refresh is PageState.Error)
        assertEquals(
            "network timeout",
            (state.pageStates.refresh as PageState.Error).error.message,
        )
    }

    @Test
    fun `uncaught exception is wrapped as PagingError Source`() = runTest {
        val source = PageSource<Int, String> { _ ->
            throw RuntimeException("boom")
        }

        val pager = Pager(config = PagerConfig(pageSize = 10), source = source)
        pager.refresh(this).join()

        val error = pager.state.value.pageStates.refresh
        assertTrue(error is PageState.Error)
        assertTrue((error as PageState.Error).error.message.contains("boom"))
    }

    @Test
    fun `CancellationException is not swallowed`() = runTest {
        val source = PageSource<Int, String> { _ ->
            delay(10_000) // will be cancelled
            PageSourceResult.Success(listOf("x"), null, null)
        }

        val pager = Pager(config = PagerConfig(pageSize = 1), source = source)
        val job = pager.refresh(this)
        job.cancelAndJoin()

        // After cancellation, refresh should still be Loading (not Error)
        // because CancellationException was rethrown, not caught
        val refreshState = pager.state.value.pageStates.refresh
        assertTrue(
            "Expected Loading after cancellation, got $refreshState",
            refreshState is PageState.Loading,
        )
    }

    // --- Append ---

    @Test
    fun `append loads next page when item near end is accessed`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = listOf("a", "b"),
                    prevKey = null,
                    nextKey = 2,
                ),
                2 to PageSourceResult.Success(
                    items = listOf("c", "d"),
                    prevKey = 1,
                    nextKey = null,
                ),
            ),
        )

        val pager = Pager(
            config = PagerConfig(pageSize = 2, prefetchDistance = 1),
            source = source,
        )

        pager.refresh(this).join()
        assertEquals(listOf("a", "b"), pager.state.value.items)

        // Access last item — within prefetchDistance
        pager.onItemAccessed(1, this)
        advanceUntilIdle()

        assertEquals(listOf("a", "b", "c", "d"), pager.state.value.items)
        assertEquals(PageState.Complete, pager.state.value.pageStates.append)
    }

    @Test
    fun `append error is captured`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = listOf("a"),
                    prevKey = null,
                    nextKey = 2,
                ),
                2 to PageSourceResult.Error(PagingError.Source("append failed")),
            ),
        )

        val pager = Pager(
            config = PagerConfig(pageSize = 1, prefetchDistance = 1),
            source = source,
        )

        pager.refresh(this).join()
        pager.onItemAccessed(0, this)
        advanceUntilIdle()

        assertTrue(pager.state.value.pageStates.append is PageState.Error)
    }

    // --- Prepend ---

    @Test
    fun `prepend loads previous page when item near start is accessed`() = runTest {
        val source = fakeSource(
            mapOf(
                5 to PageSourceResult.Success(
                    items = listOf("e", "f"),
                    prevKey = 4,
                    nextKey = null,
                ),
                4 to PageSourceResult.Success(
                    items = listOf("c", "d"),
                    prevKey = null,
                    nextKey = 5,
                ),
            ),
        )

        val pager = Pager(
            config = PagerConfig(pageSize = 2, prefetchDistance = 1),
            initialKey = 5,
            source = source,
        )

        pager.refresh(this).join()
        assertEquals(listOf("e", "f"), pager.state.value.items)

        // Access first item — within prefetchDistance
        pager.onItemAccessed(0, this)
        advanceUntilIdle()

        assertEquals(listOf("c", "d", "e", "f"), pager.state.value.items)
        assertEquals(PageState.Complete, pager.state.value.pageStates.prepend)
    }

    // --- Retry ---

    @Test
    fun `retry retries failed refresh`() = runTest {
        var callCount = 0
        val source = PageSource<Int, String> { _ ->
            callCount++
            if (callCount == 1) {
                PageSourceResult.Error(PagingError.Source("fail first time"))
            } else {
                PageSourceResult.Success(listOf("ok"), null, null)
            }
        }

        val pager = Pager(config = PagerConfig(pageSize = 1), source = source)

        pager.refresh(this).join()
        assertTrue(pager.state.value.pageStates.refresh is PageState.Error)

        pager.retry(this)
        advanceUntilIdle()

        assertEquals(listOf("ok"), pager.state.value.items)
        assertEquals(PageState.Idle, pager.state.value.pageStates.refresh)
    }

    @Test
    fun `retry retries failed append`() = runTest {
        var appendCallCount = 0
        val source = PageSource<Int, String> { params ->
            if (params.key == null) {
                PageSourceResult.Success(listOf("a"), null, 2)
            } else {
                appendCallCount++
                if (appendCallCount == 1) {
                    PageSourceResult.Error(PagingError.Source("append fail"))
                } else {
                    PageSourceResult.Success(listOf("b"), null, null)
                }
            }
        }

        val pager = Pager(
            config = PagerConfig(pageSize = 1, prefetchDistance = 1),
            source = source,
        )

        pager.refresh(this).join()
        pager.onItemAccessed(0, this)
        advanceUntilIdle()
        assertTrue(pager.state.value.pageStates.append is PageState.Error)

        pager.retry(this)
        advanceUntilIdle()

        assertEquals(listOf("a", "b"), pager.state.value.items)
        assertEquals(PageState.Complete, pager.state.value.pageStates.append)
    }

    // --- maxSize trimming ---

    @Test
    fun `maxSize trims pages from front on append`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = listOf("a", "b"),
                    prevKey = null,
                    nextKey = 2,
                ),
                2 to PageSourceResult.Success(
                    items = listOf("c", "d"),
                    prevKey = 1,
                    nextKey = 3,
                ),
                3 to PageSourceResult.Success(
                    items = listOf("e", "f"),
                    prevKey = 2,
                    nextKey = null,
                ),
            ),
        )

        // maxSize = 4, so after loading 3 pages (6 items), oldest page is trimmed
        val pager = Pager(
            config = PagerConfig(pageSize = 2, prefetchDistance = 1, maxSize = 4),
            source = source,
        )

        pager.refresh(this).join()
        pager.onItemAccessed(1, this) // trigger append page 2
        advanceUntilIdle()
        pager.onItemAccessed(3, this) // trigger append page 3
        advanceUntilIdle()

        val state = pager.state.value
        // First page should have been trimmed
        assertEquals(listOf("c", "d", "e", "f"), state.items)
        assertEquals(2, state.pages.size)
        // Prepend should be reset to Idle (was Complete, but we trimmed)
        assertEquals(PageState.Idle, state.pageStates.prepend)
    }

    // --- Boundary conditions ---

    @Test
    fun `empty page is handled`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = emptyList<String>(),
                    prevKey = null,
                    nextKey = null,
                ),
            ),
        )

        val pager = Pager(config = PagerConfig(pageSize = 10), source = source)
        pager.refresh(this).join()

        assertTrue(pager.state.value.items.isEmpty())
        assertEquals(PageState.Complete, pager.state.value.pageStates.append)
        assertEquals(PageState.Complete, pager.state.value.pageStates.prepend)
    }

    @Test
    fun `prefetchDistance zero means no automatic loading`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(
                    items = listOf("a", "b", "c"),
                    prevKey = null,
                    nextKey = 2,
                ),
            ),
        )

        val pager = Pager(
            config = PagerConfig(pageSize = 3, prefetchDistance = 0),
            source = source,
        )

        pager.refresh(this).join()

        // Accessing items that aren't at the very edge shouldn't trigger loading
        pager.onItemAccessed(0, this)
        pager.onItemAccessed(1, this)
        advanceUntilIdle()

        assertEquals(1, pager.state.value.pages.size) // no additional loads
    }

    @Test
    fun `multi-page append flow`() = runTest {
        val source = fakeSource(
            mapOf(
                null to PageSourceResult.Success(listOf("1"), null, "b"),
                "b" to PageSourceResult.Success(listOf("2"), "a", "c"),
                "c" to PageSourceResult.Success(listOf("3"), "b", null),
            ),
        )

        val pager = Pager(
            config = PagerConfig(pageSize = 1, prefetchDistance = 1),
            source = source,
        )

        pager.refresh(this).join()
        assertEquals(listOf("1"), pager.state.value.items)

        pager.onItemAccessed(0, this)
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), pager.state.value.items)

        pager.onItemAccessed(1, this)
        advanceUntilIdle()
        assertEquals(listOf("1", "2", "3"), pager.state.value.items)
        assertEquals(PageState.Complete, pager.state.value.pageStates.append)
    }
}
