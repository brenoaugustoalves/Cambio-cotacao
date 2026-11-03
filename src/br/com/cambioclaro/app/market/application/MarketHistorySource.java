package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import java.time.LocalDate;

/** Fetches real intraday observations or daily closes from the provider. */
public interface MarketHistorySource {
    String fetchIntraday(FxCurrency currency) throws Exception;
    String fetchDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate) throws Exception;
}
