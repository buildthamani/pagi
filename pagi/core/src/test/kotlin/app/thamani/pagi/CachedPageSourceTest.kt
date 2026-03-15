package app.thamani.pagi

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CachedPageSourceTest {

    // ==================== CacheFirst (default) ====================

    @Test
    fun `cache first — a cache miss fetches from the network`() = runTest {
        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("a", "b"), prevKey = null, nextKey = 2)
        }
        val source = CachedPageSource(network, InMemoryPageCache())

        val result = source.load(LoadParams(null, 20, Refresh))

        assertTrue(result is PageSourceResult.Success)
        assertEquals(listOf("a", "b"), (result as PageSourceResult.Success).items)
        assertEquals(1, networkCalls)
    }

    @Test
    fun `cache first — a cache hit returns cached data without calling the network`() = runTest {
        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("a", "b"), prevKey = null, nextKey = 2)
        }
        val source = CachedPageSource(network, InMemoryPageCache())

        source.load(LoadParams(null, 20, Append))
        assertEquals(1, networkCalls)

        val result = source.load(LoadParams(null, 20, Append))
        assertEquals(1, networkCalls)
        assertEquals(listOf("a", "b"), (result as PageSourceResult.Success).items)
    }

    @Test
    fun `cache first — a network error is returned but not cached`() = runTest {
        var callCount = 0
        val network = PageSource<Int, String> {
            callCount++
            if (callCount == 1) {
                PageSourceResult.Error(PagingError.Source("timeout"))
            } else {
                PageSourceResult.Success(listOf("a"), prevKey = null, nextKey = null)
            }
        }
        val source = CachedPageSource(network, InMemoryPageCache())

        val error = source.load(LoadParams(null, 20, Append))
        assertTrue(error is PageSourceResult.Error)

        val success = source.load(LoadParams(null, 20, Append))
        assertTrue(success is PageSourceResult.Success)
        assertEquals(2, callCount)
    }

    @Test
    fun `cache first — a successful result is stored in the cache`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val network = PageSource<Int, String> {
            PageSourceResult.Success(listOf("x"), prevKey = null, nextKey = 2)
        }
        val source = CachedPageSource(network, cache)

        source.load(LoadParams(null, 20, Append))

        assertEquals(listOf("x"), cache.get(null)?.items)
    }

    @Test
    fun `cache first — different keys are cached independently`() = runTest {
        var networkCalls = 0
        val network = PageSource<Int, String> { params ->
            networkCalls++
            val key = params.key ?: 0
            PageSourceResult.Success(listOf("page-$key"), null, key + 1)
        }
        val source = CachedPageSource(network, InMemoryPageCache())

        source.load(LoadParams(null, 20, Append))
        source.load(LoadParams(1, 20, Append))
        assertEquals(2, networkCalls)

        source.load(LoadParams(null, 20, Append))
        source.load(LoadParams(1, 20, Append))
        assertEquals(2, networkCalls)
    }

    @Test
    fun `cache first — the default strategy is CacheFirst`() = runTest {
        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("data"), prevKey = null, nextKey = null)
        }
        // No strategy parameter — should default to CacheFirst
        val source = CachedPageSource(network, InMemoryPageCache())

        source.load(LoadParams(null, 20, Append))
        source.load(LoadParams(null, 20, Append))

        assertEquals(1, networkCalls) // second load was a cache hit
    }

    // ==================== NetworkFirst ====================

    @Test
    fun `network first — always hits the network even when cache has data`() = runTest {
        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("fresh-$networkCalls"), prevKey = null, nextKey = 2)
        }
        val source = CachedPageSource(network, InMemoryPageCache(), strategy = NetworkFirst)

        source.load(LoadParams(null, 20, Append))
        val result = source.load(LoadParams(null, 20, Append))

        assertEquals(2, networkCalls)
        assertEquals(listOf("fresh-2"), (result as PageSourceResult.Success).items)
    }

    @Test
    fun `network first — caches successful network results`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val network = PageSource<Int, String> {
            PageSourceResult.Success(listOf("x"), prevKey = null, nextKey = null)
        }
        val source = CachedPageSource(network, cache, strategy = NetworkFirst)

        source.load(LoadParams(null, 20, Append))

        assertEquals(listOf("x"), cache.get(null)?.items)
    }

    @Test
    fun `network first — falls back to cache when network fails`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val cachedPage = PageSourceResult.Success(listOf("stale"), prevKey = null, nextKey = 2)
        cache.put(null, cachedPage)

        val network = PageSource<Int, String> {
            PageSourceResult.Error(PagingError.Source("offline"))
        }
        val source = CachedPageSource(network, cache, strategy = NetworkFirst)

        val result = source.load(LoadParams(null, 20, Append))

        assertTrue(result is PageSourceResult.Success)
        assertEquals(listOf("stale"), (result as PageSourceResult.Success).items)
    }

    @Test
    fun `network first — returns error when both network and cache miss`() = runTest {
        val network = PageSource<Int, String> {
            PageSourceResult.Error(PagingError.Source("offline"))
        }
        val source = CachedPageSource(network, InMemoryPageCache(), strategy = NetworkFirst)

        val result = source.load(LoadParams(null, 20, Append))

        assertTrue(result is PageSourceResult.Error)
        assertEquals("offline", (result as PageSourceResult.Error).error.message)
    }

    @Test
    fun `network first — updates cache with fresh data on success`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        cache.put(null, PageSourceResult.Success(listOf("old"), null, 2))

        val network = PageSource<Int, String> {
            PageSourceResult.Success(listOf("new"), prevKey = null, nextKey = 2)
        }
        val source = CachedPageSource(network, cache, strategy = NetworkFirst)

        source.load(LoadParams(null, 20, Append))

        assertEquals(listOf("new"), cache.get(null)?.items)
    }

    @Test
    fun `network first — does not cache errors`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        val network = PageSource<Int, String> {
            PageSourceResult.Error(PagingError.Source("fail"))
        }
        val source = CachedPageSource(network, cache, strategy = NetworkFirst)

        source.load(LoadParams(null, 20, Append))

        assertNull(cache.get(null))
    }

    // ==================== Refresh behavior (both strategies) ====================

    @Test
    fun `cache first — refresh clears the cache before fetching`() = runTest {
        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("item-$networkCalls"), prevKey = null, nextKey = 2)
        }
        val cache = InMemoryPageCache<Int, String>()
        val source = CachedPageSource(network, cache)

        source.load(LoadParams(null, 20, Append))
        assertEquals(1, networkCalls)

        val result = source.load(LoadParams(null, 20, Refresh))
        assertEquals(2, networkCalls)
        assertEquals(listOf("item-2"), (result as PageSourceResult.Success).items)
    }

    @Test
    fun `cache first — refresh clears all pages not just the requested key`() = runTest {
        val network = PageSource<Int, String> { params ->
            val key = params.key ?: 0
            PageSourceResult.Success(listOf("page-$key"), null, key + 1)
        }
        val cache = InMemoryPageCache<Int, String>()
        val source = CachedPageSource(network, cache)

        source.load(LoadParams(null, 20, Append))
        source.load(LoadParams(1, 20, Append))
        assertEquals(2, cache.size())

        source.load(LoadParams(null, 20, Refresh))
        assertEquals(1, cache.size())
        assertNull(cache.get(1))
    }

    @Test
    fun `network first — refresh clears the cache before fetching`() = runTest {
        val cache = InMemoryPageCache<Int, String>()
        cache.put(null, PageSourceResult.Success(listOf("stale"), null, 2))
        cache.put(1, PageSourceResult.Success(listOf("stale-2"), null, null))

        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("fresh"), prevKey = null, nextKey = 2)
        }
        val source = CachedPageSource(network, cache, strategy = NetworkFirst)

        source.load(LoadParams(null, 20, Refresh))

        assertEquals(1, cache.size()) // old pages cleared, only fresh page remains
        assertEquals(listOf("fresh"), cache.get(null)?.items)
        assertNull(cache.get(1))
    }

    // ==================== Custom cache ====================

    @Test
    fun `works with a custom PageCache implementation`() = runTest {
        val noopCache = object : PageCache<Int, String> {
            override suspend fun get(key: Int?) = null
            override suspend fun put(key: Int?, page: PageSourceResult.Success<Int, String>) {}
            override suspend fun clear() {}
        }

        var networkCalls = 0
        val network = PageSource<Int, String> {
            networkCalls++
            PageSourceResult.Success(listOf("data"), prevKey = null, nextKey = null)
        }
        val source = CachedPageSource(network, noopCache)

        source.load(LoadParams(null, 20, Append))
        source.load(LoadParams(null, 20, Append))

        assertEquals(2, networkCalls)
    }
}
