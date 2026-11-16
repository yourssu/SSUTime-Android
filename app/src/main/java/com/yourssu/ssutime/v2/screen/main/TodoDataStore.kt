package com.yourssu.ssutime.v2.screen.main

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.data.TodoData
import com.yourssu.ssutime.v2.datastore.JsonDataStoreSerializer
import kotlinx.serialization.json.Json

val Context.todoDataStore: DataStore<TodoData> by dataStore(
    fileName = "todos.json",
    serializer = TodoDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { TodoDataSerializer.defaultValue },
)

object TodoDataSerializer : JsonDataStoreSerializer<TodoData>(
    defaultValue = TodoData(),
    kSerializer = TodoData.serializer(),
    readErrorMessage = "과제 정보를 읽어오지 못했습니다.",
    json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    },
)
