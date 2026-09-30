package com.yourssu.ssutime.desktop

import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import java.awt.SystemTray
import java.awt.EventQueue
import java.awt.event.ActionEvent
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DesktopWindowsRuntimeTest {
    @Test fun windowsDuplicateLaunchNotifiesExistingInstanceAndLockIsReleased() {
        assumeTrue(System.getProperty("os.name").startsWith("Windows"))
        val namespace = "ssutime.qa.${UUID.randomUUID()}"
        val first = DesktopSingleInstance.acquireOrNotifyExisting(namespace = namespace)!!
        val activated = CountDownLatch(1)
        try {
            first.startListening { activated.countDown() }
            assertNull(DesktopSingleInstance.acquireOrNotifyExisting(namespace = namespace))
            assertTrue(activated.await(5, TimeUnit.SECONDS))
        } finally { first.close() }
        val reopened = DesktopSingleInstance.acquireOrNotifyExisting(namespace = namespace)
        assertNotNull(reopened)
        reopened!!.close()
    }

    @Test fun windowsTrayExistsWithoutNotificationsAndMenuDispatchesOpenAndExit() {
        assumeTrue(DesktopDeadlineNotifier.isSupported())
        val tray = SystemTray.getSystemTray()
        val initial = tray.trayIcons.toSet()
        var opened = false
        var exited = false
        val notifier = DesktopDeadlineNotifier(onOpen = { opened = true }, onExit = { exited = true })
        try {
            EventQueue.invokeAndWait { assertTrue(notifier.start()) }
            val added = (tray.trayIcons.toSet() - initial).single()
            EventQueue.invokeAndWait {
                val open = added.popupMenu.getItem(0)
                open.actionListeners.forEach { it.actionPerformed(ActionEvent(open, 0, "open")) }
                val exit = added.popupMenu.getItem(2)
                exit.actionListeners.forEach { it.actionPerformed(ActionEvent(exit, 0, "exit")) }
            }
            assertTrue(opened)
            assertTrue(exited)
        } finally { EventQueue.invokeAndWait { notifier.close() } }
        assertEquals(initial, tray.trayIcons.toSet())
    }
}
