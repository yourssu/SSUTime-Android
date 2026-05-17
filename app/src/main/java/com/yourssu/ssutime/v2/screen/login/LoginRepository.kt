package com.yourssu.ssutime.v2.screen.login

import androidx.datastore.core.DataStore
import com.yourssu.data.LoginData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class LoginRepository(
    private val loginDataStore: DataStore<LoginData>
) {
    val loginData: Flow<LoginData> = loginDataStore.data

    suspend fun updateLoginData(loginData: LoginData) {
        loginDataStore.updateData { loginData }
    }

    suspend fun logout() {
        loginDataStore.updateData {
            it.copy(
                isAutoLogin = false,
                accessToken = "",
            )
        }
    }

    suspend fun getLoginData(): LoginData = loginDataStore.data.first()
}
