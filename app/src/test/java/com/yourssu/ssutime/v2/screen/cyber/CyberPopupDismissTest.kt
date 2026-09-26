package com.yourssu.ssutime.v2.screen.cyber

import com.yourssu.data.CyberLoginData
import com.yourssu.ssutime.v2.screen.navigation.MainTab
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.cookies
import io.ktor.client.plugins.pluginOrNull
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyberPopupDismissTest {

    private fun isBannerVisible(
        currentTab: MainTab,
        cyberLoginData: CyberLoginData,
        showCyberPopup: Boolean = true,
    ): Boolean {
        return currentTab != MainTab.MY &&
            !cyberLoginData.hasCredentials &&
            !cyberLoginData.isBannerDismissed &&
            showCyberPopup
    }

    @Test
    fun defaultCyberLoginData_isBannerDismissedIsFalse() {
        val data = CyberLoginData()
        assertFalse(data.isBannerDismissed)
        assertFalse(data.hasCredentials)
    }

    @Test
    fun bannerVisibility_initialStateOnHomeAndCalendar() {
        val data = CyberLoginData()
        assertTrue(isBannerVisible(MainTab.HOME, data))
        assertTrue(isBannerVisible(MainTab.CALENDAR, data))
        assertFalse(isBannerVisible(MainTab.MY, data))
    }

    @Test
    fun bannerVisibility_afterDismissingBanner() {
        val data = CyberLoginData(isBannerDismissed = true)
        assertFalse(isBannerVisible(MainTab.HOME, data))
        assertFalse(isBannerVisible(MainTab.CALENDAR, data))
        assertFalse(isBannerVisible(MainTab.MY, data))
    }

    @Test
    fun bannerVisibility_whenConnected() {
        val data = CyberLoginData(id = "user", pw = "pass", isConnected = true, isBannerDismissed = true)
        assertFalse(isBannerVisible(MainTab.HOME, data))
        assertFalse(isBannerVisible(MainTab.CALENDAR, data))
    }

    @Test
    fun bannerVisibility_afterDisconnect_remainsHidden() {
        // Disconnecting sets isBannerDismissed = true
        val loggedOutData = CyberLoginData(isBannerDismissed = true)
        assertFalse(loggedOutData.hasCredentials)
        assertTrue(loggedOutData.isBannerDismissed)

        assertFalse(isBannerVisible(MainTab.HOME, loggedOutData))
        assertFalse(isBannerVisible(MainTab.CALENDAR, loggedOutData))
    }

    @Test
    fun serialization_backwardCompatibility_missingFieldDefaultsToFalse() {
        val json = """{"id":"test","pw":"1234","isConnected":true}"""
        val decoded = Json.decodeFromString<CyberLoginData>(json)
        assertEquals("test", decoded.id)
        assertEquals("1234", decoded.pw)
        assertTrue(decoded.isConnected)
        assertFalse(decoded.isBannerDismissed)
    }

    @Test
    fun serialization_roundTripPreservesIsBannerDismissed() {
        val original = CyberLoginData(id = "user", pw = "pass", isConnected = true, isBannerDismissed = true)
        val json = Json.encodeToString(original)
        val decoded = Json.decodeFromString<CyberLoginData>(json)
        assertEquals(original, decoded)
        assertTrue(decoded.isBannerDismissed)
    }

    @Test
    fun clearCyberSessionCookies_clearsStoredCookies() = runBlocking {
        val clazz = Class.forName("io.github.chlwhdtn03.CyberApiKt")
        val client = clazz.getMethod("getCyberClient").invoke(null) as HttpClient
        val httpCookies = client.pluginOrNull(HttpCookies)
        assertNotNull(httpCookies)

        val testUrl = Url("https://portal.kcu.ac")
        val storage = HttpCookies::class.java.getDeclaredField("storage").apply {
            isAccessible = true
        }.get(httpCookies) as CookiesStorage
        storage.addCookie(testUrl, Cookie(name = "JSESSIONID", value = "test_session_id"))

        val beforeCookies = client.cookies(testUrl)
        assertTrue(beforeCookies.any { it.name == "JSESSIONID" })

        CyberSessionCleaner.clearCookies()

        val afterCookies = client.cookies(testUrl)
        assertTrue(afterCookies.isEmpty())
    }
}
