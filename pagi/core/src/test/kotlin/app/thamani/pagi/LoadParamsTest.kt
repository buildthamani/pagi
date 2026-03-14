package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class LoadParamsTest {
    @Test
    fun `load params store the key, load size, and direction`() {
        val params = LoadParams(key = 5, loadSize = 20, direction = Append)

        assertEquals(5, params.key)
        assertEquals(20, params.loadSize)
        assertEquals(Append, params.direction)
    }

    @Test
    fun `the key can be null for an initial load`() {
        val params = LoadParams<Int>(key = null, loadSize = 10, direction = Refresh)

        assertNull(params.key)
    }

    @Test
    fun `two load params with identical values are equal`() {
        val a = LoadParams(key = 1, loadSize = 20, direction = Append)
        val b = LoadParams(key = 1, loadSize = 20, direction = Append)

        assertEquals(a, b)
    }
}
