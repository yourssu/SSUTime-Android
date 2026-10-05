package com.yourssu.ssutime.desktop.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.io.StringReader
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator

fun parseHtmlToPlainText(html: String): String =
    parseHtmlToAnnotatedString(html).text.replace('\u00a0', ' ')

/**
 * JVM counterpart of Android's AnnotatedString.fromHtml: text formatting and links,
 * rather than a browser document. Parsing does not execute scripts or load remote assets.
 */
fun parseHtmlToAnnotatedString(
    html: String,
    onOpenUrl: ((String) -> Unit)? = null,
): AnnotatedString {
    if (html.isBlank()) return AnnotatedString("")
    val builder = AnnotatedString.Builder()
    data class Element(val tag: String, val start: Int, val style: SpanStyle, val href: String?, val order: Int)
    val elements = mutableListOf<Element>()
    val styles = mutableListOf<Pair<Element, Int>>()
    var nextOrder = 0
    var ignoredDepth = 0
    var preformatted = false
    var pendingBreaks = 0
    fun flushBreaks() {
        if (builder.length > 0) repeat(pendingBreaks) { builder.append('\n') }
        pendingBreaks = 0
    }
    fun start(tag: String, attrs: MutableAttributeSet) {
        if (tag in setOf("head", "script", "style")) { ignoredDepth++; return }
        if (ignoredDepth > 0) return
        if (tag in blocks) pendingBreaks = maxOf(pendingBreaks, if (tag == "div" || tag == "li") 1 else 2)
        flushBreaks()
        if (tag == "li") builder.append("• ")
        if (tag == "pre") preformatted = true
        elements += Element(tag, builder.length, htmlStyle(tag, attrs), attrs.getAttribute(HTML.Attribute.HREF)?.toString(), nextOrder++)
    }
    fun end(tag: String) {
        if (tag in setOf("head", "script", "style")) { ignoredDepth = (ignoredDepth - 1).coerceAtLeast(0); return }
        if (ignoredDepth > 0) return
        val index = elements.indexOfLast { it.tag == tag }
        if (index >= 0) {
            val element = elements.removeAt(index)
            if (element.start < builder.length) {
                styles += element to builder.length
                element.href?.takeIf { it.isNotBlank() }?.let { url ->
                    builder.addLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(SpanStyle(color = Color(0xFF007BFF), textDecoration = TextDecoration.Underline)),
                            onOpenUrl?.let { open -> LinkInteractionListener { open(url) } },
                        ),
                        element.start, builder.length,
                    )
                }
            }
        }
        if (tag == "pre") preformatted = false
        if (tag in blocks) pendingBreaks = maxOf(pendingBreaks, if (tag == "div" || tag == "li") 1 else 2)
    }
    ParserDelegator().parse(StringReader(html), object : HTMLEditorKit.ParserCallback() {
        override fun handleStartTag(tag: HTML.Tag, attributes: MutableAttributeSet, pos: Int) =
            start(tag.toString(), attributes)
        override fun handleEndTag(tag: HTML.Tag, pos: Int) = end(tag.toString())
        override fun handleSimpleTag(tag: HTML.Tag, attributes: MutableAttributeSet, pos: Int) {
            when {
                ignoredDepth > 0 -> Unit
                tag == HTML.Tag.BR -> { flushBreaks(); builder.append('\n') }
                tag == HTML.Tag.HR -> pendingBreaks = maxOf(pendingBreaks, 2)
                tag is HTML.UnknownTag -> {
                    if (attributes.getAttribute(HTML.Attribute.ENDTAG) == "true") end(tag.toString())
                    else start(tag.toString(), attributes)
                }
            }
        }
        override fun handleText(data: CharArray, pos: Int) {
            if (ignoredDepth > 0) return
            flushBreaks()
            val text = String(data)
            builder.append(if (preformatted) text else text.replace(Regex("[\\t\\r\\n ]+"), " "))
        }
    }, true)
    // Close any malformed/unknown inline tags without losing their text.
    elements.toList().asReversed().forEach { end(it.tag) }
    // Parent styles precede child styles so nested colors and sizes override correctly.
    styles.sortedBy { it.first.order }.forEach { (element, end) ->
        builder.addStyle(element.style, element.start, end)
    }
    return builder.toAnnotatedString()
}

private val blocks = setOf("p", "div", "blockquote", "pre", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6")

private fun htmlStyle(tag: String, attributes: MutableAttributeSet): SpanStyle {
    var style = when (tag) {
        "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
        "i", "em", "cite", "dfn" -> SpanStyle(fontStyle = FontStyle.Italic)
        "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
        "s", "strike", "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript)
        "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript)
        "big" -> SpanStyle(fontSize = 1.25.em)
        "small" -> SpanStyle(fontSize = 0.8.em)
        "tt", "code", "pre" -> SpanStyle(fontFamily = FontFamily.Monospace)
        "h1", "h2", "h3", "h4", "h5", "h6" -> SpanStyle(
            fontWeight = FontWeight.Bold,
            fontSize = listOf(2f, 1.5f, 1.17f, 1f, 0.83f, 0.67f)[tag.last().digitToInt() - 1].em,
        )
        else -> SpanStyle()
    }
    htmlColor(attributes.getAttribute(HTML.Attribute.COLOR)?.toString())?.let { style = style.merge(SpanStyle(color = it)) }
    attributes.getAttribute(HTML.Attribute.FACE)?.toString()?.let {
        style = style.merge(SpanStyle(fontFamily = if (it.contains("mono", true)) FontFamily.Monospace else FontFamily.Default))
    }
    val css = attributes.getAttribute(HTML.Attribute.STYLE)?.toString().orEmpty()
        .split(';').mapNotNull {
            val pair = it.split(':', limit = 2)
            if (pair.size == 2) pair[0].trim().lowercase() to pair[1].trim().lowercase() else null
        }.toMap()
    htmlColor(css["color"])?.let { style = style.merge(SpanStyle(color = it)) }
    htmlColor(css["background-color"])?.let { style = style.merge(SpanStyle(background = it)) }
    if (css["font-weight"] == "bold" || (css["font-weight"]?.toIntOrNull() ?: 0) >= 600)
        style = style.merge(SpanStyle(fontWeight = FontWeight.Bold))
    if (css["font-style"] == "italic") style = style.merge(SpanStyle(fontStyle = FontStyle.Italic))
    css["text-decoration"]?.let {
        val decorations = listOfNotNull(
            TextDecoration.Underline.takeIf { _ -> "underline" in it },
            TextDecoration.LineThrough.takeIf { _ -> "line-through" in it },
        )
        if (decorations.isNotEmpty()) style = style.merge(SpanStyle(textDecoration = TextDecoration.combine(decorations)))
    }
    css["font-size"]?.let { size ->
        val amount = size.removeSuffix("px").removeSuffix("pt").removeSuffix("em").removeSuffix("%").toFloatOrNull()
        if (amount != null && amount > 0) {
            style = style.merge(SpanStyle(fontSize = when {
                size.endsWith("em") -> amount.em
                size.endsWith("%") -> (amount / 100).em
                size.endsWith("pt") -> (amount * 4 / 3).sp
                else -> amount.sp
            }))
        }
    }
    return style
}

private fun htmlColor(value: String?): Color? {
    val color = value?.trim()?.lowercase() ?: return null
    val named = mapOf(
        "black" to 0x000000, "white" to 0xffffff, "red" to 0xff0000, "green" to 0x008000,
        "blue" to 0x0000ff, "yellow" to 0xffff00, "gray" to 0x808080, "grey" to 0x808080,
        "purple" to 0x800080, "orange" to 0xffa500, "navy" to 0x000080, "teal" to 0x008080,
    )
    val rgb = named[color] ?: if (color.startsWith("#")) {
        val hex = color.drop(1)
        (if (hex.length == 3) hex.flatMap { listOf(it, it) }.joinToString("") else hex)
            .takeIf { it.length == 6 }?.toIntOrNull(16)
    } else {
        Regex("rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)").matchEntire(color)?.let {
            val channels = it.groupValues.drop(1).map(String::toInt)
            if (channels.all { channel -> channel in 0..255 }) (channels[0] shl 16) or (channels[1] shl 8) or channels[2] else null
        }
    }
    return rgb?.let { Color(0xff000000L or it.toLong()) }
}
