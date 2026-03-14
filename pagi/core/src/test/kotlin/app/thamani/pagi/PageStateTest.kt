package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PageStateTest {

    @Test
    fun `Idle is a singleton`() {
        assertTrue(PageState.Idle === PageState.Idle)
    }

    @Test
    fun `Loading is a singleton`() {
        assertTrue(PageState.Loading === PageState.Loading)
    }

    @Test
    fun `Complete is a singleton`() {
        assertTrue(PageState.Complete === PageState.Complete)
    }

    @Test
    fun `Error carries a PagingError`() {
        val pagingError = PagingError.Source("timeout")
        val state = PageState.Error(pagingError)

        assertEquals(pagingError, state.error)
    }

    @Test
    fun `Error equality is based on the wrapped error`() {
        val a = PageState.Error(PagingError.Source("timeout"))
        val b = PageState.Error(PagingError.Source("timeout"))

        assertEquals(a, b)
    }

    @Test
    fun `when expression covers all variants`() {
        val states = listOf(
            PageState.Idle,
            PageState.Loading,
            PageState.Complete,
            PageState.Error(PagingError.Source("err")),
        )

        val labels = states.map { state ->
            when (state) {
                is PageState.Idle -> "idle"
                is PageState.Loading -> "loading"
                is PageState.Complete -> "complete"
                is PageState.Error -> "error"
            }
        }

        assertEquals(listOf("idle", "loading", "complete", "error"), labels)
    }
}
