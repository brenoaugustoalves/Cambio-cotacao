package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;

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

    public void refreshDaily(FxCurrency currency, int observations) throws Exception {
        requireCurrency(currency);
        if (observations < 1 || observations > 365) throw new IllegalArgumentException("Período inválido");
        int requestDays = observations == 365 ? 360 : Math.min(360, observations * 2 + 7);
        history.saveDaily(currency, observations, source.fetchDaily(currency, requestDays));
    }

    private static void requireCurrency(FxCurrency currency) {
        if (currency == null) throw new IllegalArgumentException("Moeda obrigatória");
    }
}
