package com.onigiri.spend.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.Receipt
import com.onigiri.spend.data.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val receipt: Receipt? = null,
    val deleting: Boolean = false,
)

class ReceiptDetailViewModel(
    private val receiptId: String,
    private val apiClient: ApiClient,
) : ViewModel() {
    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { apiClient.api().getReceipt(receiptId) }
                .onSuccess { receipt -> _state.update { it.copy(loading = false, receipt = receipt) } }
                .onFailure { error -> _state.update { it.copy(loading = false, error = error.userMessage()) } }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(deleting = true, error = null) }
            runCatching { apiClient.api().deleteReceipt(receiptId) }
                .onSuccess { onDeleted() }
                .onFailure { error ->
                    _state.update { it.copy(deleting = false, error = error.userMessage()) }
                }
        }
    }

    companion object {
        fun factory(receiptId: String, apiClient: ApiClient) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReceiptDetailViewModel(receiptId, apiClient) as T
            }
        }
    }
}
