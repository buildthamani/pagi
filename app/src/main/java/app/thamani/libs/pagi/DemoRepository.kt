package app.thamani.libs.pagi

import app.thamani.pagi.Pager
import app.thamani.pagi.PagerConfig
import kotlinx.coroutines.CoroutineScope

class DemoRepository {

    fun getItems(scope: CoroutineScope): Pager<Int, DemoItem> {
        return Pager(
            config = PagerConfig(pageSize = 20),
            source = DemoPageSource(),
            scope = scope,
        )
    }
}
