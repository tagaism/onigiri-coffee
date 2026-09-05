package com.onigiri.spend.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    @SerialName("default_currency") val defaultCurrency: String,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    val user: User,
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    @SerialName("default_currency") val defaultCurrency: String? = null,
)

@Serializable
data class LineItem(
    val id: String? = null,
    val name: String,
    val quantity: String,
    @SerialName("unit_price") val unitPrice: String? = null,
    val amount: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
)

@Serializable
data class LineItemIn(
    val name: String,
    val quantity: String = "1",
    @SerialName("unit_price") val unitPrice: String? = null,
    val amount: String? = null,
)

@Serializable
data class Receipt(
    val id: String,
    @SerialName("merchant_name") val merchantName: String,
    @SerialName("purchased_at") val purchasedAt: String,
    val currency: String,
    val tax: String,
    val total: String,
    @SerialName("computed_total") val computedTotal: String,
    @SerialName("total_mismatch") val totalMismatch: Boolean,
    val notes: String? = null,
    val items: List<LineItem> = emptyList(),
)

@Serializable
data class ReceiptListItem(
    val id: String,
    @SerialName("merchant_name") val merchantName: String,
    @SerialName("purchased_at") val purchasedAt: String,
    val currency: String,
    val tax: String,
    val total: String,
    @SerialName("item_count") val itemCount: Int,
)

@Serializable
data class ReceiptIn(
    @SerialName("merchant_name") val merchantName: String,
    @SerialName("purchased_at") val purchasedAt: String,
    val currency: String? = null,
    val tax: String = "0",
    val total: String? = null,
    val notes: String? = null,
    val items: List<LineItemIn>,
)

@Serializable
data class DayTotal(
    val date: String,
    val count: Int,
    val total: String,
)

@Serializable
data class Summary(
    @SerialName("from") val fromDate: String,
    @SerialName("to") val toDate: String,
    @SerialName("receipt_count") val receiptCount: Int,
    val total: String,
    @SerialName("by_day") val byDay: List<DayTotal> = emptyList(),
)

@Serializable
data class ApiError(
    val detail: String? = null,
)
