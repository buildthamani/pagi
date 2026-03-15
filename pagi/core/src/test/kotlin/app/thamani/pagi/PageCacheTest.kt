package app.thamani.pagi

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PageCacheTest {

    @Test
    fun `a custom PageCache implementation works with get, put, and clear`() = runTest {
        val backing = mutableMapOf<Int?, PageSourceResult.Success<Int, String>>()

        val cache = object : PageCache<Int, String> {
            override suspend fun get(key: Int?) = backing[key]
            override suspend fun put(key: Int?, page: PageSourceResult.Success<Int, String>) {
                backing[key] = page
            }
            override suspend fun clear() { backing.clear() }
        }

        // Initially empty
        assertNull(cache.get(null))

        // Put and get
        val page = PageSourceResult.Success(listOf("a"), prevKey = null, nextKey = 2)
        cache.put(null, page)
        assertEquals(page, cache.get(null))

        // Clear
        cache.clear()
        assertNull(cache.get(null))
        assertTrue(backing.isEmpty())
    }
}
