package com.yourssu.ssutime.v2.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.glance.color.ColorProvider

// Set of Material typography styles to start with
private val White = ColorProvider(day = Color.White, night = Color.White)


val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

internal object SSUType {
    val H1Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    )

    val H1SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    )

    val G_H1SemiBold = androidx.glance.text.TextStyle(
        fontWeight = androidx.glance.text.FontWeight.Bold,
        fontSize = 24.sp,
        color = White
    )

    val H2Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    )

    val H2SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    )

    val H3Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    )

    val H3SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.3).sp
    )

    val G_H3SemiBold = androidx.glance.text.TextStyle(
        fontWeight = androidx.glance.text.FontWeight.Bold,
        fontSize = 18.sp,
        color = White
    )

    val H4Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    val H4SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    val H4ExtraBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W800,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    )

    val H5SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.15).sp
    )

    val Label1Regular = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.18).sp
    )

    val Label1Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.18).sp
    )

    val Label1SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.18).sp
    )

    val Label1Bold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W700,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.18).sp
    )

    val Label2Regular = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val Label2Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val Label2SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val Label2Bold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W700,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.16).sp
    )

    val Label3Regular = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.12).sp
    )

    val Label3Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.12).sp
    )

    val Label3SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.12).sp
    )

    val Label3Bold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W700,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = (-0.12).sp
    )

    val Body1Regular = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    val Body1Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    val Body2Regular = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    )

    val Body2Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.1).sp
    )

    val Caption1Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 12.sp,
        lineHeight = 12.sp,
        letterSpacing = (0).sp
    )

    val Caption1SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 12.sp,
        letterSpacing = (0).sp
    )

    val G_Caption1SemiBold = androidx.glance.text.TextStyle(
        fontWeight = androidx.glance.text.FontWeight.Bold,
        fontSize = 12.sp,

        color = White
    )

    val Caption2Medium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 10.sp,
        lineHeight = 10.sp,
        letterSpacing = (-0.18).sp
    )

    val G_Caption2Medium = androidx.glance.text.TextStyle(
        fontWeight = androidx.glance.text.FontWeight.Medium,
        fontSize = 10.sp,
        color = White
    )

    val Caption2SemiBold = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W600,
        fontSize = 10.sp,
        lineHeight = 10.sp,
        letterSpacing = (-0.18).sp
    )

    val G_Caption2SemiBold = androidx.glance.text.TextStyle(
        fontWeight = androidx.glance.text.FontWeight.Bold,
        fontSize = 10.sp,
        color = White
    )

    val Caption3Regular = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W400,
        fontSize = 8.sp,
        lineHeight = 10.sp,
        letterSpacing = (-0.18).sp
    )

    val G_Caption3Regular = androidx.glance.text.TextStyle(
        fontWeight = androidx.glance.text.FontWeight.Normal,
        fontSize = 8.sp,
        color = White
    )
}
