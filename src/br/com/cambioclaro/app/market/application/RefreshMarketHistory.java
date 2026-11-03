package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import java.time.Duration;
import java.time.LocalDate;

/** Coordinates source reads and persistence for real intraday and daily history. */
public final class RefreshMarketHistory {
    private final MarketHistorySource source;
    private final MarketHistoryRepository history;

    public RefreshMarketHistory(MarketHistorySource source, MarketHistoryRepository history) {
        if (source == null || history == null) throw new IllegalArgumentException("Fonte e histórico são obrigatórios");
        this.source = source;
        this.history = history;
    }

    public void refreshIntraday(FxCurrency currency) throws Exception {
        requireCurrency(currency);
        history.saveIntraday(currency, source.fetchIntraday(currency));
    }

    public void refreshDaily(FxCurrency currency, LocalDate startDate, LocalDate endDate) throws Exception {
        requireCurrency(currency);
        if (startDate == null || endDate == null || startDate.isAfter(endDate)
                || Duration.between(startDate.atStartOfDay(), endDate.atStartOfDay()).toDays() > 366)
            throw new IllegalArgumentException("Período inválido");
        history.saveDaily(currency, startDate, endDate, source.fetchDaily(currency, startDate, endDate));
    }

    private static void requireCurrency(FxCurrency currency) {
        if (currency == null) throw new IllegalArgumentException("Moeda obrigatória");
    }
}
