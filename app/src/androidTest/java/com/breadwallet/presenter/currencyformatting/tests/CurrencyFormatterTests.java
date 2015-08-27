//package com.breadwallet.presenter.currencyformatting.tests;
//
//import android.test.AndroidTestCase;
//
//import java.util.Locale;
//public class CurrencyFormatterTests extends AndroidTestCase {
//    /**
//     * These tests differ between Android <= 4.0, and >= 4.1.
//     */
//
//    public void testJPY() {
//        String expected = "￥15";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("ja", "JP"), "JPY", 15.00);
//        assertEquals(expected, actual);
//        // 4.0 and below - Fails. (￥15.00)
//        // 4.1 and above - Passes.
//    }
//
//    public void testNZDollarsInJapanLocale() {
//        // New Zealand currency, but the device locale is ja_JP
//        String expected = "NZ$16"; // Yen has no decimal units
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("ja", "JP"), "NZD", 15.50);
//        assertEquals(expected, actual);
//        // 4.0 and below - Fails. NZ$15.50.
//        // 4.1 and above - Passes.
//    }
//
//    public void testCLP() {
//        String expected = "$15"; // Chilean Peso has no decimal units.
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("es", "CL"), "CLP", 15.00);
//        assertEquals(expected, actual);
//        // 4.0 and below - Fails. (CL$ 15,00)
//        // 4.1 and above - Passes.
//    }
//
//    public void testNZDollarsInChileLocale() {
//        // New Zealand currency, but the device locale is es_CL
//        String expected = "NZ$16";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("es", "CL"), "NZD", 15.50);
//        assertEquals(expected, actual);
//        // 4.0 and below - Fails. NZD 15,50
//        // 4.1 and above - Fails. NZD16
//    }
//
//    /**
//     * These tests fail, presumably because the formatting tables for the provided locales don't
//     * have a custom currency unit for the supplied ISO currency code.
//     */
//
//    public void testNZDollarsInAustralianLocale() {
//        String expected = "NZ$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "AU"), "NZD", 15.00);
//        assertEquals(expected, actual);
//        // Fails. NZD15.00
//    }
//
//    public void testAustralianDollarsInNewZealandLocale() {
//        String expected = "AU$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "NZ"), "AUD", 15.00);
//        assertEquals(expected, actual);
//        // Fails. AUD15.00
//    }
//
//    /**
//     * These tests pass in all cases I've tested. (device version and locale variations)
//     */
//
//    public void testGBP() {
//        String expected = "£15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "GB"), "GBP", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testNZD() {
//        String expected = "$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "NZ"), "NZD", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testUSD() {
//        String expected = "$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "US"), "USD", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testNZDInUSLocale() {
//        String expected = "NZ$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "US"), "NZD", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testEURInFranceLocale() {
//        String expected = "15,00 €";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("fr", "FR"), "EUR", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testEURInRepublicOfIrelandLocale() {
//        String expected = "€15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "IE"), "EUR", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testUSDollarsInCanadianLocale() {
//        String expected = "US$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "CA"), "USD", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testCanadianDollarsInUSLocale() {
//        String expected = "CA$15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "US"), "CAD", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testEurosInUnitedKingdomLocale() {
//        String expected = "€15.00";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("en", "GB"), "EUR", 15.00);
//        assertEquals(expected, actual);
//    }
//
//    public void testPoundsInFranceLocale() {
//        String expected = "15,00 £UK";
//        String actual = CurrencyFormatter.getFormattedCurrencyStringForLocale(new Locale("fr", "FR"), "GBP", 15.00);
//        assertEquals(expected, actual);
//    }
//
//
//}