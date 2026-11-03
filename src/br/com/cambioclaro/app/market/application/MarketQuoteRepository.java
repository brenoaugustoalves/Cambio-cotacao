package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.MarketObservation;

/** Persists a validated provider response and reads the effective observation per currency. */
public interface MarketQuoteRepository {
    void saveBatch(String rawResponse) throws Exception;
    MarketObservation snapshot(FxCurrency currency);
}
