package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.PtaxBatch;
import java.util.List;

/** Successful PTAX refresh and its local fetch time. */
public final class StoredPtaxRates {
    public final PtaxBatch batch;
    public final long fetchedAt;
    public final List<Rate> rates;

    StoredPtaxRates(PtaxBatch batch, long fetchedAt) {
        this.batch = batch;
        this.fetchedAt = fetchedAt;
        this.rates = batch.rates;
    }
}
