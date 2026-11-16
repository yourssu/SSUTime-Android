package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.data.LabsData
import com.yourssu.ssutime.v2.datastore.JsonDataStoreSerializer

val Context.labsDataStore: DataStore<LabsData> by dataStore(
    fileName = "labs.json",
    serializer = LabsDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { LabsDataSerializer.defaultValue },
)

object LabsDataSerializer : JsonDataStoreSerializer<LabsData>(
    defaultValue = LabsData(),
    kSerializer = LabsData.serializer(),
    readErrorMessage = "실험실 정보를 읽어오지 못했습니다.",
)
