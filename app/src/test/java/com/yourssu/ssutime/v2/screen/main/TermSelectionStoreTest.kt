package com.yourssu.ssutime.v2.screen.main

import io.github.chlwhdtn03.data.Lms.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class TermSelectionStoreTest {
    @Test
    fun selectedTerm_isInMemoryAndCanReturnToCurrentTermBehavior() {
        val store = TermSelectionStore()
        val term = Term(
            id = 1,
            name = "2026-1",
            start_at = Instant.parse("2026-03-02T15:00:00Z"),
            end_at = Instant.parse("2026-08-31T14:59:59Z"),
        )

        assertNull(store.selectedTerm.value)

        store.select(term)
        assertEquals(term, store.selectedTerm.value)

        store.clear()
        assertNull(store.selectedTerm.value)
    }
}
