package com.yourssu.ssutime.desktop

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopTextParityTest {
    private val root = Path.of("..")
    private val aliases = mapOf(
        "my_labs_title" to "my_lab_title",
        "my_labs_tooltip_title" to "my_lab_question",
        "my_labs_enable_submitted_file" to "my_lab_submitted_files",
        "my_labs_tooltip_desc" to "my_lab_description",
        "todo_detail_ai_sparkle_desc" to "todo_detail_ai_summary_banner",
        "todo_detail_late_notice" to "todo_detail_late_submission_available",
        "todo_detail_lms_link" to "todo_detail_open_lms",
        "todo_detail_back" to "common_back",
        "desktop_nav_todo" to "tab_home",
        "desktop_nav_my" to "tab_my",
        "cyber_login_failed" to "cyber_login_error_credentials",
        "cyber_login_error" to "cyber_login_error_general",
        "calendar_empty_events" to "calendar_empty_day_todos",
        "notice_new" to "calendar_new_notice",
        "notice_attachment" to "common_attachment",
        "main_submitted_attachment" to "common_attachment",
    )

    @Test
    fun sharedAndRenamedTextMatchesAndroidInBothLanguages() {
        for (locale in listOf("values", "values-ko")) {
            val android = strings(root.resolve("app/src/main/res/$locale/strings.xml"))
            val desktop = strings(Path.of("src/main/composeResources/$locale/strings.xml"))
            for ((name, value) in desktop) {
                val androidName = aliases[name] ?: name
                if (androidName in android) {
                    assertEquals(android[androidName], value, "$locale: $name -> $androidName")
                }
            }
            for (name in listOf("assignment_hide_popup_title", "common_hide_popup_title", "quiz_hide_popup_title")) {
                assertEquals(android.getValue(name), desktop.getValue(name), "$locale: $name")
            }
            // Android's tab labels currently come directly from this enum rather than localized resources.
            val tabs = Files.readString(root.resolve("app/src/main/java/com/yourssu/ssutime/v2/screen/main/todo/TodoDetailTab.kt"))
            val labels = Regex("""(DESCRIPTION|AI_SUMMARY)\("([^"]+)"""").findAll(tabs).associate { it.groupValues[1] to it.groupValues[2] }
            assertEquals(labels.getValue("DESCRIPTION"), desktop.getValue("todo_detail_description_tab"))
            assertEquals(labels.getValue("AI_SUMMARY"), desktop.getValue("todo_detail_ai_summary_tab"))
        }
    }

    private fun strings(path: Path): Map<String, String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile()).getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }
}
