package com.yourssu.ssutime.desktop

import com.yourssu.ssutime.desktop.screen.notice.formatDiscussionDate
import com.yourssu.ssutime.desktop.screen.notice.parseHtmlToPlainText
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopNoticeUtilTest {

    @Test
    fun `parseHtmlToPlainText strips html tags and decodes html entities`() {
        val html = "<p>안녕하세요.&nbsp;공지사항입니다.<br>다음 주 <strong>휴강</strong> 안내입니다.</p><div>참고하세요. &lt;링크&gt;</div>"
        val plain = parseHtmlToPlainText(html)

        assertTrue(plain.contains("안녕하세요. 공지사항입니다."))
        assertTrue(plain.contains("다음 주 휴강 안내입니다."))
        assertTrue(plain.contains("참고하세요. <링크>"))
    }

    @Test
    fun `formatDiscussionDate formats iso string to Korean datetime`() {
        val dateString = "2026-08-22T09:00:00Z"
        val formatted = formatDiscussionDate(dateString)

        assertTrue(formatted.contains("2026.08.22"))
    }
}
