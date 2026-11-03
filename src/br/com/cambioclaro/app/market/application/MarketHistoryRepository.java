package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import java.time.LocalDate;

/** Validates and persists provider responses in the matching history store. */
public interface MarketHistoryRepository {
    void saveIntraday(FxCurrency currency, String rawResponse) throws Exception;
    void saveDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate, String rawResponse) throws Exception;
}
