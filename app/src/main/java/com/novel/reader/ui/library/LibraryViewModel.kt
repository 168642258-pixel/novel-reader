package com.novel.reader.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.novel.reader.data.Book
import com.novel.reader.data.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ImportState(val importing: Boolean = false, val message: String? = null)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = BookRepository(app)

    val books: StateFlow<List<Book>> = repo.observeAll()
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

    fun delete(book: Book) {
        viewModelScope.launch { repo.delete(book) }
    }

    fun repository(): BookRepository = repo
}
