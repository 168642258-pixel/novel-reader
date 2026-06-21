package com.novel.reader.parser

import com.novel.reader.data.Chapter

data class BookParseResult(
    val title: String,
    val author: String,
    val coverPath: String? = null,
    val chapters: List<Chapter>
)
