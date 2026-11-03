package com.nool.core.index

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.nool.core.index.db.NoolDatabase
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertTrue

class IndexBenchmarkTest {

    @Test
    fun benchmarkTenThousandNotes() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        NoolDatabase.Schema.create(driver)
        val database = NoolDatabase(driver)
        val indexManager = SqlDelightIndexManager(database)

        val noteCount = 10_000
        val sampleTags = listOf("work", "personal", "finance", "project", "archive", "journal", "ideas")
        val sampleWords = listOf(
            "algorithm", "architecture", "distributed", "knowledge", "obsidian",
            "database", "performance", "markdown", "coroutine", "compiler",
            "security", "privacy", "asynchronous", "indexing", "benchmark"
        )

        // 1. Full 10,000-note Indexing Benchmark
        val insertDurationMs = measureTimeMillis {
            driver.execute(null, "BEGIN TRANSACTION", 0)
            for (i in 1..noteCount) {
                val tag1 = sampleTags[i % sampleTags.size]
                val tag2 = sampleTags[(i * 3) % sampleTags.size]
                val word1 = sampleWords[i % sampleWords.size]
                val word2 = sampleWords[(i * 7) % sampleWords.size]
                val content = "Note #$i content discussing $word1 and $word2 in detail with #$tag1 and #$tag2 tags."
                
                indexManager.indexNoteContent(
                    relativePath = "Notes/Folder${i % 50}/Note_$i.md",
                    content = content,
                    lastModified = 1700000000L + i,
                    size = content.length.toLong()
                )
            }
            driver.execute(null, "COMMIT", 0)
        }

        println("BENCHMARK: 10,000 notes full reindex completed in ${insertDurationMs}ms (${insertDurationMs.toDouble() / noteCount} ms/note)")
        assertTrue(insertDurationMs > 0)

        // 2. Single-Note Incremental Update Benchmark
        val singleUpdateNanos = measureNanoTime {
            indexManager.indexNoteContent(
                relativePath = "Notes/Folder10/Note_500.md",
                content = "Updated content discussing quantum computation and advanced cryptography.",
                lastModified = 1700009999L,
                size = 75L
            )
        }
        val singleUpdateMs = singleUpdateNanos / 1_000_000.0
        println("BENCHMARK: Single-note incremental update: ${singleUpdateMs}ms")
        assertTrue(singleUpdateMs < 100.0) // Must not block main thread (<100ms)

        // 3. Search p50 / p95 Benchmark
        val latencies = mutableListOf<Double>()
        for (q in 0 until 100) {
            val queryWord = sampleWords[q % sampleWords.size]
            val query = "$queryWord*"
            val nano = measureNanoTime {
                val results = indexManager.search(query)
                assertTrue(results.isNotEmpty())
            }
            latencies.add(nano / 1_000_000.0)
        }

        latencies.sort()
        val p50 = latencies[latencies.size / 2]
        val p95 = latencies[(latencies.size * 95) / 100]

        println("BENCHMARK: Search over 10,000 notes -> p50: ${p50}ms, p95: ${p95}ms")
        assertTrue(p50 < 50.0)
        assertTrue(p95 < 100.0)
    }
}
