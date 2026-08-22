package com.yourssu.ssutime.v2.screen.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.yourssu.ssutime.v2.R

enum class MainTab(
    val route: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    HOME("tab_home", R.string.tab_home, R.drawable.home),
    CALENDAR("tab_calendar", R.string.tab_calendar, R.drawable.calendar),
    MY("tab_my", R.string.tab_my, R.drawable.my);

    companion object {
        val startDestination: MainTab = HOME

        fun fromRoute(route: String?): MainTab =
            entries.firstOrNull { it.route == route } ?: HOME
    }
}
