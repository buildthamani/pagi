package app.thamani.pagi

/**
 * Determines the order in which [CachedPageSource] checks cache vs network.
 *
 * - [CacheFirst] — check cache, fall back to network on miss. Best for
 *   reducing network calls when data doesn't change often.
 *
 * - [NetworkFirst] — always hit the network, fall back to cache on error.
 *   Best for data that changes frequently but should still work offline.
 *
 * Both strategies clear the cache on refresh and never cache errors.
 */
sealed interface FetchStrategy

/**
 * Check cache first. On miss, fetch from network and cache the result.
 *
 * ```
 * cache.get(key)
 *   ├── hit → return cached
 *   └── miss → network.load()
 *        ├── success → cache.put(), return
 *        └── error → return error
 * ```
 */
data object CacheFirst : FetchStrategy

/**
 * Fetch from network first. On error, fall back to cache.
 *
 * ```
 * network.load()
 *   ├── success → cache.put(), return
 *   └── error → cache.get(key)
 *        ├── hit → return cached (stale but available)
 *        └── miss → return original error
 * ```
 */
data object NetworkFirst : FetchStrategy
