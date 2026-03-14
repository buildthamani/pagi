package app.thamani.pagi.testing

import app.thamani.pagi.Append
import app.thamani.pagi.LoadParams
import app.thamani.pagi.PageSourceResult
import app.thamani.pagi.PagingError
import app.thamani.pagi.Refresh
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePageSourceTest {

    // --- Serving pages ---

    @Test
    fun `loading a configured key returns the matching page`() = runTest {
        val source = FakePageSource(
            mapOf(
                null to FakePage(
                    items = listOf("a", "b"),
                    nextKey = 2,
                ),
            ),
        )

        val result = source.load(
            LoadParams(key = null, loadSize = 2, direction = Refresh),
        )

        assertTrue(result is PageSourceResult.Success)
        val success = result as PageSourceResult.Success
        assertEquals(listOf("a", "b"), success.items)
        assertNull(success.prevKey)
        assertEquals(2, success.nextKey)
    }

    @Test
    fun `loading an unconfigured key returns an error`() = runTest {
        val source = FakePageSource<Int, String>(emptyMap())

        val result = source.load(
            LoadParams(key = null, loadSize = 10, direction = Refresh),
        )

        assertTrue(result is PageSourceResult.Error)
    }

    @Test
    fun `different keys return their own configured pages`() = runTest {
        val source = FakePageSource(
            mapOf(
                null to FakePage(listOf("first"), nextKey = 2),
                2 to FakePage(listOf("second"), prevKey = null, nextKey = null),
            ),
        )

        val first = source.load(LoadParams(null, 1, Refresh))
        val second = source.load(LoadParams(2, 1, Append))

        assertEquals(listOf("first"), (first as PageSourceResult.Success).items)
        assertEquals(listOf("second"), (second as PageSourceResult.Success).items)
    }

    // --- Load history ---

    @Test
    fun `every load call is recorded in the load history`() = runTest {
        val source = FakePageSource(
            mapOf(
                null to FakePage(listOf("a"), nextKey = 2),
                2 to FakePage(listOf("b")),
            ),
        )

        source.load(LoadParams(null, 10, Refresh))
        source.load(LoadParams(2, 10, Append))

        assertEquals(2, source.loadHistory.size)
        assertNull(source.loadHistory[0].key)
        assertEquals(2, source.loadHistory[1].key)
        assertEquals(Refresh, source.loadHistory[0].direction)
        assertEquals(Append, source.loadHistory[1].direction)
    }

    @Test
    fun `the load history starts empty before any loads`() {
        val source = FakePageSource<Int, String>(emptyMap())
        assertTrue(source.loadHistory.isEmpty())
    }

    // --- Error injection ---

    @Test
    fun `setting nextError makes the next load return an error`() = runTest {
        val source = FakePageSource(
            mapOf(null to FakePage(listOf("a"))),
        )

        source.nextError = PagingError.Source("injected error")
        val result = source.load(LoadParams(null, 1, Refresh))

        assertTrue(result is PageSourceResult.Error)
        assertEquals("injected error", (result as PageSourceResult.Error).error.message)
    }

    @Test
    fun `nextError automatically clears itself after one load`() = runTest {
        val source = FakePageSource(
            mapOf(null to FakePage(listOf("a"))),
        )

        source.nextError = PagingError.Source("one-shot")

        // First load: error
        val first = source.load(LoadParams(null, 1, Refresh))
        assertTrue(first is PageSourceResult.Error)

        // Second load: success (error cleared)
        val second = source.load(LoadParams(null, 1, Refresh))
        assertTrue(second is PageSourceResult.Success)
    }

    @Test
    fun `nextError is null by default`() {
        val source = FakePageSource<Int, String>(emptyMap())
        assertNull(source.nextError)
    }

    @Test
    fun `the load history is recorded even when an error is injected`() = runTest {
        val source = FakePageSource(
            mapOf(null to FakePage(listOf("a"))),
        )

        source.nextError = PagingError.Source("err")
        source.load(LoadParams(null, 5, Refresh))

        assertEquals(1, source.loadHistory.size)
        assertEquals(5, source.loadHistory[0].loadSize)
    }

    // --- DSL builder ---

    @Test
    fun `the fakePageSource DSL builds a source with the correct pages`() = runTest {
        val source = fakePageSource<Int, String> {
            page(key = null, items = listOf("x", "y"), nextKey = 2)
            page(key = 2, items = listOf("z"), prevKey = 1, nextKey = null)
        }

        val first = source.load(LoadParams(null, 2, Refresh))
        val second = source.load(LoadParams(2, 1, Append))

        assertTrue(first is PageSourceResult.Success)
        assertEquals(listOf("x", "y"), (first as PageSourceResult.Success).items)
        assertEquals(2, first.nextKey)

        assertTrue(second is PageSourceResult.Success)
        assertEquals(listOf("z"), (second as PageSourceResult.Success).items)
        assertEquals(1, second.prevKey)
        assertNull(second.nextKey)
    }

    @Test
    fun `the fakePageSource DSL with no pages builds an empty source`() = runTest {
        val source = fakePageSource<Int, String> {}

        val result = source.load(LoadParams(null, 1, Refresh))
        assertTrue(result is PageSourceResult.Error)
    }
}
