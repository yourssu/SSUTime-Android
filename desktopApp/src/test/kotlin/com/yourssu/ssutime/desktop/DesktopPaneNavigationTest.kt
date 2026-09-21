package com.yourssu.ssutime.desktop

import com.yourssu.ssutime.desktop.screen.main.DEFAULT_WINDOW_HEIGHT_DP
import com.yourssu.ssutime.desktop.screen.main.DEFAULT_WINDOW_WIDTH_DP
import com.yourssu.ssutime.desktop.screen.main.DesktopNavTab
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopPaneNavigationTest {

    @Test
    fun `navigation tabs contain todo, calendar, and my page in 1-pane mode`() {
        val tabs = DesktopNavTab.entries
        assertEquals(3, tabs.size)
        assertEquals(DesktopNavTab.TODO, tabs[0])
        assertEquals(DesktopNavTab.CALENDAR, tabs[1])
        assertEquals(DesktopNavTab.MY_PAGE, tabs[2])
    }

    @Test
    fun `default window size is set for single pane layout`() {
        assertEquals(520, DEFAULT_WINDOW_WIDTH_DP)
        assertEquals(760, DEFAULT_WINDOW_HEIGHT_DP)
    }
}
