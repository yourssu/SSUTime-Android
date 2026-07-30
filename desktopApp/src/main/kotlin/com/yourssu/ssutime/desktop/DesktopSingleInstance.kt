package com.yourssu.ssutime.desktop

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Win32Exception
import com.sun.jna.platform.win32.WinBase
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import java.awt.EventQueue

internal class DesktopSingleInstance private constructor(
    private val kernel32: Kernel32,
    private val mutexHandle: WinNT.HANDLE,
    private val activationEventHandle: WinNT.HANDLE,
) : AutoCloseable {
    @Volatile
    private var isListening = false

    @Volatile
    private var isClosed = false

    private var listenerThread: Thread? = null

    @Synchronized
    fun startListening(onActivationRequested: () -> Unit) {
        check(!isClosed) { "Single-instance guard is already closed." }
        if (listenerThread != null) return

        isListening = true
        listenerThread = Thread(
            {
                while (isListening) {
                    when (
                        kernel32.WaitForSingleObject(
                            activationEventHandle,
                            LISTENER_POLL_INTERVAL_MS,
                        )
                    ) {
                        WinBase.WAIT_OBJECT_0 -> {
                            if (isListening) {
                                EventQueue.invokeLater(onActivationRequested)
                            }
                        }

                        WinError.WAIT_TIMEOUT -> Unit
                        else -> break
                    }
                }
            },
            "ssutime-single-instance-listener",
        ).apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stopListening() {
        val thread = listenerThread ?: return
        isListening = false
        kernel32.SetEvent(activationEventHandle)
        thread.join(LISTENER_JOIN_TIMEOUT_MS)
        listenerThread = null
    }

    @Synchronized
    override fun close() {
        if (isClosed) return

        stopListening()
        isClosed = true
        kernel32.CloseHandle(activationEventHandle)
        kernel32.CloseHandle(mutexHandle)
    }

    companion object {
        private const val MUTEX_NAME =
            "Local\\com.yourssu.ssutime.desktop.SingleInstance"
        private const val ACTIVATION_EVENT_NAME =
            "Local\\com.yourssu.ssutime.desktop.Activate"
        private const val LISTENER_POLL_INTERVAL_MS = 1_000
        private const val LISTENER_JOIN_TIMEOUT_MS = 2_000L

        fun acquireOrNotifyExisting(
            kernel32: Kernel32 = Kernel32.INSTANCE,
        ): DesktopSingleInstance? {
            val activationEventHandle = kernel32.CreateEvent(
                null,
                false,
                false,
                ACTIVATION_EVENT_NAME,
            ) ?: throw Win32Exception(kernel32.GetLastError())

            val mutexHandle = kernel32.CreateMutex(
                null,
                false,
                MUTEX_NAME,
            )
            val mutexError = kernel32.GetLastError()

            if (mutexHandle == null) {
                kernel32.CloseHandle(activationEventHandle)
                throw Win32Exception(mutexError)
            }

            if (mutexError == WinError.ERROR_ALREADY_EXISTS) {
                kernel32.SetEvent(activationEventHandle)
                kernel32.CloseHandle(mutexHandle)
                kernel32.CloseHandle(activationEventHandle)
                return null
            }

            return DesktopSingleInstance(
                kernel32 = kernel32,
                mutexHandle = mutexHandle,
                activationEventHandle = activationEventHandle,
            )
        }
    }
}
