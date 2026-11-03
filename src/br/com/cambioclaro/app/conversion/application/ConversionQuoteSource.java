package br.com.cambioclaro.app.conversion.application;

import br.com.cambioclaro.app.conversion.domain.ConversionQuote;

/** Port implemented by platform data adapters. Null means no valid saved observation. */
public interface ConversionQuoteSource {
    ConversionQuote market(String currencyCode);
    ConversionQuote tourism();
    ConversionQuote ptax(String currencyCode);
}
