package com.yourssu.ssutime.desktop.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.pretendard_black
import com.yourssu.ssutime.desktop.ui.resources.pretendard_bold
import com.yourssu.ssutime.desktop.ui.resources.pretendard_extrabold
import com.yourssu.ssutime.desktop.ui.resources.pretendard_extralight
import com.yourssu.ssutime.desktop.ui.resources.pretendard_light
import com.yourssu.ssutime.desktop.ui.resources.pretendard_medium
import com.yourssu.ssutime.desktop.ui.resources.pretendard_regular
import com.yourssu.ssutime.desktop.ui.resources.pretendard_semibold
import com.yourssu.ssutime.desktop.ui.resources.pretendard_thin
import org.jetbrains.compose.resources.Font

@Composable
fun pretendardFamily(): FontFamily = FontFamily(
    Font(Res.font.pretendard_extralight, FontWeight.ExtraLight),
    Font(Res.font.pretendard_light, FontWeight.Light),
    Font(Res.font.pretendard_bold, FontWeight.Bold),
    Font(Res.font.pretendard_thin, FontWeight.Thin),
    Font(Res.font.pretendard_medium, FontWeight.Medium),
    Font(Res.font.pretendard_black, FontWeight.Black),
    Font(Res.font.pretendard_extrabold, FontWeight.ExtraBold),
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
)

@Composable
fun ssuTypography(): Typography = Typography(
    bodyLarge = ssuTextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)

@Composable
private fun ssuTextStyle(
    fontWeight: FontWeight,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    letterSpacing: TextUnit,
): TextStyle = TextStyle(
    fontFamily = pretendardFamily(),
    fontWeight = fontWeight,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

object SSUType {
    val H1Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 24.sp, 36.sp, (-0.5).sp)

    val H1SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 24.sp, 36.sp, (-0.5).sp)

    val H2Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 20.sp, 28.sp, (-0.4).sp)

    val H2SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 20.sp, 28.sp, (-0.4).sp)

    val H3Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 18.sp, 24.sp, (-0.3).sp)

    val H3SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 18.sp, 24.sp, (-0.3).sp)

    val H4Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 16.sp, 24.sp, (-0.2).sp)

    val H4SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 16.sp, 24.sp, (-0.2).sp)

    val H4ExtraBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W800, 16.sp, 24.sp, (-0.2).sp)

    val H5SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 14.sp, 20.sp, (-0.15).sp)

    val Label1Regular: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W400, 16.sp, 22.sp, (-0.18).sp)

    val Label1Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 16.sp, 22.sp, (-0.18).sp)

    val Label1SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 16.sp, 22.sp, (-0.18).sp)

    val Label1Bold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W700, 16.sp, 22.sp, (-0.18).sp)

    val Label2Regular: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W400, 14.sp, 20.sp, (-0.16).sp)

    val Label2Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 14.sp, 20.sp, (-0.16).sp)

    val Label2SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 14.sp, 20.sp, (-0.16).sp)

    val Label2Bold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W700, 14.sp, 20.sp, (-0.16).sp)

    val Label3Regular: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W400, 12.sp, 16.sp, (-0.12).sp)

    val Label3Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 12.sp, 16.sp, (-0.12).sp)

    val Label3SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 12.sp, 16.sp, (-0.12).sp)

    val Label3Bold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W700, 12.sp, 16.sp, (-0.12).sp)

    val Body1Regular: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W400, 14.sp, 20.sp, 0.sp)

    val Body1Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 14.sp, 20.sp, 0.sp)

    val Body2Regular: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W400, 12.sp, 18.sp, (-0.1).sp)

    val Body2Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 12.sp, 18.sp, (-0.1).sp)

    val Caption1Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 12.sp, 12.sp, 0.sp)

    val Caption1SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 12.sp, 12.sp, 0.sp)

    val Caption2Medium: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W500, 10.sp, 10.sp, (-0.18).sp)

    val Caption2SemiBold: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W600, 10.sp, 10.sp, (-0.18).sp)

    val Caption3Regular: TextStyle
        @Composable get() = ssuTextStyle(FontWeight.W400, 8.sp, 10.sp, (-0.18).sp)
}
