package com.iyox.vouch.core.currency

import android.icu.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CurrencyManagerTest {

    private val eur = Currency.getInstance("EUR")
    private val usd = Currency.getInstance("USD")
    private val jpy = Currency.getInstance("JPY")
    private val gbp = Currency.getInstance("GBP")

    @Test
    fun testGetMinorUnitFactor() {
        assertEquals(100, CurrencyManager.getMinorUnitFactor(eur))
        assertEquals(100, CurrencyManager.getMinorUnitFactor(usd))
        assertEquals(1, CurrencyManager.getMinorUnitFactor(jpy))
        assertEquals(100, CurrencyManager.getMinorUnitFactor(gbp))
    }

    @Test
    fun testFormatAmount_twoDecimals() {
        val usLocale = Locale.US

        assertEquals("0.00", CurrencyManager.formatAmount(0L, eur, usLocale))
        assertEquals("0.05", CurrencyManager.formatAmount(5L, eur, usLocale))
        assertEquals("0.50", CurrencyManager.formatAmount(50L, eur, usLocale))
        assertEquals("1.00", CurrencyManager.formatAmount(100L, eur, usLocale))
        assertEquals("10.50", CurrencyManager.formatAmount(1050L, eur, usLocale))
        assertEquals("12345.67", CurrencyManager.formatAmount(1234567L, eur, usLocale))

        // Negative numbers
        assertEquals("-0.05", CurrencyManager.formatAmount(-5L, eur, usLocale))
        assertEquals("-10.50", CurrencyManager.formatAmount(-1050L, eur, usLocale))
    }

    @Test
    fun testFormatAmount_zeroDecimals() {
        val usLocale = Locale.US

        assertEquals("0", CurrencyManager.formatAmount(0L, jpy, usLocale))
        assertEquals("5", CurrencyManager.formatAmount(5L, jpy, usLocale))
        assertEquals("1000", CurrencyManager.formatAmount(1000L, jpy, usLocale))
        assertEquals("-500", CurrencyManager.formatAmount(-500L, jpy, usLocale))
    }

    @Test
    fun testFormatAmount_germanLocale() {
        val deLocale = Locale.GERMANY

        assertEquals("0,00", CurrencyManager.formatAmount(0L, eur, deLocale))
        assertEquals("10,50", CurrencyManager.formatAmount(1050L, eur, deLocale))
        assertEquals("-10,50", CurrencyManager.formatAmount(-1050L, eur, deLocale))
    }

    @Test
    fun testFormatAmountWithSymbol_englishPrefix() {
        val usLocale = Locale.US

        val formattedPos = CurrencyManager.formatAmountWithSymbol(1050L, usd, usLocale)
        assertTrue(formattedPos.startsWith("$") || formattedPos.startsWith("USD"))
        assertTrue(formattedPos.contains("10.50"))

        val formattedNeg = CurrencyManager.formatAmountWithSymbol(-1050L, usd, usLocale)
        assertTrue(formattedNeg.startsWith("-$") || formattedNeg.startsWith("-USD"))
        assertTrue(formattedNeg.contains("10.50"))
    }

    @Test
    fun testFormatAmountWithSymbol_germanSuffix() {
        val deLocale = Locale.GERMANY

        val formattedPos = CurrencyManager.formatAmountWithSymbol(1050L, eur, deLocale)
        assertTrue(formattedPos.endsWith("€") || formattedPos.endsWith("EUR"))
        assertTrue(formattedPos.contains("10,50"))

        val formattedNeg = CurrencyManager.formatAmountWithSymbol(-1050L, eur, deLocale)
        assertTrue(formattedNeg.startsWith("-"))
        assertTrue(formattedNeg.endsWith("€") || formattedNeg.endsWith("EUR"))
        assertTrue(formattedNeg.contains("10,50"))
    }

    @Test
    fun testParseToMinorUnits_twoDecimals() {
        val usLocale = Locale.US

        assertEquals(1050L, CurrencyManager.parseToMinorUnits("10.50", eur, usLocale))
        assertEquals(1050L, CurrencyManager.parseToMinorUnits("10.5", eur, usLocale))
        assertEquals(1000L, CurrencyManager.parseToMinorUnits("10", eur, usLocale))
        assertEquals(5L, CurrencyManager.parseToMinorUnits("0.05", eur, usLocale))
        assertEquals(50L, CurrencyManager.parseToMinorUnits("0.5", eur, usLocale))
        assertEquals(0L, CurrencyManager.parseToMinorUnits("0", eur, usLocale))
        assertEquals(0L, CurrencyManager.parseToMinorUnits("0.00", eur, usLocale))

        // Comma decimal separator in input is normalized
        assertEquals(1050L, CurrencyManager.parseToMinorUnits("10,50", eur, usLocale))

        // German locale with comma
        val deLocale = Locale.GERMANY
        assertEquals(1050L, CurrencyManager.parseToMinorUnits("10,50", eur, deLocale))
        assertEquals(1050L, CurrencyManager.parseToMinorUnits("10.50", eur, deLocale))
    }

    @Test
    fun testParseToMinorUnits_zeroDecimals() {
        val usLocale = Locale.US

        assertEquals(100L, CurrencyManager.parseToMinorUnits("100", jpy, usLocale))
        assertEquals(0L, CurrencyManager.parseToMinorUnits("0", jpy, usLocale))
        assertEquals(5000L, CurrencyManager.parseToMinorUnits("5000", jpy, usLocale))

        // JPY should not accept decimal places
        assertNull(CurrencyManager.parseToMinorUnits("100.5", jpy, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("100.50", jpy, usLocale))
    }

    @Test
    fun testParseToMinorUnits_invalidInputs() {
        val usLocale = Locale.US

        assertNull(CurrencyManager.parseToMinorUnits("", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("   ", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("abc", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("-10", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("10.500", eur, usLocale)) // More decimals than allowed
        assertNull(CurrencyManager.parseToMinorUnits("10.5.5", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("10,5,5", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("$10.50", eur, usLocale))
        assertNull(CurrencyManager.parseToMinorUnits("10€", eur, usLocale))
    }

    @Test
    fun testSupportedCurrencies() {
        val list = CurrencyManager.supportedCurrencies
        assertEquals(9, list.size)
        val codes = list.map { it.currencyCode }
        assertTrue(codes.contains("EUR"))
        assertTrue(codes.contains("USD"))
        assertTrue(codes.contains("GBP"))
        assertTrue(codes.contains("JPY"))
        assertTrue(codes.contains("CHF"))
        assertTrue(codes.contains("CAD"))
        assertTrue(codes.contains("AUD"))
        assertTrue(codes.contains("CNY"))
        assertTrue(codes.contains("INR"))
    }

    @Test
    fun testSetCurrencyAndFlow() {
        CurrencyManager.setCurrency("USD")
        assertEquals("USD", CurrencyManager.getCurrency().currencyCode)
        assertEquals("USD", CurrencyManager.currencyFlow().value.currencyCode)

        CurrencyManager.setCurrency("JPY")
        assertEquals("JPY", CurrencyManager.getCurrency().currencyCode)
        assertEquals("JPY", CurrencyManager.currencyFlow().value.currencyCode)

        // Invalid currency code fallback to EUR
        CurrencyManager.setCurrency("INVALID_CODE")
        assertEquals("EUR", CurrencyManager.getCurrency().currencyCode)

        // Reset to EUR for clean state
        CurrencyManager.setCurrency("EUR")
        assertEquals("EUR", CurrencyManager.getCurrency().currencyCode)
    }

    @Test
    fun testGetDecimalSeparator() {
        assertEquals('.', CurrencyManager.getDecimalSeparator(Locale.US))
        assertEquals(',', CurrencyManager.getDecimalSeparator(Locale.GERMANY))
    }
}
