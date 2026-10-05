package com.yourssu.ssutime.desktop

import java.nio.file.Path

/** Executed in a separate JVM using the same classpath isolation as the IDE's runDesktop task. */
object DesktopRuntimeClasspathProbe {
    @JvmStatic
    fun main(args: Array<String>) {
        val screens = listOf(
            "calendar.DesktopCalendarScreenKt",
            "cyber.DesktopCyberLoginScreenKt",
            "login.DesktopLoginScreenKt",
            "main.DesktopMainScreenKt",
            "main.DesktopVerticalNavBarKt",
            "my.DesktopHiddenTodosScreenKt",
            "my.DesktopMyPageScreenKt",
            "notice.DesktopNoticeScreenKt",
            "onboarding.DesktopOnBoardingScreenKt",
            "splash.DesktopSplashScreenKt",
            "submitted.DesktopSubmittedScreenKt",
            "todo.DesktopTodoDetailScreenKt",
        )
        for (screen in screens) {
            val type = Class.forName("com.yourssu.ssutime.desktop.screen.$screen")
            // Resolve signatures too, rather than only looking for the JAR entry.
            check(type.declaredMethods.isNotEmpty())
            val location = Path.of(type.protectionDomain.codeSource.location.toURI())
            check(location.fileName.toString().endsWith(".jar")) { "Screen did not load from the application JAR: $screen" }
            check(location.parent.fileName.toString().startsWith("ssutime-desktop-run-")) {
                "Screen still loads from mutable build output: $screen"
            }
            println("Loaded $screen from private runtime JAR")
        }
    }
}
