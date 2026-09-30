package com.yourssu.ssutime.v2.screen.cyber

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.pluginOrNull

object CyberSessionCleaner {
    private const val TAG = "CyberSessionCleaner"

    fun clearCookies() {
        runCatching {
            val clazz = Class.forName("io.github.chlwhdtn03.CyberApiKt")
            val method = clazz.getMethod("getCyberClient")
            val client = method.invoke(null) as? HttpClient ?: return
            val httpCookies = client.pluginOrNull(HttpCookies) ?: return

            val storageField = HttpCookies::class.java.getDeclaredField("storage").apply {
                isAccessible = true
            }
            val storage = storageField.get(httpCookies) as? CookiesStorage
            if (storage != null) {
                runCatching {
                    val containerField = storage.javaClass.getDeclaredField("container").apply {
                        isAccessible = true
                    }
                    val container = containerField.get(storage) as? MutableList<*>
                    container?.clear()
                }
            }

            storageField.set(httpCookies, AcceptAllCookiesStorage())
        }.onFailure {
            runCatching {
                Log.e(TAG, "Failed to clear cyber session cookies", it)
            }
        }
    }
}
