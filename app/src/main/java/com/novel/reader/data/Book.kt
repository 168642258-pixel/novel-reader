package com.novel.reader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 书籍实体。
 * chaptersJson：解析后的章节列表（标题 + 内容）以 JSON 字符串存储，避免多表查询开销，适合轻量阅读器。
 */
@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String = "未知",
    val format: String,            // "TXT" / "EPUB"
    val coverPath: String? = null,
    val chaptersJson: String,      // List<Chapter> 序列化
    val totalChars: Long = 0L,     // 全书总字数（用于进度估算）
    val lastChapterIndex: Int = 0, // 上次阅读到第几章
    val lastCharOffset: Int = 0,   // 章节内字符偏移
    val lastReadTime: Long = System.currentTimeMillis(),
    val addedTime: Long = System.currentTimeMillis()
)

data class Chapter(
    val title: String,
    val content: String
)
