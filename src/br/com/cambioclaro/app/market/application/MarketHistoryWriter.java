package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.MarketObservation;

/** Stores a real batch observation in the local chart archive. */
public interface MarketHistoryWriter {
    void archive(FxCurrency currency, MarketObservation observation);
}
