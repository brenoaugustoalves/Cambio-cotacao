package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.MarketObservation;

/** Evaluates an observation against the user's opted-in alert settings. */
public interface MarketAlertSink {
    void evaluate(FxCurrency currency, MarketObservation observation);
}
