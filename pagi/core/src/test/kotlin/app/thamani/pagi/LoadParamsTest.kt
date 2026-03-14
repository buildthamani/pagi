package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

class LoadParamsTest {

    @Test
    fun `stores key, loadSize, and direction`() {
        val params = LoadParams(key = 5, loadSize = 20, direction = Append)

        assertEquals(5, params.key)
        assertEquals(20, params.loadSize)
        assertEquals(Append, params.direction)
    }

    @Test
    fun `key can be null for initial load`() {
        val params = LoadParams<Int>(key = null, loadSize = 10, direction = Refresh)

        assertNull(params.key)
    }

    @Test
    fun `identical params are equal`() {
        val a = LoadParams(key = 1, loadSize = 20, direction = LoadDirection.APPEND)
        val b = LoadParams(key = 1, loadSize = 20, direction = LoadDirection.APPEND)

        assertEquals(a, b)
    }
}
