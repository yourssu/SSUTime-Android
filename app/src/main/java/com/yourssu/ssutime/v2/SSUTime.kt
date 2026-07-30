package com.yourssu.ssutime.v2

import android.content.Context
import android.text.format.DateFormat
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.yourssu.data.AlertData
import com.yourssu.data.LoginData
import com.yourssu.data.TodoData
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.login.LoginViewModel
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import com.yourssu.ssutime.v2.screen.main.MainViewModel
import com.yourssu.ssutime.v2.screen.main.TermSelectionStore
import com.yourssu.ssutime.v2.screen.main.notificationStore
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import com.yourssu.ssutime.v2.screen.my.MyViewModel
import com.yourssu.ssutime.v2.screen.onboarding.OnBoardingData
import com.yourssu.ssutime.v2.screen.onboarding.OnBoardingRepository
import com.yourssu.ssutime.v2.screen.onboarding.OnBoardingViewModel
import com.yourssu.ssutime.v2.screen.onboarding.onBoardingDataStore
import com.yourssu.ssutime.v2.screen.splash.SplashViewModel
import com.yourssu.ssutime.v2.security.LoginDataCrypto
import com.yourssu.ssutime.v2.todo.TODO_DEADLINE_ZONE_ID
import com.yourssu.ssutime.v2.todo.remainingDaysUntilDeadline
import com.yourssu.ssutime.v2.todo.remainingTimeTextUntilDeadline
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

const val CHANNEL_ID = "ASSIGNMENT"
const val CALL_CHANNEL_ID = "ASSIGNMENT_CALL_V2"

internal var accessToken = ""

val appModule = module {
    single<DataStore<LoginData>> { androidContext().loginDataStore }
    single<DataStore<TodoData>>(named("todoDataStore")) { androidContext().todoDataStore }
    single<DataStore<OnBoardingData>>(named("onBoardingDataStore")) { androidContext().onBoardingDataStore }
    single<DataStore<AlertData>>(named("alertDataStore")) { androidContext().notificationStore }

    single { LoginRepository(get()) }
    single {
        MainRepository(
            get(named("todoDataStore")),
            get(named("alertDataStore")),
            get(),
            androidContext()
        )
    }
    single {
        ApiRepository()
    }
    single {
        LmsRefreshRepository(
            get(),
            get(),
            get()
        )
    }
    single { TermSelectionStore() }
    single {
        OnBoardingRepository(
            get(
                named("onBoardingDataStore")
            )
        )
    }
    viewModel { MyViewModel(get(), get(), get()) }
    viewModel { SplashViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel {
        OnBoardingViewModel(
            androidContext(),
            get(),
            get()
        )
    }
    viewModel { MainViewModel(get(), get(), get()) }
}

// Compose Preview를 위한 koinModule
val previewModule = module {
    single<DataStore<LoginData>> { androidContext().loginDataStore }
    single<DataStore<TodoData>>(named("todoDataStore")) { androidContext().todoDataStore }
    single<DataStore<OnBoardingData>>(named("onBoardingDataStore")) { androidContext().onBoardingDataStore }
    single<DataStore<AlertData>>(named("alertDataStore")) { androidContext().notificationStore }

    single { LoginRepository(get()) }
    single {
        ApiRepository()
    }
    single {
        MainRepository(
            get(named("todoDataStore")),
            get(named("alertDataStore")),
            get(),
            androidContext()
        )
    }
    single {
        LmsRefreshRepository(
            get(),
            get(),
            get()
        )
    }
    single { TermSelectionStore() }
    single {
        OnBoardingRepository(
            get(
                named("onBoardingDataStore")
            )
        )
    }
    viewModel { MyViewModel(get(), get(), get()) }
    viewModel { SplashViewModel(get(), get()) }
    viewModel { LoginViewModel(get(), get()) }
    viewModel {
        OnBoardingViewModel(
            androidContext(),
            get(),
            get()
        )
    }
    viewModel { MainViewModel(get(), get(), get()) }
}

val Context.loginDataStore: DataStore<LoginData> by dataStore(
    fileName = "account.json",
    serializer = LoginDataSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { LoginDataSerializer.defaultValue },
    produceMigrations = { listOf(LoginDataEncryptionMigration) },
)

object LoginDataSerializer : Serializer<LoginData> {
    override val defaultValue: LoginData = LoginData(id = "", pw = "", isAutoLogin = false, accessToken = "")
    @Volatile
    private var lastReadWasPlainText = false

    internal val needsPlainTextMigration: Boolean
        get() = lastReadWasPlainText

    internal fun clearPlainTextMigrationFlag() {
        lastReadWasPlainText = false
    }

    override suspend fun readFrom(input: InputStream): LoginData =
        try {
            val storedValue = input.readBytes().decodeToString()
            val json = if (LoginDataCrypto.isEncrypted(storedValue)) {
                lastReadWasPlainText = false
                LoginDataCrypto.decrypt(storedValue).decodeToString()
            } else {
                lastReadWasPlainText = true
                storedValue
            }
            Json.decodeFromString<LoginData>(json)
        } catch (serialization: SerializationException) {
            SentryExceptionReporter.capture(serialization)
            throw CorruptionException("계정 정보를 읽어오지 못했습니다.", serialization)
        } catch (security: GeneralSecurityException) {
            lastReadWasPlainText = false
            SentryExceptionReporter.capture(security)
            throw CorruptionException("계정 정보를 복호화하지 못했습니다.", security)
        } catch (illegalArgument: IllegalArgumentException) {
            lastReadWasPlainText = false
            SentryExceptionReporter.capture(illegalArgument)
            throw CorruptionException("계정 정보 암호문 형식이 올바르지 않습니다.", illegalArgument)
        }

    override suspend fun writeTo(t: LoginData, output: OutputStream) {
        val json = Json.encodeToString(LoginData.serializer(), t)
        val encryptedValue = LoginDataCrypto.encrypt(json.encodeToByteArray())
        output.write(encryptedValue.encodeToByteArray())
        clearPlainTextMigrationFlag()
    }
}

private object LoginDataEncryptionMigration : DataMigration<LoginData> {
    override suspend fun shouldMigrate(currentData: LoginData): Boolean =
        LoginDataSerializer.needsPlainTextMigration

    override suspend fun migrate(currentData: LoginData): LoginData =
        currentData

    override suspend fun cleanUp() {
        LoginDataSerializer.clearPlainTextMigrationFlag()
    }
}

fun getRemainingDays(targetTime: String, now: Instant = Instant.now()): Long {
    return remainingDaysUntilDeadline(targetTime, now)
}

fun getRemainingTimeText(targetTime: String, now: Instant = Instant.now()): String {
    return remainingTimeTextUntilDeadline(targetTime, now)
}

fun getStringDate(targetTime: String): String {
    val targetInstant = Instant.parse(targetTime)
    val formatter = DateTimeFormatter.ofPattern(monthDayPattern(), Locale.getDefault())

    return targetInstant
        .atZone(TODO_DEADLINE_ZONE_ID)
        .format(formatter)
}

fun getStringDateWithTime(targetTime: String): String {
    val targetInstant = Instant.parse(targetTime)
    val formatter = DateTimeFormatter.ofPattern(monthDayTimePattern(), Locale.getDefault())

    return targetInstant
        .atZone(TODO_DEADLINE_ZONE_ID)
        .format(formatter)
}


fun getStringSimpleDate(context: Context, targetTime: String): String {
    val targetInstant = Instant.parse(targetTime)
    val formatter = DateTimeFormatter.ofPattern(
        if (DateFormat.is24HourFormat(context)) "HH:mm" else "a hh:mm",
        Locale.getDefault()
    )

    return targetInstant
        .atZone(TODO_DEADLINE_ZONE_ID)
        .format(formatter)
}

private fun monthDayPattern(): String =
    if (Locale.getDefault().language == Locale.KOREAN.language) "MM월 dd일" else "MMM dd"

private fun monthDayTimePattern(): String =
    if (Locale.getDefault().language == Locale.KOREAN.language) "MM월 dd일 HH:mm:ss" else "MMM dd HH:mm:ss"
