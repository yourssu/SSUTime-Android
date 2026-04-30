package com.yourssu.ssutime

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.LoginData
import com.yourssu.ssutime.screen.login.LoginRepository
import com.yourssu.ssutime.screen.login.LoginViewModel
import com.yourssu.ssutime.screen.main.MainViewModel
import com.yourssu.ssutime.screen.onboarding.OnBoardingViewModel
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

const val CHANNEL_ID = "ASSIGNMENT"

val appModule = module {
    single<DataStore<LoginData>> { androidContext().loginDataStore }
    single { LoginRepository(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { OnBoardingViewModel(androidContext()) }
    viewModel { MainViewModel() }
}

// Compose Preview를 위한 koinModule
val previewModule = module {
    single<DataStore<LoginData>> { androidContext().loginDataStore }
    single { LoginRepository(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { OnBoardingViewModel(androidContext()) }
    viewModel { MainViewModel() }
}

val Context.loginDataStore: DataStore<LoginData> by dataStore(
    fileName = "account.json",
    serializer = LoginDataSerializer,
)

object LoginDataSerializer : Serializer<LoginData> {
    override val defaultValue: LoginData = LoginData(id = "", pw = "", isAutoLogin = false)
    override suspend fun readFrom(input: InputStream): LoginData =
        try {
            Json.decodeFromString<LoginData>(
                input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("계정 정보를 읽어오지 못했습니다.", serialization)
        }

    override suspend fun writeTo(t: LoginData, output: OutputStream) {
        output.write(
            Json.encodeToString(LoginData.serializer(), t)
                .encodeToByteArray()
        )
    }
}

fun getRemainingDays(targetTime: String): Long {
    val targetInstant = Instant.parse(targetTime)
    val now = Instant.now()

    return max(0, ChronoUnit.DAYS.between(now, targetInstant))
}

fun getStringDate(targetTime: String): String {
    val targetInstant = Instant.parse(targetTime)
    val zoneId = ZoneId.of("Asia/Seoul")
    val formatter = DateTimeFormatter.ofPattern(
        "yyyy년 MM월 dd일 HH:mm:ss",
        Locale.KOREA
    )

    return targetInstant
        .atZone(zoneId)
        .format(formatter)
}
