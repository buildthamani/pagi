package app.thamani.pagi.testing

import app.thamani.pagi.PageState
import app.thamani.pagi.PagingState

/** Asserts [PagingState.items] has exactly [expected] items. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertItemCount(expected: Int) {
    check(items.size == expected) {
        "Expected $expected items, got ${items.size}. Items: $items"
    }
}

/** Asserts [PagingState.items] equals [expected] exactly. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertItems(expected: List<Value>) {
    check(items == expected) {
        "Expected items $expected, got $items"
    }
}

/** Asserts no direction is in an error state. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertNoErrors() {
    val errors =
        buildList {
            if (pageStates.refresh is PageState.Error) add("refresh: ${(pageStates.refresh as PageState.Error).error.message}")
            if (pageStates.prepend is PageState.Error) add("prepend: ${(pageStates.prepend as PageState.Error).error.message}")
            if (pageStates.append is PageState.Error) add("append: ${(pageStates.append as PageState.Error).error.message}")
        }
    check(errors.isEmpty()) {
        "Expected no errors, found: ${errors.joinToString()}"
    }
}

/** Asserts the refresh direction is [PageState.Loading]. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertRefreshLoading() {
    check(pageStates.refresh is PageState.Loading) {
        "Expected refresh to be Loading, got ${pageStates.refresh}"
    }
}

/** Asserts the refresh direction is [PageState.Error]. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertRefreshError() {
    check(pageStates.refresh is PageState.Error) {
        "Expected refresh to be Error, got ${pageStates.refresh}"
    }
}

/** Asserts the append direction is [PageState.Error]. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertAppendError() {
    check(pageStates.append is PageState.Error) {
        "Expected append to be Error, got ${pageStates.append}"
    }
}

/** Asserts the append direction is [PageState.Complete]. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertAppendComplete() {
    check(pageStates.append is PageState.Complete) {
        "Expected append to be Complete, got ${pageStates.append}"
    }
}

/** Asserts the prepend direction is [PageState.Complete]. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertPrependComplete() {
    check(pageStates.prepend is PageState.Complete) {
        "Expected prepend to be Complete, got ${pageStates.prepend}"
    }
}

/** Asserts the prepend direction is [PageState.Error]. */
fun <Key : Any, Value : Any> PagingState<Key, Value>.assertPrependError() {
    check(pageStates.prepend is PageState.Error) {
        "Expected prepend to be Error, got ${pageStates.prepend}"
    }
}
