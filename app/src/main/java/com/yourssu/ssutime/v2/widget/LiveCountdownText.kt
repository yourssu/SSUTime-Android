package com.yourssu.ssutime.v2.widget

import android.os.SystemClock
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import com.yourssu.ssutime.v2.R

internal enum class LiveCountdownColor {
    Accent,
    White,
}

@Composable
@GlanceComposable
internal fun LiveCountdownText(
    targetEpochMillis: Long?,
    fallbackText: String,
    fontSizeSp: Int,
    color: LiveCountdownColor,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val nowEpochMillis = System.currentTimeMillis()
    val remainingMillis = targetEpochMillis
        ?.minus(nowEpochMillis)
        ?.coerceAtLeast(0L)
        ?: 0L
    val remoteViews = RemoteViews(
        context.packageName,
        when (color) {
            LiveCountdownColor.Accent -> R.layout.widget_live_countdown_accent
            LiveCountdownColor.White -> R.layout.widget_live_countdown_white
        },
    ).apply {
        setTextViewTextSize(
            R.id.widget_live_countdown,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp.toFloat(),
        )

        if (targetEpochMillis != null && remainingMillis > 0L) {
            val base = SystemClock.elapsedRealtime() + remainingMillis
            setBoolean(R.id.widget_live_countdown, "setCountDown", true)
            setChronometer(R.id.widget_live_countdown, base, chronometerFormat(remainingMillis), true)
        } else {
            setTextViewText(R.id.widget_live_countdown, fallbackText)
        }
    }

    AndroidRemoteViews(
        remoteViews = remoteViews,
        modifier = modifier,
    )
}

@Composable
@GlanceComposable
internal fun LateBadge(
    text: String,
    fontSizeSp: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val remoteViews = RemoteViews(
        context.packageName,
        R.layout.widget_late_badge,
    ).apply {
        setTextViewText(R.id.widget_late_badge, text)
        setTextViewTextSize(
            R.id.widget_late_badge,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp.toFloat(),
        )
    }

    AndroidRemoteViews(
        remoteViews = remoteViews,
        modifier = modifier,
    )
}

private fun chronometerFormat(remainingMillis: Long): String? =
    when (remainingMillis / HOUR_MILLIS) {
        0L -> "00:%s"
        in 1L..9L -> "0%s"
        else -> null
    }

private const val HOUR_MILLIS = 60 * 60 * 1000L
