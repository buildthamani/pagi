package app.thamani.pagi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PagingStateTest {

    // --- Default state ---

    @Test
    fun `default state has empty items`() {
        val state = PagingState<Int, String>()
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `default state has all Idle page states`() {
        val state = PagingState<Int, String>()
        assertEquals(PageState.Idle, state.pageStates.refresh)
        assertEquals(PageState.Idle, state.pageStates.prepend)
        assertEquals(PageState.Idle, state.pageStates.append)
    }

    @Test
    fun `default state has no pages`() {
        val state = PagingState<Int, String>()
        assertTrue(state.pages.isEmpty())
    }

    // --- isInitialLoading ---

    @Test
    fun `isInitialLoading is true when empty and refresh is Loading`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(refresh = PageState.Loading),
        )
        assertTrue(state.isInitialLoading)
    }

    @Test
    fun `isInitialLoading is false when items exist even if refresh is Loading`() {
        val state = PagingState<Int, String>(
            items = listOf("a"),
            pageStates = PageStates(refresh = PageState.Loading),
        )
        assertFalse(state.isInitialLoading)
    }

    @Test
    fun `isInitialLoading is false when empty and refresh is Idle`() {
        val state = PagingState<Int, String>()
        assertFalse(state.isInitialLoading)
    }

    // --- isInitialError ---

    @Test
    fun `isInitialError is true when empty and refresh is Error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(
                refresh = PageState.Error(PagingError.Source("failed")),
            ),
        )
        assertTrue(state.isInitialError)
    }

    @Test
    fun `isInitialError is false when items exist even if refresh is Error`() {
        val state = PagingState<Int, String>(
            items = listOf("a"),
            pageStates = PageStates(
                refresh = PageState.Error(PagingError.Source("failed")),
            ),
        )
        assertFalse(state.isInitialError)
    }

    @Test
    fun `isInitialError is false when empty and refresh is Loading`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(refresh = PageState.Loading),
        )
        assertFalse(state.isInitialError)
    }

    // --- hasItems ---

    @Test
    fun `hasItems is true when items exist`() {
        val state = PagingState<Int, String>(items = listOf("x", "y"))
        assertTrue(state.hasItems)
    }

    @Test
    fun `hasItems is false when items is empty`() {
        val state = PagingState<Int, String>()
        assertFalse(state.hasItems)
    }

    // --- isEmpty ---

    @Test
    fun `isEmpty is true when no items and refresh is Idle`() {
        val state = PagingState<Int, String>()
        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEmpty is true when no items and refresh is Error`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(
                refresh = PageState.Error(PagingError.Source("err")),
            ),
        )
        assertTrue(state.isEmpty)
    }

    @Test
    fun `isEmpty is false when refresh is Loading even if no items`() {
        val state = PagingState<Int, String>(
            pageStates = PageStates(refresh = PageState.Loading),
        )
        assertFalse(state.isEmpty)
    }

    @Test
    fun `isEmpty is false when items exist`() {
        val state = PagingState<Int, String>(items = listOf("a"))
        assertFalse(state.isEmpty)
    }

    // --- Data class behavior ---

    @Test
    fun `identical states are equal`() {
        val page = PageSourceResult.Success(
            items = listOf("a"),
            prevKey = null,
            nextKey = 2,
        )
        val a = PagingState(items = listOf("a"), pages = listOf(page))
        val b = PagingState(items = listOf("a"), pages = listOf(page))

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `copy preserves unmodified fields`() {
        val original = PagingState<Int, String>(
            items = listOf("a", "b"),
            pageStates = PageStates(append = PageState.Loading),
        )

        val copied = original.copy(
            pageStates = PageStates(append = PageState.Complete),
        )

        assertEquals(listOf("a", "b"), copied.items)
        assertEquals(PageState.Complete, copied.pageStates.append)
        assertEquals(PageState.Idle, copied.pageStates.refresh)
    }
}
