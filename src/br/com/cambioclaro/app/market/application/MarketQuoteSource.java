package br.com.cambioclaro.app.market.application;

/** Fetches the provider's unmodified batch response. */
public interface MarketQuoteSource {
    String fetch() throws Exception;
}
