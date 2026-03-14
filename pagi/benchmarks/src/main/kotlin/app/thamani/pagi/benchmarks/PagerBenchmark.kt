package app.thamani.pagi.benchmarks

import app.thamani.pagi.Append
import app.thamani.pagi.LoadParams
import app.thamani.pagi.PageSource
import app.thamani.pagi.PageSourceResult
import app.thamani.pagi.Pager
import app.thamani.pagi.PagerConfig
import app.thamani.pagi.Refresh
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTime

/**
 * Benchmarks for Pagi — measures paging performance at different scales.
 *
 * Run via: `./gradlew :pagi:benchmarks:run`
 *
 * Measures:
 * - **Refresh**: Time to load the initial page
 * - **Full scroll**: Time to load all pages by simulating sequential appends
 * - **Page flattening**: Time spent flattening pages into a single items list
 * - **Memory**: Item count and page count after full scroll
 *
 * Scale tiers: 10 → 100 → 1,000 → 10,000 total items
 */

private const val PAGE_SIZE = 20
private const val WARMUP_RUNS = 3
private const val MEASURED_RUNS = 5

data class BenchmarkResult(
    val tier: String,
    val totalItems: Int,
    val refreshTime: Duration,
    val fullScrollTime: Duration,
    val flattenTime: Duration,
    val pageCount: Int,
    val itemCount: Int,
)

/**
 * Creates a [PageSource] that serves [totalItems] items split across pages of
 * [pageSize]. Each item is a simple string like "item-0", "item-1", etc.
 * Keys are 0-based page indices (null for the first page).
 */
fun createBenchmarkSource(totalItems: Int, pageSize: Int): PageSource<Int, String> {
    val totalPages = (totalItems + pageSize - 1) / pageSize
    val allItems = List(totalItems) { "item-$it" }

    return PageSource { params ->
        val pageIndex = params.key ?: 0
        val start = pageIndex * pageSize
        val end = minOf(start + pageSize, totalItems)

        if (start >= totalItems) {
            return@PageSource PageSourceResult.Success(
                items = emptyList(),
                prevKey = if (pageIndex > 0) pageIndex - 1 else null,
                nextKey = null,
            )
        }

        PageSourceResult.Success(
            items = allItems.subList(start, end),
            prevKey = if (pageIndex > 0) pageIndex - 1 else null,
            nextKey = if (end < totalItems) pageIndex + 1 else null,
        )
    }
}

/**
 * Runs a single benchmark for the given [totalItems] count.
 * Returns the median result across [MEASURED_RUNS] iterations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun runBenchmark(totalItems: Int): BenchmarkResult {
    val tierName = formatTier(totalItems)
    val timeSource = TimeSource.Monotonic

    // Warmup
    repeat(WARMUP_RUNS) {
        runTest {
            val source = createBenchmarkSource(totalItems, PAGE_SIZE)
            val pager = Pager(
                config = PagerConfig(pageSize = PAGE_SIZE, prefetchDistance = 0),
                source = source,
                scope = this,
            )
            pager.refresh().join()
            scrollToEnd(pager, totalItems)
            advanceUntilIdle()
        }
    }

    // Measured runs
    val results = mutableListOf<BenchmarkResult>()

    repeat(MEASURED_RUNS) {
        runTest {
            val source = createBenchmarkSource(totalItems, PAGE_SIZE)
            val pager = Pager(
                config = PagerConfig(pageSize = PAGE_SIZE, prefetchDistance = 0),
                source = source,
                scope = this,
            )

            // Measure refresh
            val refreshTime = measureTime {
                pager.refresh().join()
            }

            // Measure full scroll (all remaining pages via append)
            val fullScrollTime = measureTime {
                scrollToEnd(pager, totalItems)
                advanceUntilIdle()
            }

            // Measure flatten (already done internally, but measure standalone)
            val pages = pager.state.value.pages
            val flattenTime = measureTime {
                repeat(100) {
                    buildList {
                        pages.forEach { addAll(it.items) }
                    }
                }
            } / 100 // Average per flatten

            val state = pager.state.value
            results.add(
                BenchmarkResult(
                    tier = tierName,
                    totalItems = totalItems,
                    refreshTime = refreshTime,
                    fullScrollTime = fullScrollTime,
                    flattenTime = flattenTime,
                    pageCount = state.pages.size,
                    itemCount = state.items.size,
                ),
            )
        }
    }

    // Return median by full scroll time
    return results.sortedBy { it.fullScrollTime }[results.size / 2]
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun kotlinx.coroutines.test.TestScope.scrollToEnd(pager: Pager<Int, String>, totalItems: Int) {
    val totalPages = (totalItems + PAGE_SIZE - 1) / PAGE_SIZE
    // Simulate user scrolling through each page boundary
    for (page in 1 until totalPages) {
        val lastItemIndex = pager.state.value.items.size - 1
        if (lastItemIndex >= 0) {
            pager.onItemAccessed(lastItemIndex)
            advanceUntilIdle()
        }
    }
}

private fun formatTier(count: Int): String = when {
    count >= 10_000 -> "${count / 1_000}k"
    count >= 1_000 -> "${count / 1_000}k"
    else -> count.toString()
}

fun main() {
    val tiers = listOf(10, 100, 1_000, 10_000)

    println()
    println("╔══════════════════════════════════════════════════════════════════════════════╗")
    println("║                          PAGI BENCHMARK RESULTS                             ║")
    println("╠══════════════════════════════════════════════════════════════════════════════╣")
    println("║  Config: pageSize=$PAGE_SIZE | warmup=$WARMUP_RUNS | measured=$MEASURED_RUNS (median reported)        ║")
    println("╚══════════════════════════════════════════════════════════════════════════════╝")
    println()
    println(
        "%-8s │ %8s │ %12s │ %12s │ %12s │ %6s │ %6s".format(
            "Tier", "Items", "Refresh", "Full Scroll", "Flatten", "Pages", "Items",
        ),
    )
    println("─".repeat(82))

    for (tier in tiers) {
        val result = runBenchmark(tier)
        println(
            "%-8s │ %8d │ %12s │ %12s │ %12s │ %6d │ %6d".format(
                result.tier,
                result.totalItems,
                result.refreshTime.toString(),
                result.fullScrollTime.toString(),
                result.flattenTime.toString(),
                result.pageCount,
                result.itemCount,
            ),
        )
    }

    println("─".repeat(82))
    println()
    println("Legend:")
    println("  Refresh     — Time to load the initial page (first pageSize items)")
    println("  Full Scroll — Time to load ALL pages by simulating sequential appends")
    println("  Flatten     — Average time to flatten all pages into a single list")
    println("  Pages       — Total number of loaded pages after full scroll")
    println("  Items       — Total item count after full scroll")
    println()
}
