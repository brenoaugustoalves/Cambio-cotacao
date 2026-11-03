package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import java.time.LocalDate;

/** Stores official rates and then evaluates alerts for the successfully published batch. */
public final class RefreshPtaxRates {
    private final StorePtaxRates store;
    private final PtaxAlertSink alerts;

    public RefreshPtaxRates(StorePtaxRates store, PtaxAlertSink alerts) {
        if (store == null || alerts == null) throw new IllegalArgumentException("Armazenamento e alertas PTAX são obrigatórios");
        this.store = store;
        this.alerts = alerts;
    }

    public StoredPtaxRates execute(FxCurrency currency, LocalDate date) throws Exception {
        StoredPtaxRates result = store.execute(currency, date);
        alerts.evaluate(currency, result.batch);
        return result;
    }
}
