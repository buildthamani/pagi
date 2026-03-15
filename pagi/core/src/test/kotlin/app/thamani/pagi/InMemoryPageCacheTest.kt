package app.thamani.pagi

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryPageCacheTest {

    @Test
    fun `getting a key that was never stored returns null`() = runTest {
        val cache = InMemoryPageCache<Int, String>()

        assertNull(cache.get(null))
        assertNull(cache.get(1))
    }

    @Test
    fun `putting a page then getting it returns the same page`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val page = PageSourceResult.Success(
            items = listOf("a", "b"),
            prevKey = null,
            nextKey = 2,
        )

        cache.put(null, page)

        assertEquals(page, cache.get(null))
    }

    @Test
    fun `putting pages with different keys stores them independently`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val page1 = PageSourceResult.Success(listOf("a"), prevKey = null, nextKey = 2)
        val page2 = PageSourceResult.Success(listOf("b"), prevKey = 1, nextKey = 3)

        cache.put(null, page1)
        cache.put(2, page2)

        assertEquals(page1, cache.get(null))
        assertEquals(page2, cache.get(2))
        assertEquals(2, cache.size())
    }

    @Test
    fun `putting a page with an existing key overwrites the old value`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val original = PageSourceResult.Success(listOf("old"), prevKey = null, nextKey = 2)
        val updated = PageSourceResult.Success(listOf("new"), prevKey = null, nextKey = 2)

        cache.put(null, original)
        cache.put(null, updated)

        assertEquals(updated, cache.get(null))
        assertEquals(1, cache.size())
    }

    @Test
    fun `clearing removes all cached pages`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        cache.put(null, PageSourceResult.Success(listOf("a"), null, 2))
        cache.put(2, PageSourceResult.Success(listOf("b"), 1, null))

        cache.clear()

        assertNull(cache.get(null))
        assertNull(cache.get(2))
        assertEquals(0, cache.size())
    }

    @Test
    fun `size returns the number of cached pages`() = runTest {
        val cache = InMemoryPageCache<Int, String>()

        assertEquals(0, cache.size())

        cache.put(null, PageSourceResult.Success(listOf("a"), null, 2))
        assertEquals(1, cache.size())

        cache.put(2, PageSourceResult.Success(listOf("b"), 1, null))
        assertEquals(2, cache.size())
    }
}
