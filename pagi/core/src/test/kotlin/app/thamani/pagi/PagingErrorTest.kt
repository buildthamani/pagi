package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PagingErrorTest {
    @Test
    fun `Source error stores message and cause`() {
        val cause = RuntimeException("connection reset")
        val error = PagingError.Source("Timeout", cause)

        assertEquals("Timeout", error.message)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `Source error cause defaults to null`() {
        val error = PagingError.Source("Something failed")

        assertEquals("Something failed", error.message)
        assertNull(error.cause)
    }

    // --- Equality (data class) ---

    @Test
    fun `same Source errors are equal`() {
        val a = PagingError.Source("timeout")
        val b = PagingError.Source("timeout")

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `different Source errors are not equal`() {
        val a = PagingError.Source("timeout")
        val b = PagingError.Source("refused")

        assertTrue(a != b)
    }

    // --- Type hierarchy ---

    @Test
    fun `Source error is a PagingError`() {
        val error: PagingError = PagingError.Source("test")

        assertTrue(error is PagingError.Source)
    }

    @Test
    fun `message is accessible through PagingError interface`() {
        val error: PagingError = PagingError.Source("fetch failed")

        assertEquals("fetch failed", error.message)
    }

    // --- Exhaustive matching ---

    @Test
    fun `when expression covers all current variants`() {
        val error: PagingError = PagingError.Source("timeout")

        // This compiles exhaustively — if we add a new variant later,
        // the compiler will flag this as incomplete.
        val result =
            when (error) {
                is PagingError.Source -> "source: ${error.message}"
            }

        assertEquals("source: timeout", result)
    }
}
