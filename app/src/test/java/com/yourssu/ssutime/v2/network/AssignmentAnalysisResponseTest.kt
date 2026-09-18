package com.yourssu.ssutime.v2.network

import com.yourssu.data.network.AssignmentAnalysisResponse
import com.yourssu.data.network.hasNoAnalyzableAttachment
import com.yourssu.data.network.isSuccessful
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssignmentAnalysisResponseTest {
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun noAnalyzableAttachmentErrorIsDecodedCorrectly() {
        val errorJson = """{"error":"No analyzable attachment"}"""
        val response = json.decodeFromString<AssignmentAnalysisResponse>(errorJson)

        assertEquals("No analyzable attachment", response.error)
        assertTrue(response.hasNoAnalyzableAttachment)
        assertFalse(response.isSuccessful)
    }

    @Test
    fun successResponseIsDecodedCorrectly() {
        val successJson = """
            {
                "analysisId": 101,
                "status": "PROVISIONAL",
                "skippedFiles": ["sample.zip"]
            }
        """.trimIndent()
        val response = json.decodeFromString<AssignmentAnalysisResponse>(successJson)

        assertEquals(101L, response.analysisId)
        assertEquals("PROVISIONAL", response.status)
        assertEquals(listOf("sample.zip"), response.skippedFiles)
        assertFalse(response.hasNoAnalyzableAttachment)
        assertTrue(response.isSuccessful)
    }

    @Test
    fun unknownKeysAreIgnoredWithoutException() {
        val jsonWithUnknownKeys = """
            {
                "analysisId": 202,
                "status": "CONFIRMED",
                "extraField": "ignoredValue",
                "nested": { "foo": "bar" }
            }
        """.trimIndent()
        val response = json.decodeFromString<AssignmentAnalysisResponse>(jsonWithUnknownKeys)

        assertEquals(202L, response.analysisId)
        assertEquals("CONFIRMED", response.status)
        assertTrue(response.isSuccessful)
    }
}
