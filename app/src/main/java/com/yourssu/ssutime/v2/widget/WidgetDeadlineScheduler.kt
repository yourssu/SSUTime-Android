package com.yourssu.ssutime.v2.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

private const val TAG = "WidgetDeadlineScheduler"
private const val REQUEST_CODE_WIDGET_DEADLINE = 95_000

internal fun scheduleWidgetDeadlineUpdate(context: Context, deadlineInstant: Instant) {
    val appContext = context.applicationContext
    val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
    val triggerAtMillis = deadlineInstant.toEpochMilli() + 1_000L // 마감 1초 후 갱신
    if (triggerAtMillis <= System.currentTimeMillis()) {
        return
    }

    val intent = Intent(appContext, WidgetDeadlineReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        appContext,
        REQUEST_CODE_WIDGET_DEADLINE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
        Log.i(TAG, "위젯 마감 갱신 알람 예약 완료: triggerAt=${Instant.ofEpochMilli(triggerAtMillis)}")
    } catch (e: SecurityException) {
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
        Log.w(TAG, "정확한 알람 권한이 없어 일반 알람으로 위젯 갱신 예약", e)
    } catch (e: Exception) {
        Log.e(TAG, "위젯 마감 갱신 알람 예약 실패", e)
    }
}

class WidgetDeadlineReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        Log.i(TAG, "위젯 마감 갱신 알람 수신. 위젯 전체를 갱신합니다.")
        val appContext = context.applicationContext
        scope.launch {
            runCatching {
                updateAllTodoWidgets(appContext)
            }.onFailure { e ->
                Log.e(TAG, "위젯 갱신 중 오류 발생", e)
            }
        }
    }
}
