package com.yourssu.ssutime.v2.screen.onboarding

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class OnBoardingRepository(
    private val onBoardingDataStore: DataStore<OnBoardingData>
) {
    val onBoardingData: Flow<OnBoardingData> = onBoardingDataStore.data

    suspend fun updateOnBoardingData(onBoardingData: OnBoardingData) {
        onBoardingDataStore.updateData { onBoardingData }
    }

    suspend fun getOnBoardingData(): OnBoardingData = onBoardingDataStore.data.first()
}
