package com.novel.reader.data

import android.content.Context
import android.net.Uri
import com.novel.reader.parser.BookParseResult
import com.novel.reader.parser.EpubParser
import com.novel.reader.parser.TxtParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BookRepository(private val context: Context) {

    private val dao = AppDatabase.get(context).bookDao()

    fun observeSummaries(): Flow<List<BookSummary>> = dao.observeSummaries()

    suspend fun getById(id: Long): Book? = withContext(Dispatchers.IO) { dao.getById(id) }

    /** 从 Uri 导入书籍：解析 -> 存库 -> 返回 id */
    suspend fun importFromUri(uri: Uri): Long = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri)
        val name = queryDisplayName(uri)
        val isEpub = mime == "application/epub+zip" ||
            name.endsWith(".epub", ignoreCase = true)

        val result: BookParseResult = context.contentResolver.openInputStream(uri).use { stream ->
            requireNotNull(stream) { "无法打开文件" }
            if (isEpub) EpubParser.parse(stream) else TxtParser.parse(stream, name)
        }

        val book = Book(
            title = result.title,
            author = result.author,
            format = if (isEpub) "EPUB" else "TXT",
            coverPath = result.coverPath,
            chaptersJson = chaptersToJson(result.chapters),
            totalChars = result.chapters.sumOf { it.content.length }.toLong()
        )
        dao.insert(book)
    }

    suspend fun updateProgress(id: Long, chapter: Int, offset: Int) =
        withContext(Dispatchers.IO) { dao.updateProgress(id, chapter, offset, System.currentTimeMillis()) }

    suspend fun delete(book: Book) = withContext(Dispatchers.IO) { dao.delete(book) }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        // 先取出来再 delete（dao.delete 需要实体），或直接 SQL 删
        dao.getById(id)?.let { dao.delete(it) }
    }

    // -------- 章节 JSON 序列化（轻量、无第三方依赖）--------
    fun chaptersToJson(chapters: List<Chapter>): String {
        val arr = JSONArray()
        chapters.forEach { c ->
            arr.put(JSONObject().apply {
                put("title", c.title)
                put("content", c.content)
            })
        }
        return arr.toString()
    }

    fun chaptersFromJson(json: String): List<Chapter> {
        val arr = JSONArray(json)
        val list = ArrayList<Chapter>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(Chapter(o.optString("title", "第${i + 1}章"), o.optString("content", "")))
        }
        return list
    }

    private fun queryDisplayName(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return "未知.txt"
        return cursor.use {
            val idx = it.getColumnIndex("_display_name")
            if (idx >= 0 && it.moveToFirst()) it.getString(idx) else "未知.txt"
        }
    }
}
