package com.iyox.vouch.core.currency

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.icu.text.NumberFormat
import android.icu.util.Currency
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import com.iyox.vouch.core.utils.PreferenceKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

sealed interface CurrencyGridSlot {
    data class CurrencyItem(val currency: Currency) : CurrencyGridSlot
    data object SearchButton : CurrencyGridSlot
}

@Singleton
class CurrencyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PreferenceKeys.PREFS_SETTINGS, Context.MODE_PRIVATE)

    private val _currencyFlow = MutableStateFlow(getInitialCurrency())
    val currencyFlow: StateFlow<Currency> = _currencyFlow.asStateFlow()

    private val _currencyState: MutableState<Currency> = mutableStateOf(_currencyFlow.value)

    init {
        INSTANCE = this
        fallbackCurrencyFlow.value = _currencyFlow.value
        fallbackCurrencyState.value = _currencyState.value
    }

    private fun getInitialCurrency(): Currency {
        val code = prefs.getString(PreferenceKeys.KEY_CURRENCY_CODE, null)
        if (!code.isNullOrBlank()) {
            return try {
                Currency.getInstance(code)
            } catch (_: Exception) {
                resolveDefaultCurrency()
            }
        }
        val defaultCurrency = resolveDefaultCurrency()
        prefs.edit { putString(PreferenceKeys.KEY_CURRENCY_CODE, defaultCurrency.currencyCode) }
        return defaultCurrency
    }

    fun getCurrency(): Currency = _currencyState.value

    fun setCurrency(currencyCode: String) {
        val newCurrency = try {
            Currency.getInstance(currencyCode)
        } catch (_: Exception) {
            Currency.getInstance("EUR")
        }
        prefs.edit { putString(PreferenceKeys.KEY_CURRENCY_CODE, newCurrency.currencyCode) }
        _currencyState.value = newCurrency
        _currencyFlow.value = newCurrency
        fallbackCurrencyFlow.value = newCurrency
        fallbackCurrencyState.value = newCurrency
    }

    fun formatAmount(
        minorUnits: Long,
        currency: Currency = getCurrency(),
        locale: Locale = Locale.getDefault()
    ): String = Companion.formatAmount(minorUnits, currency, locale)

    fun formatAmountWithSymbol(
        minorUnits: Long,
        currency: Currency = getCurrency(),
        locale: Locale = Locale.getDefault()
    ): String = Companion.formatAmountWithSymbol(minorUnits, currency, locale)

    fun parseToMinorUnits(
        userInput: String,
        currency: Currency = getCurrency(),
        locale: Locale = Locale.getDefault()
    ): Long? = Companion.parseToMinorUnits(userInput, currency, locale)

    companion object {
        @Volatile
        private var INSTANCE: CurrencyManager? = null

        private val fallbackCurrencyFlow: MutableStateFlow<Currency> = MutableStateFlow(resolveDefaultCurrency())
        private val fallbackCurrencyState: MutableState<Currency> = mutableStateOf(resolveDefaultCurrency())

        val popularCurrencies: List<Currency> = listOf(
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

        val allCurrencies: List<Currency> by lazy {
            try {
                val available = Currency.getAvailableCurrencies()
                val list = available
                    .filter { currency ->
                        val code = currency.currencyCode
                        code != null && code.length == 3 && code.all { it.isLetter() } && code != "XXX"
                    }
                    .sortedBy { it.currencyCode }
                list.ifEmpty { popularCurrencies }
            } catch (_: Exception) {
                popularCurrencies
            }
        }

        val supportedCurrencies: List<Currency>
            get() = allCurrencies

        fun getDefaultCurrency(): Currency = resolveDefaultCurrency()

        fun resolveDefaultCurrency(): Currency {
            return try {
                val systemLocale = try {
                    val locales = Resources.getSystem().configuration.locales
                    if (!locales.isEmpty) locales[0] else Locale.getDefault()
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

                localeCurrency ?: Currency.getInstance("EUR")
            } catch (_: Exception) {
                Currency.getInstance("EUR")
            }
        }

        fun getQuickCurrencySlots(
            localeCurrency: Currency = resolveDefaultCurrency()
        ): List<CurrencyGridSlot> {
            val otherCurrencies = popularCurrencies
                .filter { it.currencyCode != localeCurrency.currencyCode }
                .toMutableList()

            if (otherCurrencies.size < 7) {
                for (curr in allCurrencies) {
                    if (curr.currencyCode != localeCurrency.currencyCode &&
                        otherCurrencies.none { it.currencyCode == curr.currencyCode }
                    ) {
                        otherCurrencies.add(curr)
                    }
                    if (otherCurrencies.size == 7) break
                }
            }

            return buildList {
                add(CurrencyGridSlot.CurrencyItem(localeCurrency))
                addAll(otherCurrencies.take(7).map { CurrencyGridSlot.CurrencyItem(it) })
                add(CurrencyGridSlot.SearchButton)
            }
        }

        fun getCurrency(): Currency =
            INSTANCE?.getCurrency() ?: fallbackCurrencyState.value

        fun setCurrency(currencyCode: String) {
            val newCurrency = try {
                Currency.getInstance(currencyCode)
            } catch (_: Exception) {
                Currency.getInstance("EUR")
            }
            val instance = INSTANCE
            if (instance != null) {
                instance.setCurrency(currencyCode)
            } else {
                fallbackCurrencyFlow.value = newCurrency
                fallbackCurrencyState.value = newCurrency
            }
        }

        val currencyFlow: StateFlow<Currency>
            get() = INSTANCE?.currencyFlow ?: fallbackCurrencyFlow.asStateFlow()

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
            val factor = getMinorUnitFactor(currency)
            val doubleValue = minorUnits.toDouble() / factor
            val formatter = NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                minimumFractionDigits = currency.defaultFractionDigits
                maximumFractionDigits = currency.defaultFractionDigits
            }
            return formatter.format(doubleValue)
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
}
