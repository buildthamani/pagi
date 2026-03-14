package app.thamani.pagi

/**
 * The single interface consumers implement to provide paged data.
 *
 * One method, one return type. Implement this with a class for complex sources,
 * or pass a lambda for simple ones:
 *
 * ```
 * val source = PageSource<Int, User> { params ->
 *     val response = api.getUsers(page = params.key ?: 1, limit = params.loadSize)
 *     PageSourceResult.Success(
 *         items = response.users,
 *         prevKey = null,
 *         nextKey = response.nextPage,
 *     )
 * }
 * ```
 *
 * @param Key   the pagination token type (Int for offset, String for cursor, etc.)
 * @param Value the item type
 */
fun interface PageSource<Key : Any, Value : Any> {

    /**
     * Load a page of data.
     *
     * This is a suspend function — do your async work directly here.
     * Return [PageSourceResult.Success] with items and navigation keys,
     * or [PageSourceResult.Error] if something went wrong.
     */
    suspend fun load(params: LoadParams<Key>): PageSourceResult<Key, Value>
}
