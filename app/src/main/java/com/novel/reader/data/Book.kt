package com.novel.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍实体。
 * chaptersJson：解析后的章节列表（标题 + 内容）以 JSON 字符串存储。
 * ⚠️ 该字段可能很大（整本小说几十 MB），书架列表查询时必须用 [BookSummary] 投影，避免一次性载入。
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "未知",
    val format: String,            // "TXT" / "EPUB"
    val coverPath: String? = null,
    val chaptersJson: String,      // List<Chapter> 序列化（大字段，按需加载）
    val totalChars: Long = 0L,     // 全书总字数
    val lastChapterIndex: Int = 0,
    val lastCharOffset: Int = 0,
    val lastReadTime: Long = System.currentTimeMillis(),
    val addedTime: Long = System.currentTimeMillis()
)

/**
 * 书架列表用的轻量投影：不含 chaptersJson。
 * 防止书架页一次性把整本小说正文拉进内存导致 OOM 闪退。
 */
data class BookSummary(
    val id: Long,
    val title: String,
    val author: String,
    val format: String,
    val totalChars: Long,
    val lastChapterIndex: Int,
    val lastReadTime: Long
)

data class Chapter(
    val title: String,
    val content: String
)
