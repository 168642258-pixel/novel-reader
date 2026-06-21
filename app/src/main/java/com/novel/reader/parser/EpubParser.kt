package com.novel.reader.parser

import com.novel.reader.data.Chapter
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * EPUB 解析器：
 * 1. 以 ZIP 方式打开 epub；
 * 2. 读取 META-INF/container.xml 找到 OPF 文件路径；
 * 3. 解析 OPF 的 metadata（书名/作者）与 spine+manifest（阅读顺序）；
 * 4. 按顺序提取每个 XHTML 的纯文本作为一个章节。
 *
 * 仅依赖 jsoup 做 HTML 正文清洗，OPF/container 用 XmlPullParser 解析。
 */
object EpubParser {

    fun parse(stream: InputStream): BookParseResult {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(stream).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }

        // 1. container.xml -> opf 路径
        val containerXml = entries["META-INF/container.xml"]
            ?: error("非法的 EPUB 文件：缺少 container.xml")
        val opfPath = parseContainer(containerXml.toString(Charsets.UTF_8))

        // 2. opf -> metadata + spine(reading order)
        val opfBytes = entries[opfPath] ?: error("找不到 OPF 文件: $opfPath")
        val opfDir = opfPath.substringBeforeLast('/', "")
        val (title, author, spineHrefs) = parseOpf(opfBytes.toString(Charsets.UTF_8))

        // 3. 解析 manifest: id -> href（相对于 opf 目录）
        // 重新解析一次拿到 manifest，合并到上面
        val manifest = parseManifest(opfBytes.toString(Charsets.UTF_8))

        val chapters = mutableListOf<Chapter>()
        for (idref in spineHrefs) {
            val href = manifest[idref] ?: continue
            val fullPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
            val html = entries[fullPath]?.toString(Charsets.UTF_8) ?: continue
            val text = htmlToText(html)
            if (text.isNotBlank()) {
                val chTitle = extractTitle(html) ?: "第${chapters.size + 1}章"
                chapters.add(Chapter(chTitle, text))
            }
        }

        if (chapters.isEmpty()) error("EPUB 未解析到任何正文内容")
        return BookParseResult(
            title = title.ifBlank { "未知书名" },
            author = author.ifBlank { "未知" },
            chapters = chapters
        )
    }

    // ---------- container.xml ----------
    private fun parseContainer(xml: String): String {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path")
                    ?: error("container.xml 缺少 full-path")
            }
            event = parser.next()
        }
        error("container.xml 中未找到 rootfile")
    }

    // ---------- OPF ----------
    private data class OpfInfo(val title: String, val author: String, val spine: List<String>)

    private fun parseOpf(xml: String): OpfInfo {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        var title = ""
        var author = ""
        val spine = mutableListOf<String>()
        var inMetadata = false
        var inSpine = false
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "metadata" -> inMetadata = true
                    "title" -> if (inMetadata) title = parser.nextText().trim()
                    "creator" -> if (inMetadata && author.isBlank()) author = parser.nextText().trim()
                    "spine" -> inSpine = true
                    "itemref" -> if (inSpine) {
                        parser.getAttributeValue(null, "idref")?.let { spine.add(it) }
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "metadata" -> inMetadata = false
                    "spine" -> inSpine = false
                }
            }
            event = parser.next()
        }
        return OpfInfo(title, author, spine)
    }

    private fun parseManifest(xml: String): Map<String, String> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        val manifest = mutableMapOf<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val id = parser.getAttributeValue(null, "id")
                val href = parser.getAttributeValue(null, "href")
                if (id != null && href != null) manifest[id] = href
            }
            event = parser.next()
        }
        return manifest
    }

    // ---------- HTML -> 纯文本 ----------
    private fun htmlToText(html: String): String {
        val doc = Jsoup.parse(html, "", Parser.xmlParser())
        // 移除脚本/样式
        doc.select("script, style, head").remove()
        // <br> <p> 转换为换行
        doc.select("br").after("\n")
        doc.select("p, div, h1, h2, h3, h4, h5, h6, li, tr").after("\n")
        val text = doc.wholeText()
        // 合并多余空行
        return text.split('\n').joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun extractTitle(html: String): String? {
        val doc = Jsoup.parse(html, "", Parser.xmlParser())
        doc.select("h1, h2, h3").firstOrNull()?.text()?.takeIf { it.isNotBlank() }?.let { return it }
        doc.title()?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }
}
