package com.onigiri.spend.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun parseMoney(raw: String): BigDecimal? {
    val trimmed = raw.trim().replace(",", "")
    if (trimmed.isEmpty()) return null
    return runCatching { BigDecimal(trimmed) }.getOrNull()
}

fun formatMoney(amount: String, currency: String): String {
    val value = parseMoney(amount) ?: return amount
    return formatMoney(value, currency)
}

fun formatMoney(amount: BigDecimal, currency: String): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { format.currency = Currency.getInstance(currency) }
    format.maximumFractionDigits = if (currency == "JPY") 0 else 2
    format.minimumFractionDigits = if (currency == "JPY") 0 else 2
    return format.format(amount)
}

fun computeLineAmount(quantity: String, unitPrice: String, amount: String): BigDecimal {
    parseMoney(amount)?.let { return it.setScale(2, RoundingMode.HALF_UP) }
    val qty = parseMoney(quantity) ?: BigDecimal.ONE
    val price = parseMoney(unitPrice) ?: BigDecimal.ZERO
    return qty.multiply(price).setScale(2, RoundingMode.HALF_UP)
}
