package app.thamani.pagi.testing

import app.thamani.pagi.PageState
import app.thamani.pagi.PageStates
import app.thamani.pagi.PagingError
import app.thamani.pagi.PagingState
import org.junit.Assert.fail
import org.junit.Test

class PagingAssertionsTest {

    // --- assertItemCount ---

    @Test
    fun `assertItemCount passes when the actual count matches the expected count`() {
        val state = PagingState<Int, String>(items = listOf("a", "b"))
        state.assertItemCount(2) // should not throw
    }

    @Test
    fun `assertItemCount fails when the actual count does not match`() {
        val state = PagingState<Int, String>(items = listOf("a"))
        expectFailure { state.assertItemCount(5) }
    }

    // --- assertItems ---

    @Test
    fun `assertItems passes when the items list matches exactly`() {
        val state = PagingState<Int, String>(items = listOf("x", "y"))
        state.assertItems(listOf("x", "y"))
    }

    @Test
    fun `assertItems fails when the items list differs`() {
        val state = PagingState<Int, String>(items = listOf("x"))
        expectFailure { state.assertItems(listOf("y")) }
    }

    // --- assertNoErrors ---

    @Test
    fun `assertNoErrors passes when no direction has an error`() {
        val state = PagingState<Int, String>()
        state.assertNoErrors()
    }

    @Test
    fun `assertNoErrors fails when the refresh direction has an error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(refresh = PageState.Error(PagingError.Source("err"))),
        )
        expectFailure { state.assertNoErrors() }
    }

    @Test
    fun `assertNoErrors fails when the append direction has an error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(append = PageState.Error(PagingError.Source("err"))),
        )
        expectFailure { state.assertNoErrors() }
    }

    // --- assertRefreshLoading ---

    @Test
    fun `assertRefreshLoading passes when the refresh direction is Loading`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(refresh = PageState.Loading),
        )
        state.assertRefreshLoading()
    }

    @Test
    fun `assertRefreshLoading fails when the refresh direction is Idle`() {
        val state = PagingState<Int, String>()
        expectFailure { state.assertRefreshLoading() }
    }

    // --- assertRefreshError ---

    @Test
    fun `assertRefreshError passes when the refresh direction is Error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(refresh = PageState.Error(PagingError.Source("err"))),
        )
        state.assertRefreshError()
    }

    @Test
    fun `assertRefreshError fails when the refresh direction is Idle`() {
        val state = PagingState<Int, String>()
        expectFailure { state.assertRefreshError() }
    }

    // --- assertAppendError ---

    @Test
    fun `assertAppendError passes when the append direction is Error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(append = PageState.Error(PagingError.Source("err"))),
        )
        state.assertAppendError()
    }

    @Test
    fun `assertAppendError fails when the append direction is Idle`() {
        val state = PagingState<Int, String>()
        expectFailure { state.assertAppendError() }
    }

    // --- assertAppendComplete ---

    @Test
    fun `assertAppendComplete passes when the append direction is Complete`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(append = PageState.Complete),
        )
        state.assertAppendComplete()
    }

    @Test
    fun `assertAppendComplete fails when the append direction is Idle`() {
        val state = PagingState<Int, String>()
        expectFailure { state.assertAppendComplete() }
    }

    // --- assertPrependComplete ---

    @Test
    fun `assertPrependComplete passes when the prepend direction is Complete`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(prepend = PageState.Complete),
        )
        state.assertPrependComplete()
    }

    @Test
    fun `assertPrependComplete fails when the prepend direction is Loading`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(prepend = PageState.Loading),
        )
        expectFailure { state.assertPrependComplete() }
    }

    // --- assertPrependError ---

    @Test
    fun `assertPrependError passes when the prepend direction is Error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(prepend = PageState.Error(PagingError.Source("err"))),
        )
        state.assertPrependError()
    }

    @Test
    fun `assertPrependError fails when the prepend direction is Idle`() {
        val state = PagingState<Int, String>()
        expectFailure { state.assertPrependError() }
    }

    // --- Helper ---

    private fun expectFailure(block: () -> Unit) {
        try {
            block()
            fail("Expected assertion to fail but it passed")
        } catch (_: IllegalStateException) {
            // Expected — check() throws IllegalStateException
        }
    }
}
