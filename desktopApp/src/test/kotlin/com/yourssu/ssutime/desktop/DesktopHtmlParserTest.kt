package com.yourssu.ssutime.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.yourssu.ssutime.desktop.ui.util.parseHtmlToAnnotatedString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopHtmlParserTest {
    @Test fun formattingEntitiesParagraphsAndLinksSurviveHtmlParsing() {
        var opened = ""
        val text = parseHtmlToAnnotatedString(
            """<p><b>굵게</b> <span style="color: #ff0000; text-decoration: underline">색상</span></p><p><a href="https://example.com/task?a=1&amp;b=2">자료 링크</a><br>&lt;내용&gt; &#9733;</p>""",
        ) { opened = it }
        assertEquals("굵게 색상\n\n자료 링크\n<내용> ★", text.text)
        assertTrue(text.spanStyles.any { it.item.fontWeight == FontWeight.Bold && text.text.substring(it.start, it.end) == "굵게" })
        assertTrue(text.spanStyles.any { it.item.color == Color.Red && it.item.textDecoration == TextDecoration.Underline })
        val link = text.getLinkAnnotations(0, text.length).single()
        assertEquals("자료 링크", text.text.substring(link.start, link.end))
        val url = link.item as LinkAnnotation.Url
        assertEquals("https://example.com/task?a=1&b=2", url.url)
        url.linkInteractionListener!!.onClick(url)
        assertEquals(url.url, opened)
    }

    @Test fun malformedHtmlKeepsTextAndSkipsScriptContent() {
        val text = parseHtmlToAnnotatedString("<p>본문 <b>강조</p><script>alert('bad')</script><p>다음</p>")
        assertEquals("본문 강조\n\n다음", text.text)
        assertTrue(text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertEquals("", parseHtmlToAnnotatedString("").text)
    }
}
