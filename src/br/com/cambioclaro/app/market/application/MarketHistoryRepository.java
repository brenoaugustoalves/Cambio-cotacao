package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;

/** Validates and persists provider responses in the matching history store. */
public interface MarketHistoryRepository {
    void saveIntraday(FxCurrency currency, String rawResponse) throws Exception;
    void saveDaily(FxCurrency currency, int observations, String rawResponse) throws Exception;
}
