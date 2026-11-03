package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.PtaxBatch;

public interface PtaxAlertSink {
    void evaluate(FxCurrency currency, PtaxBatch batch);
}
