package com.yourssu.ssutime.desktop

import com.yourssu.data.*
import com.yourssu.data.network.*
import com.yourssu.ssutime.desktop.core.lms.LmsAppService
import com.yourssu.ssutime.desktop.core.network.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopApiParityTest {
    private val todo = TodoInfo(todoId = 10, title = "Assignment", due_date = "2027-01-01T00:00:00Z", type = TodoType.ASSIGNMENT,
        subject = SubjectInfo(1, "Course", "Teacher"), description = "description")
    private val headers = headersOf(HttpHeaders.ContentType, "application/json")
    private val summaryJson = """[{"todo":{"subjectId":1,"materialCode":10,"type":"ASSIGNMENT","status":"PROVISIONAL","aiSummary":"Summary","attachmentLinks":[{"url":"https://example.com/file.pdf","fileName":"file.pdf"}]}}]"""

    @Test fun expiredSummaryTokenIsRefreshedOnceAndProvisionalTextIsShown() = runBlocking {
        var attempts = 0
        var refreshes = 0
        val client = HttpClient(MockEngine { request ->
            attempts++
            if (request.headers[HttpHeaders.Authorization] == "Bearer old") respond("{}", HttpStatusCode.Unauthorized, headers)
            else { assertEquals("Bearer new", request.headers[HttpHeaders.Authorization]); respond(summaryJson, HttpStatusCode.OK, headers) }
        }) { install(ContentNegotiation) { json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true }) } }
        try {
            val summary = LmsAppService(SsuTimeApi(client)).loadAiSummary("old", todo, onRefreshToken = { refreshes++; "new" })
            assertEquals("Summary", summary?.summary)
            assertEquals("file.pdf", summary?.attachmentLinks?.single()?.fileName)
            assertEquals(2, attempts)
            assertEquals(1, refreshes)
        } finally { client.close() }
    }

    @Test fun failedSummaryResponseRetainsHttpStatusForRetry() = runBlocking {
        val client = HttpClient(MockEngine { respond("{}", HttpStatusCode.Forbidden, headers) })
        try { assertEquals(403, assertFailsWith<DesktopApiException> { SsuTimeApi(client).findAiSummary("token", todo) }.status.value) }
        finally { client.close() }
    }

    @Test fun withdrawalUsesAuthenticatedPostAndNeverReportsSuccessForFailure() = runBlocking {
        val client = HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/auth/withdrawal-requests", request.url.encodedPath)
            assertEquals("Bearer token", request.headers[HttpHeaders.Authorization])
            respond("{}", HttpStatusCode.InternalServerError, headers)
        }) { install(ContentNegotiation) { json() } }
        try { assertEquals(500, SsuTimeApi(client).requestAccountWithdrawal("token").value) }
        finally { client.close() }
    }

    @Test fun emptySummaryIsHiddenAndNonemptySummaryPreservesAttachmentLinks() {
        assertNull(ReportedTodoResponse(status = "CONFIRMED", aiSummary = " ").toAiSummaryOrNull())
        val summary = ReportedTodoResponse(status = "PROVISIONAL", aiSummary = "Ready", attachmentLinks = listOf(AttachmentLinkResponse("https://example.com", "file"))).toAiSummaryOrNull()
        assertEquals("Ready", summary?.summary)
        assertEquals("file", summary?.attachmentLinks?.single()?.fileName)
    }
}
