package com.yourssu.ssutime.v2.screen.main

import io.github.chlwhdtn03.data.Lms.Term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class LmsCurrentTermTest {
    @Test
    fun currentTermAt_selectsMatchingTermWhenUpcomingTermIsFirst() {
        val upcomingTerm = term(
            id = 2,
            startAt = "2026-09-01T00:00:00Z",
            endAt = "2027-02-28T23:59:59Z",
        )
        val activeTerm = term(
            id = 1,
            startAt = "2026-03-02T15:00:00Z",
            endAt = "2026-08-31T14:59:59Z",
        )

        val currentTerm = listOf(upcomingTerm, activeTerm)
            .currentTermAt(Instant.parse("2026-06-07T00:00:00Z"))

        assertEquals(activeTerm, currentTerm)
    }

    @Test
    fun currentTermAt_includesStartAndEndTimes() {
        val activeTerm = term(
            id = 1,
            startAt = "2026-03-02T15:00:00Z",
            endAt = "2026-08-31T14:59:59Z",
        )

        assertEquals(
            activeTerm,
            listOf(activeTerm).currentTermAt(Instant.parse("2026-03-02T15:00:00Z")),
        )
        assertEquals(
            activeTerm,
            listOf(activeTerm).currentTermAt(Instant.parse("2026-08-31T14:59:59Z")),
        )
    }

    @Test
    fun currentTermAt_returnsNullWhenNoTermContainsNow() {
        val pastTerm = term(
            id = 1,
            startAt = "2026-03-02T15:00:00Z",
            endAt = "2026-08-31T14:59:59Z",
        )

        assertNull(listOf(pastTerm).currentTermAt(Instant.parse("2026-09-01T00:00:00Z")))
    }

    private fun term(
        id: Int,
        startAt: String,
        endAt: String,
    ): Term =
        Term(
            id = id,
            name = "term-$id",
            start_at = Instant.parse(startAt),
            end_at = Instant.parse(endAt),
        )

    @Test
    fun printLmsPropertyNullability() {
        val classes = listOf(
            io.github.chlwhdtn03.data.Lms.ScholarshipHistoryCell::class,
            io.github.chlwhdtn03.data.Lms.TuitionCell::class,
            io.github.chlwhdtn03.data.Lms.GraduateTableCell::class
        )
        classes.forEach { clazz ->
            println("=== Class: ${clazz.qualifiedName} ===")
            clazz.members.forEach { member ->
                if (member is kotlin.reflect.KProperty) {
                    println("  Property: ${member.name}, Nullable: ${member.returnType.isMarkedNullable}")
                }
            }
        }
    }
}
