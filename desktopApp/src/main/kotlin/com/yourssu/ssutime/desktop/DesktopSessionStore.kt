package com.yourssu.ssutime.desktop

import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.platform.win32.WinCrypt
import com.yourssu.ssutime.desktop.core.model.AppProfile
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class DesktopStoredState(
    val userId: String = "",
    val protectedPassword: String = "",
    val autoLogin: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val systemNotificationsEnabled: Boolean = false,
    val todoData: AppTodoData = AppTodoData(),
    val profile: AppProfile? = null,
    val cyberUserId: String = "",
    val protectedCyberPassword: String = "",
    val isCyberConnected: Boolean = false,
    val isEnableSubmittedFile: Boolean = false,
)

data class StoredCredentials(
    val userId: String,
    val password: String,
)

class DesktopSessionStore(
    private val directory: Path = applicationDataDirectory(),
) {
    private val statePath = directory.resolve("state.json")
    private val secretCodec = createSecretCodec(directory.resolve("secret.key"))
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): DesktopStoredState {
        if (!Files.isRegularFile(statePath)) return DesktopStoredState()
        return runCatching {
            json.decodeFromString<DesktopStoredState>(Files.readString(statePath))
        }.getOrDefault(DesktopStoredState())
    }

    fun credentials(state: DesktopStoredState): StoredCredentials? {
        if (
            !state.autoLogin ||
            state.userId.isBlank() ||
            state.protectedPassword.isBlank()
        ) {
            return null
        }
        return runCatching {
            StoredCredentials(
                userId = state.userId,
                password = secretCodec.decrypt(state.protectedPassword),
            )
        }.getOrNull()
    }

    fun save(
        current: DesktopStoredState,
        userId: String,
        password: String,
        autoLogin: Boolean,
    ): DesktopStoredState {
        val next = current.copy(
            userId = if (autoLogin) userId else "",
            protectedPassword = if (autoLogin) secretCodec.encrypt(password) else "",
            autoLogin = autoLogin,
        )
        write(next)
        return next
    }

    fun update(current: DesktopStoredState): DesktopStoredState {
        write(current)
        return current
    }

    fun logout(current: DesktopStoredState): DesktopStoredState {
        val next = current.copy(
            userId = "",
            protectedPassword = "",
            autoLogin = false,
            todoData = AppTodoData(),
            profile = null,
        )
        write(next)
        return next
    }

    fun cyberCredentials(state: DesktopStoredState): StoredCredentials? {
        if (
            !state.isCyberConnected ||
            state.cyberUserId.isBlank() ||
            state.protectedCyberPassword.isBlank()
        ) {
            return null
        }
        return runCatching {
            StoredCredentials(
                userId = state.cyberUserId,
                password = secretCodec.decrypt(state.protectedCyberPassword),
            )
        }.getOrNull()
    }

    fun saveCyber(
        current: DesktopStoredState,
        cyberUserId: String,
        cyberPassword: String,
    ): DesktopStoredState {
        val next = current.copy(
            cyberUserId = cyberUserId,
            protectedCyberPassword = secretCodec.encrypt(cyberPassword),
            isCyberConnected = true,
        )
        write(next)
        return next
    }

    fun clearCyber(current: DesktopStoredState): DesktopStoredState {
        val next = current.copy(
            cyberUserId = "",
            protectedCyberPassword = "",
            isCyberConnected = false,
        )
        write(next)
        return next
    }

    private fun write(state: DesktopStoredState) {
        Files.createDirectories(directory)
        restrictToOwner(directory)
        val temporaryPath = directory.resolve("state.json.tmp")
        Files.writeString(temporaryPath, json.encodeToString(state))
        restrictToOwner(temporaryPath)
        try {
            Files.move(
                temporaryPath,
                statePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporaryPath,
                statePath,
                StandardCopyOption.REPLACE_EXISTING,
            )
        }
        restrictToOwner(statePath)
    }
}

private interface SecretCodec {
    fun encrypt(value: String): String
    fun decrypt(value: String): String
}

private class WindowsDpapiSecretCodec : SecretCodec {
    override fun encrypt(value: String): String {
        val plaintext = value.encodeToByteArray()
        return try {
            Base64.getEncoder().encodeToString(
                Crypt32Util.cryptProtectData(
                    plaintext,
                    WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
                ),
            )
        } finally {
            plaintext.fill(0)
        }
    }

    override fun decrypt(value: String): String {
        val protectedData = Base64.getDecoder().decode(value)
        val plaintext = try {
            Crypt32Util.cryptUnprotectData(
                protectedData,
                WinCrypt.CRYPTPROTECT_UI_FORBIDDEN,
            )
        } finally {
            protectedData.fill(0)
        }
        return try {
            plaintext.decodeToString()
        } finally {
            plaintext.fill(0)
        }
    }
}

private class AesGcmSecretCodec(
    private val keyPath: Path,
) : SecretCodec {
    private val random = SecureRandom()

    override fun encrypt(value: String): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(loadOrCreateKey(), "AES"),
            GCMParameterSpec(128, iv),
        )
        val encrypted = cipher.doFinal(value.encodeToByteArray())
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun decrypt(value: String): String {
        val payload = Base64.getDecoder().decode(value)
        require(payload.size > 12) { "저장된 로그인 정보가 올바르지 않습니다." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(loadOrCreateKey(), "AES"),
            GCMParameterSpec(128, payload.copyOfRange(0, 12)),
        )
        return cipher.doFinal(payload.copyOfRange(12, payload.size)).decodeToString()
    }

    private fun loadOrCreateKey(): ByteArray {
        if (Files.isRegularFile(keyPath)) {
            return Files.readAllBytes(keyPath)
        }
        Files.createDirectories(keyPath.parent)
        restrictToOwner(keyPath.parent)
        val key = ByteArray(32).also(random::nextBytes)
        Files.write(keyPath, key)
        restrictToOwner(keyPath)
        return key
    }
}

private fun createSecretCodec(keyPath: Path): SecretCodec =
    if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        WindowsDpapiSecretCodec()
    } else {
        AesGcmSecretCodec(keyPath)
    }

private fun restrictToOwner(path: Path) {
    runCatching {
        val permissions = if (Files.isDirectory(path)) {
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE,
            )
        } else {
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
            )
        }
        Files.setPosixFilePermissions(path, permissions)
    }
}

private fun applicationDataDirectory(): Path {
    System.getenv("SSUTIME_DATA_DIR")
        ?.takeIf(String::isNotBlank)
        ?.let(Path::of)
        ?.let { return it }

    val osName = System.getProperty("os.name").lowercase()
    val userHome = Path.of(System.getProperty("user.home"))
    return when {
        osName.contains("win") -> Path.of(
            System.getenv("APPDATA")
                ?: userHome.resolve("AppData").resolve("Roaming").toString(),
            "SSUTime",
        )
        osName.contains("mac") -> userHome
            .resolve("Library")
            .resolve("Application Support")
            .resolve("SSUTime")
        else -> Path.of(
            System.getenv("XDG_DATA_HOME")
                ?: userHome.resolve(".local").resolve("share").toString(),
            "SSUTime",
        )
    }
}
