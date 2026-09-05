package com.onigiri.spend.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onigiri.spend.data.ApiClient
import com.onigiri.spend.data.LineItemIn
import com.onigiri.spend.data.ReceiptIn
import com.onigiri.spend.data.SessionStore
import com.onigiri.spend.data.userMessage
import com.onigiri.spend.ui.computeLineAmount
import com.onigiri.spend.ui.parseMoney
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class ItemDraft(
    val key: String = UUID.randomUUID().toString(),
    val name: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
    val amount: String = "",
)

data class ReceiptFormState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val merchantName: String = "",
    val purchasedAt: String = LocalDate.now().toString(),
    val currency: String = "JPY",
    val tax: String = "0",
    val notes: String = "",
    val items: List<ItemDraft> = listOf(ItemDraft()),
    val isEdit: Boolean = false,
)

class ReceiptFormViewModel(
    private val receiptId: String?,
    private val apiClient: ApiClient,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ReceiptFormState(isEdit = receiptId != null))
    val state: StateFlow<ReceiptFormState> = _state

    init {
        viewModelScope.launch {
            val currency = session.currency.first()
            _state.update { it.copy(currency = currency) }
            if (receiptId != null) load(receiptId)
        }
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { apiClient.api().getReceipt(id) }
                .onSuccess { receipt ->
                    _state.update {
                        it.copy(
                            loading = false,
                            merchantName = receipt.merchantName,
                            purchasedAt = receipt.purchasedAt,
                            currency = receipt.currency,
                            tax = receipt.tax,
                            notes = receipt.notes.orEmpty(),
                            items = receipt.items.map { item ->
                                ItemDraft(
                                    name = item.name,
                                    quantity = item.quantity,
                                    unitPrice = item.unitPrice.orEmpty(),
                                    amount = item.amount,
                                )
                            }.ifEmpty { listOf(ItemDraft()) },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.userMessage()) }
                }
        }
    }

    fun updateMerchant(value: String) = _state.update { it.copy(merchantName = value, error = null) }
    fun updateDate(value: String) = _state.update { it.copy(purchasedAt = value, error = null) }
    fun updateTax(value: String) = _state.update { it.copy(tax = value, error = null) }
    fun updateNotes(value: String) = _state.update { it.copy(notes = value) }

    fun updateItem(key: String, transform: (ItemDraft) -> ItemDraft) {
        _state.update { state ->
            state.copy(items = state.items.map { if (it.key == key) transform(it) else it }, error = null)
        }
    }

    fun addItem() = _state.update { it.copy(items = it.items + ItemDraft()) }

    fun removeItem(key: String) {
        _state.update { state ->
            val remaining = state.items.filterNot { it.key == key }
            state.copy(items = remaining.ifEmpty { listOf(ItemDraft()) })
        }
    }

    fun computedTotal(): BigDecimal {
        val current = _state.value
        val items = current.items.fold(BigDecimal.ZERO) { acc, item ->
            acc + computeLineAmount(item.quantity, item.unitPrice, item.amount)
        }
        return items + (parseMoney(current.tax) ?: BigDecimal.ZERO)
    }

    fun save(onSaved: (String) -> Unit) {
        val current = _state.value
        if (current.merchantName.isBlank()) {
            _state.update { it.copy(error = "Merchant is required.") }
            return
        }
        val items = current.items.mapNotNull { item ->
            if (item.name.isBlank()) return@mapNotNull null
            val amount = computeLineAmount(item.quantity, item.unitPrice, item.amount)
            LineItemIn(
                name = item.name.trim(),
                quantity = item.quantity.ifBlank { "1" },
                unitPrice = item.unitPrice.ifBlank { null },
                amount = amount.toPlainString(),
            )
        }
        if (items.isEmpty()) {
            _state.update { it.copy(error = "Add at least one item with a name.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            val body = ReceiptIn(
                merchantName = current.merchantName.trim(),
                purchasedAt = current.purchasedAt,
                currency = current.currency,
                tax = (parseMoney(current.tax) ?: BigDecimal.ZERO).toPlainString(),
                notes = current.notes.ifBlank { null },
                items = items,
            )
            runCatching {
                val api = apiClient.api()
                if (receiptId == null) api.createReceipt(body) else api.updateReceipt(receiptId, body)
            }.onSuccess { receipt ->
                _state.update { it.copy(saving = false) }
                onSaved(receipt.id)
            }.onFailure { error ->
                _state.update { it.copy(saving = false, error = error.userMessage()) }
            }
        }
    }

    companion object {
        fun factory(receiptId: String?, apiClient: ApiClient, session: SessionStore) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReceiptFormViewModel(receiptId, apiClient, session) as T
                }
            }
    }
}
