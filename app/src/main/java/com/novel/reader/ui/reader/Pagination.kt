package com.novel.reader.ui.reader

/**
 * 文本分页：按段落累积填充，尽量不把段落拆断。
 * @param paragraphs 段落列表
 * @param charsPerPage 每页估算字符容量
 */
fun paginate(paragraphs: List<String>, charsPerPage: Int): List<List<String>> {
    if (paragraphs.isEmpty()) return listOf(emptyList())
    val pages = mutableListOf<MutableList<String>>()
    var current = mutableListOf<String>()
    var used = 0

    for (p in paragraphs) {
        val len = p.length + 1
        if (used + len > charsPerPage && current.isNotEmpty()) {
            pages.add(current)
            current = mutableListOf()
            used = 0
        }
        // 单段超过一页容量：硬切
        if (len > charsPerPage && current.isEmpty()) {
            p.chunked(charsPerPage).forEach { chunk ->
                pages.add(mutableListOf(chunk))
            }
            current = mutableListOf()
            used = 0
        } else {
            current.add(p)
            used += len
        }
    }
    if (current.isNotEmpty()) pages.add(current)
    return if (pages.isEmpty()) listOf(emptyList()) else pages
}

/** 把章节正文按换行拆成段落 */
fun toParagraphs(content: String): List<String> =
    content.split(Regex("\n+")).map { it.trim() }.filter { it.isNotEmpty() }
