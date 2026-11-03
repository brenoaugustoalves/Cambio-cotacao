package br.com.cambioclaro.app.conversion.domain;

import java.util.Locale;

/** User intent for a foreign-currency-to-BRL conversion. */
public final class ConversionRequest {
    public final String currencyCode;
    public final String amountText;
    public final boolean tourism;

    public ConversionRequest(String currencyCode, String amountText, boolean tourism) {
        this.currencyCode = currencyCode == null ? "" : currencyCode.trim().toUpperCase(Locale.ROOT);
        this.amountText = amountText == null ? "" : amountText.trim();
        this.tourism = tourism;
    }
}
