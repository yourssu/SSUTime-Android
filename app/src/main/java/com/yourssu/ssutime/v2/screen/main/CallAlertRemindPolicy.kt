package com.yourssu.ssutime.v2.screen.main

import com.yourssu.data.AlertData
import java.time.LocalDate
import java.time.ZoneId

val CALL_ALERT_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

/**
 * 매년 3~6월(1학기: "YYYY-1"), 9~12월(2학기: "YYYY-2")의 학기 식별자를 반환합니다.
 * 1, 2, 7, 8월(방학 기간)에는 null을 반환합니다.
 */
fun callAlertRemindPeriod(
    localDate: LocalDate = LocalDate.now(CALL_ALERT_ZONE_ID),
): String? {
    return when (localDate.monthValue) {
        in 3..6 -> "${localDate.year}-1"
        in 9..12 -> "${localDate.year}-2"
        else -> null
    }
}

/**
 * CallingAlertBottomSheet 노출 필요 여부를 반환합니다.
 * - 최초 진입(!alertData.valid): true
 * - 학기 리마인드: valid == true && !allowCallAlert && currentPeriod != null && lastCallAlertRemindPeriod != currentPeriod
 */
fun shouldShowCallingAlertBottomSheet(
    alertData: AlertData,
    currentPeriod: String? = callAlertRemindPeriod(),
): Boolean {
    if (!alertData.valid) {
        return true
    }
    if (alertData.allowCallAlert) {
        return false
    }
    if (currentPeriod == null) {
        return false
    }
    return alertData.lastCallAlertRemindPeriod != currentPeriod
}
