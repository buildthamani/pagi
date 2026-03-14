package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class LoadDirectionTest {

    @Test
    fun `Refresh is a singleton`() {
        assertTrue(Refresh === Refresh)
    }

    @Test
    fun `Prepend is a singleton`() {
        assertTrue(Prepend === Prepend)
    }

    @Test
    fun `Append is a singleton`() {
        assertTrue(Append === Append)
    }

    @Test
    fun `when expression covers all variants`() {
        val directions: List<LoadDirection> = listOf(
            Refresh,
            Prepend,
            Append,
        )

        val labels = directions.map { direction ->
            when (direction) {
                is Refresh -> "refresh"
                is Prepend -> "prepend"
                is Append -> "append"
            }
        }

        assertEquals(listOf("refresh", "prepend", "append"), labels)
    }
}
