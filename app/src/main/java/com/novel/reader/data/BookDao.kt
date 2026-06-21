package com.novel.reader.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    /** 书架列表：只查元信息，不加载 chaptersJson（避免大字段导致 OOM） */
    @Query("""
        SELECT id, title, author, format, totalChars,
               lastChapterIndex, lastReadTime
        FROM books ORDER BY lastReadTime DESC
    """)
    fun observeSummaries(): Flow<List<BookSummary>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): Book?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: Book): Long

    @Update
    suspend fun update(book: Book)

    @Delete
    suspend fun delete(book: Book)

    @Query("UPDATE books SET lastChapterIndex = :chapter, lastCharOffset = :offset, lastReadTime = :time WHERE id = :id")
    suspend fun updateProgress(id: Long, chapter: Int, offset: Int, time: Long)
}
