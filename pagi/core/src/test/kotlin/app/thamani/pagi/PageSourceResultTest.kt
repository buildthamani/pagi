package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PageSourceResultTest {

    // --- Success construction ---

    @Test
    fun `a successful result holds items and navigation keys`() {
        val result = PageSourceResult.Success(
            items = listOf("a", "b", "c"),
            prevKey = null,
            nextKey = 2,
        )

        assertEquals(listOf("a", "b", "c"), result.items)
        assertNull(result.prevKey)
        assertEquals(2, result.nextKey)
    }

    @Test
    fun `a successful result can have both prev and next keys`() {
        val result = PageSourceResult.Success(
            items = listOf("x"),
            prevKey = 1,
            nextKey = 3,
        )

        assertEquals(1, result.prevKey)
        assertEquals(3, result.nextKey)
    }

    @Test
    fun `a successful result with null keys means no more data in either direction`() {
        val result = PageSourceResult.Success(
            items = listOf("only"),
            prevKey = null,
            nextKey = null,
        )

        assertNull(result.prevKey)
        assertNull(result.nextKey)
    }

    @Test
    fun `a successful result can hold an empty items list`() {
        val result = PageSourceResult.Success(
            items = emptyList<String>(),
            prevKey = null,
            nextKey = null,
        )

        assertTrue(result.items.isEmpty())
    }

    // --- Error construction ---

    @Test
    fun `an error result wraps a PagingError`() {
        val pagingError = PagingError.Source("timeout")
        val result = PageSourceResult.Error(pagingError)

        assertEquals(pagingError, result.error)
    }

    // --- Pattern matching ---

    @Test
    fun `a when expression can pattern match Success vs Error`() {
        val success: PageSourceResult<Int, String> = PageSourceResult.Success(
            items = listOf("item"),
            prevKey = null,
            nextKey = 2,
        )

        val failure: PageSourceResult<Int, String> = PageSourceResult.Error(
            PagingError.Source("offline"),
        )

        val successMessage = when (success) {
            is PageSourceResult.Success -> "got ${success.items.size} items"
            is PageSourceResult.Error -> "failed: ${success.error.message}"
        }

        val failureMessage = when (failure) {
            is PageSourceResult.Success -> "got ${failure.items.size} items"
            is PageSourceResult.Error -> "failed: ${failure.error.message}"
        }

        assertEquals("got 1 items", successMessage)
        assertEquals("failed: offline", failureMessage)
    }

    // --- Data class equality ---

    @Test
    fun `two identical successful results are equal`() {
        val a = PageSourceResult.Success(listOf("x"), prevKey = null, nextKey = 1)
        val b = PageSourceResult.Success(listOf("x"), prevKey = null, nextKey = 1)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `two identical error results are equal`() {
        val a = PageSourceResult.Error(PagingError.Source("err"))
        val b = PageSourceResult.Error(PagingError.Source("err"))

        assertEquals(a, b)
    }

    // --- Covariance ---

    @Test
    fun `an error result is assignable to any PageSourceResult type thanks to Nothing covariance`() {
        val result: PageSourceResult<Int, String> = PageSourceResult.Error(
            PagingError.Source("test"),
        )

        assertTrue(result is PageSourceResult.Error)
    }
}
