package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class PagerConfigTest {

    // --- Defaults ---

    @Test
    fun `the default page size is 20`() {
        assertEquals(20, PagerConfig().pageSize)
    }

    @Test
    fun `the default prefetch distance equals the page size`() {
        val config = PagerConfig(pageSize = 15)
        assertEquals(15, config.prefetchDistance)
    }

    @Test
    fun `the default max size is null meaning unlimited`() {
        assertNull(PagerConfig().maxSize)
    }

    // --- Custom values ---

    @Test
    fun `custom page size, prefetch distance, and max size are accepted`() {
        val config = PagerConfig(pageSize = 10, prefetchDistance = 5, maxSize = 100)

        assertEquals(10, config.pageSize)
        assertEquals(5, config.prefetchDistance)
        assertEquals(100, config.maxSize)
    }

    @Test
    fun `a prefetch distance of zero is valid`() {
        val config = PagerConfig(pageSize = 10, prefetchDistance = 0)
        assertEquals(0, config.prefetchDistance)
    }

    // --- Validation ---

    @Test(expected = IllegalArgumentException::class)
    fun `a page size of zero is rejected`() {
        PagerConfig(pageSize = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a negative page size is rejected`() {
        PagerConfig(pageSize = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a negative prefetch distance is rejected`() {
        PagerConfig(pageSize = 10, prefetchDistance = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a max size smaller than pageSize plus twice the prefetch distance is rejected`() {
        // pageSize=10, prefetchDistance=10 → minimum maxSize = 10 + 2*10 = 30
        PagerConfig(pageSize = 10, prefetchDistance = 10, maxSize = 20)
    }

    @Test
    fun `a max size at the exact minimum threshold is accepted`() {
        // pageSize=10, prefetchDistance=5 → minimum maxSize = 10 + 2*5 = 20
        val config = PagerConfig(pageSize = 10, prefetchDistance = 5, maxSize = 20)
        assertEquals(20, config.maxSize)
    }

    // --- Equality ---

    @Test
    fun `two configs with the same values are equal`() {
        val a = PagerConfig(pageSize = 25, prefetchDistance = 10)
        val b = PagerConfig(pageSize = 25, prefetchDistance = 10)

        assertEquals(a, b)
    }
}
