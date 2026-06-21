package com.novel.reader.ui.reader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.Book
import com.novel.reader.data.BookRepository
import com.novel.reader.data.Chapter
import com.novel.reader.data.ReadingPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Error(val message: String) : ReaderUiState
    data class Ready(
        val book: Book,
        val chapters: List<Chapter>,
        val chapterIndex: Int
    ) : ReaderUiState
}

class ReaderViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BookRepository(app)
    val prefs = ReadingPrefs(app)

    private val _state = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private var currentBook: Book? = null
    private var chapters: List<Chapter> = emptyList()

    fun load(bookId: Long) {
        viewModelScope.launch {
            try {
                val book = repo.getById(bookId) ?: run {
                    _state.value = ReaderUiState.Error("书籍不存在")
                    return@launch
                }
                currentBook = book
                chapters = repo.chaptersFromJson(book.chaptersJson)
                _state.value = ReaderUiState.Ready(book, chapters, book.lastChapterIndex.coerceIn(0, chapters.lastIndex))
            } catch (e: Exception) {
                _state.value = ReaderUiState.Error(e.message ?: "加载失败")
            }
        }
    }

    fun setChapter(index: Int) {
        val ready = _state.value as? ReaderUiState.Ready ?: return
        val safe = index.coerceIn(0, ready.chapters.lastIndex)
        if (safe == ready.chapterIndex) return
        _state.value = ready.copy(chapterIndex = safe)
        saveProgress(safe, 0)
    }

    /** 下一章，返回是否还有下一章 */
    fun nextChapter(): Boolean {
        val ready = _state.value as? ReaderUiState.Ready ?: return false
        return if (ready.chapterIndex < ready.chapters.lastIndex) {
            setChapter(ready.chapterIndex + 1); true
        } else false
    }

    fun prevChapter(): Boolean {
        val ready = _state.value as? ReaderUiState.Ready ?: return false
        return if (ready.chapterIndex > 0) {
            setChapter(ready.chapterIndex - 1); true
        } else false
    }

    fun saveProgress(chapter: Int, offset: Int) {
        val book = currentBook ?: return
        viewModelScope.launch { repo.updateProgress(book.id, chapter, offset) }
    }
}
