package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import org.junit.Test

class PageStatesTest {

    @Test
    fun `default PageStates has all directions Idle`() {
        val states = PageStates()

        assertEquals(PageState.Idle, states.refresh)
        assertEquals(PageState.Idle, states.prepend)
        assertEquals(PageState.Idle, states.append)
    }

    @Test
    fun `identical PageStates are equal`() {
        val a = PageStates(refresh = PageState.Loading, append = PageState.Idle)
        val b = PageStates(refresh = PageState.Loading, append = PageState.Idle)

        assertEquals(a, b)
    }

    @Test
    fun `copy updates only specified direction`() {
        val original = PageStates()
        val updated = original.copy(append = PageState.Loading)

        assertEquals(PageState.Idle, updated.refresh)
        assertEquals(PageState.Idle, updated.prepend)
        assertEquals(PageState.Loading, updated.append)
    }
}
