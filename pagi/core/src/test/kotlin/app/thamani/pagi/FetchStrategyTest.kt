package app.thamani.pagi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchStrategyTest {

    @Test
    fun `CacheFirst is a singleton with referential equality`() {
        assertTrue(CacheFirst === CacheFirst)
    }

    @Test
    fun `NetworkFirst is a singleton with referential equality`() {
        assertTrue(NetworkFirst === NetworkFirst)
    }

    @Test
    fun `CacheFirst and NetworkFirst are both FetchStrategy`() {
        val strategies: List<FetchStrategy> = listOf(CacheFirst, NetworkFirst)

        assertTrue(strategies[0] is CacheFirst)
        assertTrue(strategies[1] is NetworkFirst)
    }

    @Test
    fun `a when expression exhaustively matches all FetchStrategy variants`() {
        val strategies: List<FetchStrategy> = listOf(CacheFirst, NetworkFirst)

        val labels = strategies.map { strategy ->
            when (strategy) {
                is CacheFirst -> "cache-first"
                is NetworkFirst -> "network-first"
            }
        }

        assertEquals(listOf("cache-first", "network-first"), labels)
    }
}
