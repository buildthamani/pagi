package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class PageStateTest {

    @Test
    fun `Idle is a singleton with referential equality`() {
        assertTrue(PageState.Idle === PageState.Idle)
    }

    @Test
    fun `Loading is a singleton with referential equality`() {
        assertTrue(PageState.Loading === PageState.Loading)
    }

    @Test
    fun `Complete is a singleton with referential equality`() {
        assertTrue(PageState.Complete === PageState.Complete)
    }

    @Test
    fun `an error state carries the original PagingError`() {
        val pagingError = PagingError.Source("timeout")
        val state = PageState.Error(pagingError)

        assertEquals(pagingError, state.error)
    }

    @Test
    fun `two error states with the same PagingError are equal`() {
        val a = PageState.Error(PagingError.Source("timeout"))
        val b = PageState.Error(PagingError.Source("timeout"))

        assertEquals(a, b)
    }

    @Test
    fun `a when expression exhaustively matches all PageState variants`() {
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
