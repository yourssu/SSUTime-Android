package com.yourssu.ssutime.v2.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.net.toUri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.yourssu.ssutime.v2.R

private val TRAILING_NUMERIC_SUFFIX = Regex("""\s*\(\d+\)\s*$""")

/**
 * 문자열 끝의 `(숫자)` 접미사를 제거합니다.
 */
internal fun String.withoutTrailingNumericSuffix(): String =
    replace(TRAILING_NUMERIC_SUFFIX, "")

/**
 * HTML 형식의 문자열을 Compose의 AnnotatedString으로 변환합니다.
 */
internal fun parseHtmlToAnnotatedString(html: String): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")
    return runCatching {
        AnnotatedString.fromHtml(
            htmlString = html,
            linkStyles = TextLinkStyles(
                style = SpanStyle(
                    color = Color(0xFF007BFF),
                    textDecoration = TextDecoration.Underline,
                )
            )
        )
    }.getOrElse { AnnotatedString(html) }
}

/**
 * 외부 브라우저 등으로 URL을 엽니다.
 */
internal fun Context.openExternalUrl(
    url: String,
    addNewTaskFlag: Boolean = true,
    showInvalidToast: Boolean = false,
    showNoAppToast: Boolean = true,
    onFailure: ((Throwable) -> Unit)? = null,
) {
    if (url.isBlank()) {
        if (showInvalidToast) {
            Toast.makeText(this, getString(R.string.notice_attachment_invalid_path), Toast.LENGTH_SHORT).show()
        }
        return
    }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            if (addNewTaskFlag) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        startActivity(intent)
    }.onFailure { exception ->
        onFailure?.invoke(exception)
        if (showNoAppToast) {
            Toast.makeText(this, getString(R.string.common_open_link_no_app), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
internal fun NoTransitionNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        modifier = modifier,
        builder = builder,
    )
}
