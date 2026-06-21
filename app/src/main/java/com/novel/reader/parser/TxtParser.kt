package com.novel.reader.parser

import com.novel.reader.data.Chapter
import java.io.InputStream
import java.nio.charset.Charset

/**
 * TXT 解析器：
 * 1. 自动探测编码（GBK / UTF-8 / UTF-16）；
 * 2. 用正则匹配中文章节标题做分章；
 * 3. 若匹配不到任何章节，则按固定字数切分。
 */
object TxtParser {

    // 常见章节标题：第一章 / 第123章 / 第1回 / 序章 / 楔子 / 引子 / 后记 / 第12节 …
    private val CHAPTER_REGEX = Regex(
        """^\s*第[零一二三四五六七八九十百千万0-9]+[章回节卷幕折集篇](.*)$|
           ^\s*(序章|楔子|引子|前言|序言|后记|尾声|番外篇?)(.*)$""".trimIndent(),
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    fun parse(stream: InputStream, fileName: String): BookParseResult {
        val bytes = stream.readBytes()
        val text = decodeBytes(bytes)
        val title = fileName.substringBeforeLast('.', fileName).trim()

        val chapters = splitChapters(text)
        return BookParseResult(title = title, author = "未知", chapters = chapters)
    }

    /** 探测编码：优先 UTF-8 BOM -> UTF-16 BOM -> UTF-8 严格解码 -> GBK 兜底 */
    private fun decodeBytes(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charset.forName("UTF-8"))
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16LE"))
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charset.forName("UTF-16BE"))
        }
        // 尝试 UTF-8 严格
        return try {
            val s = String(bytes, Charset.forName("UTF-8"))
            // 简单校验：若出现替换字符较多，回退 GBK
            if (s.contains("\uFFFD")) String(bytes, Charset.forName("GBK")) else s
        } catch (e: Exception) {
            String(bytes, Charset.forName("GBK"))
        }
    }

    private fun splitChapters(text: String): List<Chapter> {
        val lines = text.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        val chapters = mutableListOf<Chapter>()
        val current = StringBuilder()
        var currentTitle: String? = null

        fun flush() {
            if (currentTitle == null && current.isEmpty()) return
            val body = current.toString().trim()
            if (currentTitle != null || body.isNotEmpty()) {
                chapters.add(Chapter(currentTitle ?: "正文", body))
            }
            current.clear()
        }

        for (line in lines) {
            val trimmed = line.trim()
            val m = CHAPTER_REGEX.find(trimmed)
            if (m != null && trimmed.length <= 40) {
                flush()
                currentTitle = trimmed
            } else {
                if (current.isNotEmpty()) current.append('\n')
                current.append(line)
            }
        }
        flush()

        // 一个章节都没有匹配到 -> 按字数切分
        if (chapters.isEmpty()) {
            val whole = text.trim()
            if (whole.isEmpty()) return listOf(Chapter("正文", ""))
            val chunkSize = 5000
            return whole.chunked(chunkSize).mapIndexed { i, c ->
                Chapter("第${i + 1}部分", c)
            }
        }

        // 第一段没有标题（开头内容），归为序章
        if (chapters.first().title == "正文" && chapters.size > 1) {
            // 保留即可
        }
        return chapters
    }
}
