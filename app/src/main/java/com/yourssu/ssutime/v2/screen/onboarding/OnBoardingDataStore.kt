package com.yourssu.ssutime.v2.screen.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.ssutime.v2.datastore.JsonDataStoreSerializer

val Context.onBoardingDataStore: DataStore<OnBoardingData> by dataStore(
    fileName = "onboarding.json",
    serializer = OnBoardingDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { OnBoardingDataSerializer.defaultValue },
)

object OnBoardingDataSerializer : JsonDataStoreSerializer<OnBoardingData>(
    defaultValue = OnBoardingData(),
    kSerializer = OnBoardingData.serializer(),
    readErrorMessage = "온보딩 정보를 읽어오지 못했습니다.",
)
