package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.MarketObservation;

/** Coordinates one validated batch refresh, archive updates, and opted-in alerts. */
public final class RefreshMarketQuotes {
    private final StoreMarketBatch batch;
    private final MarketQuoteRepository quotes;
    private final MarketHistoryWriter history;
    private final MarketAlertSink alerts;

    public RefreshMarketQuotes(StoreMarketBatch batch, MarketQuoteRepository quotes,
            MarketHistoryWriter history, MarketAlertSink alerts) {
        if (batch == null || quotes == null || history == null || alerts == null) {
            throw new IllegalArgumentException("Dependências da atualização são obrigatórias");
        }
        this.batch = batch;
        this.quotes = quotes;
        this.history = history;
        this.alerts = alerts;
    }

    public void execute() throws Exception {
        batch.execute();
        for (FxCurrency currency : FxCurrency.values()) {
            MarketObservation observation = quotes.snapshot(currency);
            if (observation == null) continue;
            history.archive(currency, observation);
            alerts.evaluate(currency, observation);
        }
    }
}
