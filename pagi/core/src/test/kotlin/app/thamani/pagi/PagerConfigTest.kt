package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class PagerConfigTest {

    // --- Defaults ---

    @Test
    fun `default pageSize is 20`() {
        assertEquals(20, PagerConfig().pageSize)
    }

    @Test
    fun `default prefetchDistance equals pageSize`() {
        val config = PagerConfig(pageSize = 15)
        assertEquals(15, config.prefetchDistance)
    }

    @Test
    fun `default maxSize is null`() {
        assertNull(PagerConfig().maxSize)
    }

    // --- Custom values ---

    @Test
    fun `accepts custom values`() {
        val config = PagerConfig(pageSize = 10, prefetchDistance = 5, maxSize = 100)

        assertEquals(10, config.pageSize)
        assertEquals(5, config.prefetchDistance)
        assertEquals(100, config.maxSize)
    }

    @Test
    fun `prefetchDistance can be zero`() {
        val config = PagerConfig(pageSize = 10, prefetchDistance = 0)
        assertEquals(0, config.prefetchDistance)
    }

    // --- Validation ---

    @Test(expected = IllegalArgumentException::class)
    fun `pageSize must be greater than zero`() {
        PagerConfig(pageSize = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative pageSize is rejected`() {
        PagerConfig(pageSize = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative prefetchDistance is rejected`() {
        PagerConfig(pageSize = 10, prefetchDistance = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxSize too small is rejected`() {
        // pageSize=10, prefetchDistance=10 → minimum maxSize = 10 + 2*10 = 30
        PagerConfig(pageSize = 10, prefetchDistance = 10, maxSize = 20)
    }

    @Test
    fun `maxSize at exact minimum is accepted`() {
        // pageSize=10, prefetchDistance=5 → minimum maxSize = 10 + 2*5 = 20
        val config = PagerConfig(pageSize = 10, prefetchDistance = 5, maxSize = 20)
        assertEquals(20, config.maxSize)
    }

    // --- Equality ---

    @Test
    fun `identical configs are equal`() {
        val a = PagerConfig(pageSize = 25, prefetchDistance = 10)
        val b = PagerConfig(pageSize = 25, prefetchDistance = 10)

        assertEquals(a, b)
    }
}
