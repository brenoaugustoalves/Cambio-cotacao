package br.com.cambioclaro.app.conversion.application;

import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.conversion.domain.ConversionQuote;
import br.com.cambioclaro.app.conversion.domain.ConversionRequest;
import br.com.cambioclaro.app.conversion.domain.ConversionResult;
import java.math.BigDecimal;

/** Coordinates quote selection and conversion policy, without Android dependencies. */
public final class ConvertCurrency {
    private final ConversionQuoteSource quotes;

    public ConvertCurrency(ConversionQuoteSource quotes) {
        if (quotes == null) throw new IllegalArgumentException("Fonte de cotações obrigatória");
        this.quotes = quotes;
    }

    public ConversionResult execute(ConversionRequest request) {
        if (request == null) return ConversionResult.failure(ConversionResult.Status.INVALID_CURRENCY);
        if (!request.currencyCode.matches("[A-Z]{3}")) return ConversionResult.failure(ConversionResult.Status.INVALID_CURRENCY);
        if (request.tourism && !"USD".equals(request.currencyCode)) return ConversionResult.failure(ConversionResult.Status.INVALID_MODE);
        if (request.amountText.isEmpty()) return ConversionResult.failure(ConversionResult.Status.AMOUNT_REQUIRED);

        ConversionQuote quote = request.tourism ? quotes.tourism() : quotes.market(request.currencyCode);
        if (quote == null && !request.tourism) quote = quotes.ptax(request.currencyCode);
        if (quote == null) return ConversionResult.failure(ConversionResult.Status.RATE_UNAVAILABLE);

        try {
            BigDecimal converted = Rate.convert(request.amountText, quote.reaisPerUnit);
            return ConversionResult.success(converted, quote);
        } catch (IllegalArgumentException invalidAmount) {
            return ConversionResult.failure(ConversionResult.Status.INVALID_AMOUNT);
        }
    }
}
