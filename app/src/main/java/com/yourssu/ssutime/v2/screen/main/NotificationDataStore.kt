package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.data.AlertData
import com.yourssu.ssutime.v2.datastore.JsonDataStoreSerializer

val Context.notificationStore: DataStore<AlertData> by dataStore(
    fileName = "notification.json",
    serializer = AlertDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AlertDataSerializer.defaultValue },
)

object AlertDataSerializer : JsonDataStoreSerializer<AlertData>(
    defaultValue = AlertData(
        valid = false,
        allowSystemAlert = false,
        allowCallAlert = false,
        callingAlertThresholdMinutes = 60,
        lastCallAlertRemindPeriod = null,
    ),
    kSerializer = AlertData.serializer(),
    readErrorMessage = "알림 정보를 읽어오지 못했습니다.",
)
