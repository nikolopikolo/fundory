package com.fundory.app.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class SupportedCurrency(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String
)

object Currencies {
    val GEL = SupportedCurrency("GEL", "Georgian Lari", "₾", "🇬🇪")
    val USD = SupportedCurrency("USD", "US Dollar", "$", "🇺🇸")
    val EUR = SupportedCurrency("EUR", "Euro", "€", "🇪🇺")
    val CNY = SupportedCurrency("CNY", "Chinese Yuan", "¥", "🇨🇳")
    val GBP = SupportedCurrency("GBP", "British Pound", "£", "🇬🇧")
    val JPY = SupportedCurrency("JPY", "Japanese Yen", "¥", "🇯🇵")
    val TRY = SupportedCurrency("TRY", "Turkish Lira", "₺", "🇹🇷")

    val ALL = listOf(GEL, USD, EUR, CNY, GBP, JPY, TRY)

    fun find(code: String): SupportedCurrency {
        return ALL.find { it.code.equals(code, ignoreCase = true) }
            ?: SupportedCurrency(code.uppercase(), code.uppercase(), code.uppercase(), "🌐")
    }

    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    private val decimalFormat = DecimalFormat("#,##0.00", symbols)

    fun format(amount: BigDecimal, currencyCode: String = "GEL", includeCode: Boolean = true): String {
        val formatted = decimalFormat.format(amount.setScale(2, RoundingMode.HALF_UP))
        val curr = find(currencyCode)
        return if (currencyCode == "GEL") {
            if (includeCode) "$formatted GEL" else "$formatted ₾"
        } else {
            if (includeCode) "$formatted $currencyCode" else "${curr.symbol}$formatted"
        }
    }

    fun parseAmount(input: String): BigDecimal? {
        val clean = input.filter { it.isDigit() || it == '.' || it == '-' }.trim()
        return try {
            if (clean.isBlank()) null else BigDecimal(clean).setScale(2, RoundingMode.HALF_UP)
        } catch (_: Exception) {
            null
        }
    }
}

