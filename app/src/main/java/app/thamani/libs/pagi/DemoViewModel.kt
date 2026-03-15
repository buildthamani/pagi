package app.thamani.libs.pagi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.thamani.pagi.PagingState
import kotlinx.coroutines.flow.StateFlow

class DemoViewModel : ViewModel() {
    private val repository = DemoRepository()

    val pager = repository.getItems(viewModelScope)

    val pagingState: StateFlow<PagingState<Int, DemoItem>> = pager.state

    fun refresh() {
        pager.refresh()
    }

    fun retry() {
        pager.retry()
    }
}
