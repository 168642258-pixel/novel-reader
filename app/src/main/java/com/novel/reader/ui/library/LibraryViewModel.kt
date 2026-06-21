package com.novel.reader.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.BookRepository
import com.novel.reader.data.BookSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImportState(val importing: Boolean = false, val message: String? = null)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BookRepository(app)

    /** 书架只加载元信息（不含整本章节内容，避免 OOM） */
    val books: StateFlow<List<BookSummary>> = repo.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importState = MutableStateFlow(ImportState())
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun import(uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportState(importing = true)
            try {
                repo.importFromUri(uri)
                _importState.value = ImportState(importing = false, message = "导入成功")
            } catch (e: Exception) {
                _importState.value = ImportState(importing = false, message = "导入失败：${e.message}")
            }
        }
    }

    fun consumeMessage() {
        _importState.value = _importState.value.copy(message = null)
    }

    fun delete(book: BookSummary) {
        // 删除只需 id，构造一个轻量 Book 传给 dao.delete（dao 用 @Delete 按 PK 删）
        viewModelScope.launch { repo.deleteById(book.id) }
    }
}
