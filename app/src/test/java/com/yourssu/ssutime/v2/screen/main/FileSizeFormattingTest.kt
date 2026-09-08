package com.yourssu.ssutime.v2.screen.main

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormattingTest {
    @Test
    fun `formats zero and negative sizes to 0B`() {
        assertEquals("0B", 0L.toSimply())
        assertEquals("0B", (-100L).toSimply())
    }

    @Test
    fun `formats bytes correctly`() {
        assertEquals("500B", 500L.toSimply())
        assertEquals("1023B", 1023L.toSimply())
    }

    @Test
    fun `formats kilobytes correctly`() {
        assertEquals("1KB", 1024L.toSimply())
        assertEquals("235.3KB", 240947L.toSimply())
    }

    @Test
    fun `formats megabytes correctly`() {
        assertEquals("1MB", (1024L * 1024L).toSimply())
        assertEquals("21.3MB", 22334668L.toSimply())
        assertEquals("200MB", (200L * 1024L * 1024L).toSimply())
    }

    @Test
    fun `formats gigabytes correctly`() {
        assertEquals("1GB", (1024L * 1024L * 1024L).toSimply())
        val onePointFiveGb = (1.5 * 1024 * 1024 * 1024).toLong()
        assertEquals("1.5GB", onePointFiveGb.toSimply())
    }
}
