package app.thamani.pagi

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PageSourceTest {

    @Test
    fun `a page source can be implemented as a lambda`() = runTest {
        val source = PageSource<Int, String> { params ->
            PageSourceResult.Success(
                items = listOf("a", "b"),
                prevKey = null,
                nextKey = (params.key ?: 0) + 1,
            )
        }

        val result = source.load(
            LoadParams(key = null, loadSize = 2, direction = Refresh),
        )

        assertTrue(result is PageSourceResult.Success)
        val success = result as PageSourceResult.Success
        assertEquals(listOf("a", "b"), success.items)
        assertEquals(1, success.nextKey)
    }

    @Test
    fun `a page source can return an error result`() = runTest {
        val source = PageSource<Int, String> {
            PageSourceResult.Error(PagingError.Source("something broke"))
        }

        val result = source.load(
            LoadParams(key = null, loadSize = 10, direction = Refresh),
        )

        assertTrue(result is PageSourceResult.Error)
        val error = result as PageSourceResult.Error
        assertEquals("something broke", error.error.message)
    }

    @Test
    fun `a page source can be implemented as a class`() = runTest {
        class TestSource : PageSource<String, Int> {
            override suspend fun load(params: LoadParams<String>): PageSourceResult<String, Int> {
                return PageSourceResult.Success(
                    items = listOf(1, 2, 3),
                    prevKey = null,
                    nextKey = "next-cursor",
                )
            }
        }

        val source = TestSource()
        val result = source.load(
            LoadParams(key = null, loadSize = 3, direction = Refresh),
        )

        assertTrue(result is PageSourceResult.Success)
        val success = result as PageSourceResult.Success
        assertEquals(listOf(1, 2, 3), success.items)
        assertEquals("next-cursor", success.nextKey)
    }

    @Test
    fun `the page source receives the exact LoadParams that were passed in`() = runTest {
        var receivedParams: LoadParams<Int>? = null

        val source = PageSource<Int, String> { params ->
            receivedParams = params
            PageSourceResult.Success(items = emptyList(), prevKey = null, nextKey = null)
        }

        val params = LoadParams(key = 5, loadSize = 20, direction = Append)
        source.load(params)

        assertEquals(params, receivedParams)
    }
}
