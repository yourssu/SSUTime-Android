package com.yourssu.ssutime.v2.analytics

import com.yourssu.ssutime.v2.BuildConfig
import io.sentry.Sentry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

object SentryExceptionReporter {
    fun capture(exception: Throwable) {
        val isRegularCancellation =
            exception is CancellationException && exception !is TimeoutCancellationException

        if (!BuildConfig.DEBUG_MODE && !isRegularCancellation) {
            Sentry.captureException(exception)
        }
    }
}
