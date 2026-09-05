package com.onigiri.spend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.ReceiptListItem
import com.onigiri.spend.data.Summary
import com.onigiri.spend.data.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val summary: Summary? = null,
    val receipts: List<ReceiptListItem> = emptyList(),
    val currency: String = "JPY",
)

class HomeViewModel(private val apiClient: ApiClient) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val month = YearMonth.now()
            val from = month.atDay(1).toString()
            val to = month.atEndOfMonth().toString()
            runCatching {
                val api = apiClient.api()
                val summary = api.summary(from, to)
                val receipts = api.listReceipts(from, to)
                summary to receipts
            }.onSuccess { (summary, receipts) ->
                _state.update {
                    it.copy(
                        loading = false,
                        summary = summary,
                        receipts = receipts,
                        currency = receipts.firstOrNull()?.currency ?: "JPY",
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    companion object {
        fun factory(apiClient: ApiClient) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(apiClient) as T
            }
        }
    }
}
