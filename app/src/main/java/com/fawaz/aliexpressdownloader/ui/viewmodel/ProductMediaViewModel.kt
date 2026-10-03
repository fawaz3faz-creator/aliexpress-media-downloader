package com.fawaz.aliexpressdownloader.ui.viewmodel

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fawaz.aliexpressdownloader.data.repository.AliExpressRepository
import com.fawaz.aliexpressdownloader.model.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductMediaUiState(
    val url: String = "",
    val mediaList: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectionCount: Int = 0,
    val allSelected: Boolean = false
)

class ProductMediaViewModel(
    private val repository: AliExpressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductMediaUiState())
    val uiState: StateFlow<ProductMediaUiState> = _uiState.asStateFlow()

    fun setUrl(value: String) {
        _uiState.update { it.copy(url = value.trim()) }
    }

    fun pasteFromClipboard(context: Context) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
        if (!clipText.isNullOrBlank()) {
            setUrl(clipText)
        }
    }

    fun fetchMedia() {
        val rawUrl = _uiState.value.url
        if (rawUrl.isBlank()) {
            _uiState.update { it.copy(error = "Please enter an AliExpress product URL") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    mediaList = emptyList(),
                    selectionCount = 0,
                    allSelected = false
                )
            }

            try {
                val media = repository.fetchMedia(rawUrl)
                _uiState.update {
                    it.copy(
                        mediaList = media,
                        isLoading = false,
                        error = null,
                        selectionCount = if (media.isNotEmpty()) media.count { item -> item.selected } else 0,
                        allSelected = media.isNotEmpty() && media.all { item -> item.selected }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unable to parse product media. Check the URL and try again."
                    )
                }
            }
        }
    }

    fun toggleSelection(itemId: String) {
        _uiState.update { state ->
            val updated = state.mediaList.map { item ->
                if (item.id == itemId) item.copy(selected = !item.selected) else item
            }
            state.copy(
                mediaList = updated,
                selectionCount = updated.count { it.selected },
                allSelected = updated.isNotEmpty() && updated.all { it.selected }
            )
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            val shouldSelectAll = !state.allSelected
            val updated = state.mediaList.map { it.copy(selected = shouldSelectAll) }
            state.copy(
                mediaList = updated,
                selectionCount = if (shouldSelectAll) updated.size else 0,
                allSelected = shouldSelectAll
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun selectedMedia(): List<MediaItem> = _uiState.value.mediaList.filter { it.selected }

    fun allMedia(): List<MediaItem> = _uiState.value.mediaList

    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = AliExpressRepository(
                    api = AliExpressApiService.build(context),
                    okHttpClient = AliExpressRepository.createClient()
                )
                return ProductMediaViewModel(repository) as T
            }
        }
    }
}
