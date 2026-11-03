package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;

/** Fetches real intraday observations or daily closes from the provider. */
public interface MarketHistorySource {
    String fetchIntraday(FxCurrency currency) throws Exception;
    String fetchDaily(FxCurrency currency, int days) throws Exception;
}
