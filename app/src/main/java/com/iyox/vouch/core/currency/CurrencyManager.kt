package com.iyox.vouch.core.currency

import android.content.Context
import android.content.SharedPreferences
import android.icu.util.Currency
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

object CurrencyManager {
    val supportedCurrencies: List<Currency> = listOf(
        Currency.getInstance("EUR"),
        Currency.getInstance("USD"),
        Currency.getInstance("GBP"),
        Currency.getInstance("JPY"),
        Currency.getInstance("CHF"),
        Currency.getInstance("CAD"),
        Currency.getInstance("AUD"),
        Currency.getInstance("CNY"),
        Currency.getInstance("INR")
    )

    private var prefs: SharedPreferences? = null
    private val _currencyFlow = MutableStateFlow<Currency>(Currency.getInstance("EUR"))
    val currencyFlow: StateFlow<Currency> = _currencyFlow.asStateFlow()

    fun getDefaultCurrency(): Currency = resolveDefaultCurrency()

    private fun resolveDefaultCurrency(): Currency {
        return try {
            val systemLocale = try {
                val locales = android.content.res.Resources.getSystem().configuration.locales
                if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
            } catch (_: Exception) {
                Locale.getDefault()
            }

            val candidateLocale = if (systemLocale.country.isNotBlank()) {
                systemLocale
            } else if (Locale.getDefault().country.isNotBlank()) {
                Locale.getDefault()
            } else {
                null
            }

            val localeCurrency = candidateLocale?.let {
                try { Currency.getInstance(it) } catch (_: Exception) { null }
            }

            if (localeCurrency != null && supportedCurrencies.any { it.currencyCode == localeCurrency.currencyCode }) {
                localeCurrency
            } else {
                Currency.getInstance("EUR")
            }
        } catch (_: Exception) {
            Currency.getInstance("EUR")
        }
    }

    private fun getInitialCurrency(): Currency {
        val code = prefs?.getString("currency_code", null)
        if (!code.isNullOrBlank()) {
            return try {
                Currency.getInstance(code)
            } catch (_: Exception) {
                resolveDefaultCurrency()
            }
        }
        val defaultCurrency = resolveDefaultCurrency()
        prefs?.edit { putString("currency_code", defaultCurrency.currencyCode) }
        return defaultCurrency
    }

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
            _currencyFlow.value = getInitialCurrency()
        }
    }

    fun getCurrency(): Currency = _currencyFlow.value

    fun setCurrency(currencyCode: String) {
        val newCurrency = try {
            Currency.getInstance(currencyCode)
        } catch (_: Exception) {
            Currency.getInstance("EUR")
        }
        prefs?.edit { putString("currency_code", newCurrency.currencyCode) }
        _currencyFlow.value = newCurrency
    }

    fun currencyFlow(): StateFlow<Currency> = currencyFlow

    fun getDecimalSeparator(locale: Locale = Locale.getDefault()): Char {
        return DecimalFormatSymbols.getInstance(locale).decimalSeparator
    }

    fun getMinorUnitFactor(currency: Currency = getCurrency()): Int {
        val fractionDigits = maxOf(0, currency.defaultFractionDigits)
        var factor = 1
        repeat(fractionDigits) { factor *= 10 }
        return factor
    }

    fun formatAmount(
        minorUnits: Long,
        currency: Currency = getCurrency(),
        locale: Locale = Locale.getDefault()
    ): String {
        val fractionDigits = maxOf(0, currency.defaultFractionDigits)
        val isNegative = minorUnits < 0
        val absVal = abs(minorUnits)
        val factor = getMinorUnitFactor(currency)
        val whole = absVal / factor
        val remainder = absVal % factor
        val decimalSeparator = getDecimalSeparator(locale)
        val formatted = if (fractionDigits > 0) {
            val fractionStr = remainder.toString().padStart(fractionDigits, '0')
            "$whole$decimalSeparator$fractionStr"
        } else {
            whole.toString()
        }
        return if (isNegative) "-$formatted" else formatted
    }

    fun formatAmountWithSymbol(
        minorUnits: Long,
        currency: Currency = getCurrency(),
        locale: Locale = Locale.getDefault()
    ): String {
        val formatted = formatAmount(minorUnits, currency, locale)
        val symbol = currency.getSymbol(locale)
        val isSymbolPrefix = locale.language.equals("en", ignoreCase = true)
        return if (isSymbolPrefix) {
            if (formatted.startsWith("-")) {
                "-$symbol${formatted.removePrefix("-")}"
            } else {
                "$symbol$formatted"
            }
        } else {
            "$formatted $symbol"
        }
    }

    fun parseToMinorUnits(
        userInput: String,
        currency: Currency = getCurrency(),
        locale: Locale = Locale.getDefault()
    ): Long? {
        val decimalSeparator = getDecimalSeparator(locale)
        val trimmed = userInput.trim()
            .replace(decimalSeparator, '.')
            .replace(',', '.')
        if (trimmed.isBlank()) return null

        val fractionDigits = maxOf(0, currency.defaultFractionDigits)
        val regex = if (fractionDigits > 0) {
            Regex("^\\d+([.]\\d{1,$fractionDigits})?$")
        } else {
            Regex("^\\d+$")
        }
        if (!regex.matches(trimmed)) return null

        val factor = getMinorUnitFactor(currency)
        return if (trimmed.contains('.')) {
            val parts = trimmed.split('.')
            val whole = parts[0].toLongOrNull() ?: return null
            val paddedFraction = parts[1].padEnd(fractionDigits, '0')
            val fraction = paddedFraction.toLongOrNull() ?: return null
            whole * factor + fraction
        } else {
            val whole = trimmed.toLongOrNull() ?: return null
            whole * factor
        }
    }
}
