package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.PtaxBatch;

public interface PtaxRateRepository {
    Rate latest(FxCurrency currency);
    void save(FxCurrency currency, PtaxBatch batch, long fetchedAt) throws Exception;
}
