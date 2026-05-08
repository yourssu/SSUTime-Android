package com.yourssu.ssutime

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.yourssu.data.LoginData
import com.yourssu.ssutime.screen.login.LoginRepository
import com.yourssu.ssutime.screen.login.LoginViewModel
import com.yourssu.ssutime.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.screen.main.MainRepository
import com.yourssu.ssutime.screen.main.MainViewModel
import com.yourssu.ssutime.screen.main.TodoData
import com.yourssu.ssutime.screen.main.todoDataStore
import com.yourssu.ssutime.screen.my.MyViewModel
import com.yourssu.ssutime.screen.onboarding.OnBoardingData
import com.yourssu.ssutime.screen.onboarding.OnBoardingRepository
import com.yourssu.ssutime.screen.onboarding.OnBoardingViewModel
import com.yourssu.ssutime.screen.onboarding.onBoardingDataStore
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.max

const val CHANNEL_ID = "ASSIGNMENT"
const val LMS_REFRESH_TOPIC = "lms-refresh"
const val LMS_REFRESH_MESSAGE_TYPE = "lms_refresh"

val appModule = module {
    single<DataStore<LoginData>> { androidContext().loginDataStore }
    single<DataStore<TodoData>>(named("todoDataStore")) { androidContext().todoDataStore }
    single<DataStore<OnBoardingData>>(named("onBoardingDataStore")) { androidContext().onBoardingDataStore }
    single { LoginRepository(get()) }
    single { MainRepository(get(named("todoDataStore"))) }
    single { LmsRefreshRepository(get(), get()) }
    single { OnBoardingRepository(get(named("onBoardingDataStore"))) }
    viewModel { MyViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { OnBoardingViewModel(androidContext(), get()) }
    viewModel { MainViewModel(get(), get()) }
}

// Compose Preview를 위한 koinModule
val previewModule = module {
    single<DataStore<LoginData>> { androidContext().loginDataStore }
    single<DataStore<TodoData>>(named("todoDataStore")) { androidContext().todoDataStore }
    single<DataStore<OnBoardingData>>(named("onBoardingDataStore")) { androidContext().onBoardingDataStore }
    single { LoginRepository(get()) }
    single { MainRepository(get(named("todoDataStore"))) }
    single { LmsRefreshRepository(get(), get()) }
    single { OnBoardingRepository(get(named("onBoardingDataStore"))) }
    viewModel { MyViewModel(get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { OnBoardingViewModel(androidContext(), get()) }
    viewModel { MainViewModel(get(), get()) }
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

fun getRemainingDays(targetTime: String, now: Instant = Instant.now()): Long {
    val targetInstant = parseTargetInstant(targetTime)
    return max(0, ChronoUnit.DAYS.between(now, targetInstant))
}

fun getRemainingTimeText(targetTime: String, now: Instant = Instant.now()): String {
    val targetInstant = parseTargetInstant(targetTime)

    val remainingSeconds = max(
        0,
        ChronoUnit.SECONDS.between(now, targetInstant)
    )

    return if (remainingSeconds < 60) {
        "${remainingSeconds}초"
    } else {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60

        "%02d:%02d".format(hours, minutes)
    }
}

private fun parseTargetInstant(targetTime: String): Instant {
    val normalizedTime = targetTime.removeSuffix("Z")

    return LocalDateTime
        .parse(normalizedTime)
        .atZone(ZoneId.of("Asia/Seoul"))
        .toInstant()
}

fun getStringDate(targetTime: String): String {
    val targetInstant = Instant.parse(targetTime)
    val zoneId = ZoneId.of("Asia/Seoul")
    val formatter = DateTimeFormatter.ofPattern(
//        "yyyy년 MM월 dd일 HH:mm:ss",
        "MM월 dd일",
        Locale.KOREA
    )

    return targetInstant
        .atZone(zoneId)
        .format(formatter)
}

fun getStringSimpleDate(context: Context, targetTime: String): String {
    val targetInstant = Instant.parse(targetTime)
    val zoneId = ZoneId.of("Asia/Seoul")
    val formatter = DateTimeFormatter.ofPattern(
        if (DateFormat.is24HourFormat(context)) "HH:mm" else "a hh:mm",
        Locale.KOREA
    )

    return targetInstant
        .atZone(zoneId)
        .format(formatter)
}
