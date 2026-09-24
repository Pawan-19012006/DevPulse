package com.devpulse.ai.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class GitHubPaginatorTest {

    @Test
    fun fetchAll_singlePage_stopsWhenLessThanPageSize() = runTest {
        val page1 = (1..50).map { "item_$it" }

        val result = GitHubPaginator.fetchAll(
            startPage = 1,
            maxPages = 5,
            expectedPageSize = 100
        ) { page ->
            if (page == 1) page1 else emptyList()
        }

        assertEquals(50, result.size)
        assertEquals("item_1", result.first())
        assertEquals("item_50", result.last())
    }

    @Test
    fun fetchAll_multiplePages_aggregatesAcrossPagesUntilEmpty() = runTest {
        val page1 = (1..100).map { "item_$it" }
        val page2 = (101..130).map { "item_$it" }

        var requestedPages = 0
        val result = GitHubPaginator.fetchAll(
            startPage = 1,
            maxPages = 5,
            expectedPageSize = 100
        ) { page ->
            requestedPages++
            when (page) {
                1 -> page1
                2 -> page2
                else -> emptyList()
            }
        }

        assertEquals(130, result.size)
        assertEquals(2, requestedPages) // Page 2 had 30 items (< 100), stopped immediately
        assertEquals("item_130", result.last())
    }

    @Test
    fun fetchAll_respectsMaxPagesLimit() = runTest {
        val result = GitHubPaginator.fetchAll(
            startPage = 1,
            maxPages = 3,
            expectedPageSize = 100
        ) { page ->
            (1..100).map { "item_p${page}_$it" }
        }

        // Even though each page returned 100 items, it stops after 3 pages (300 items)
        assertEquals(300, result.size)
    }
}
