package com.devpulse.ai.network

/**
 * Reusable paginator for GitHub API endpoints.
 * Handles fetching pages sequentially until an empty page is returned or the
 * configured maxPages limit is reached, safely avoiding runaway network requests.
 */
object GitHubPaginator {

    suspend fun <T> fetchAll(
        startPage: Int = 1,
        maxPages: Int = 5,
        expectedPageSize: Int = 100,
        fetchPage: suspend (page: Int) -> List<T>
    ): List<T> {
        val results = mutableListOf<T>()
        var currentPage = startPage

        while (currentPage < startPage + maxPages) {
            val pageItems = try {
                fetchPage(currentPage)
            } catch (e: Exception) {
                if (results.isNotEmpty()) break else throw e
            }

            if (pageItems.isEmpty()) {
                break
            }

            results.addAll(pageItems)

            // If the returned page has fewer items than expectedPageSize, this is the last page
            if (pageItems.size < expectedPageSize) {
                break
            }

            currentPage++
        }

        return results
    }
}
