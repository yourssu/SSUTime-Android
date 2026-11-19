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
import java.util.Locale

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
            setChronometer(R.id.widget_live_countdown, base, null, true)
        } else {
            // Stop recycled launcher Chronometer ticks before writing static fallback text.
            setBoolean(R.id.widget_live_countdown, "setCountDown", false)
            setChronometer(R.id.widget_live_countdown, SystemClock.elapsedRealtime(), null, false)
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
internal fun StaticCountdownText(
    text: String,
    fontSizeSp: Int,
    color: LiveCountdownColor,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val remoteViews = RemoteViews(
        context.packageName,
        when (color) {
            LiveCountdownColor.Accent -> R.layout.widget_static_countdown_accent
            LiveCountdownColor.White -> R.layout.widget_static_countdown_white
        },
    ).apply {
        setTextViewTextSize(
            R.id.widget_static_countdown,
            TypedValue.COMPLEX_UNIT_SP,
            fontSizeSp.toFloat(),
        )
        setTextViewText(R.id.widget_static_countdown, text)
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

internal fun Long.toLiveCountdownFallbackText(): String {
    val safeSeconds = coerceAtLeast(0L)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
