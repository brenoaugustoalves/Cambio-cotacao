package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.market.domain.PtaxBatch;
import java.time.LocalDate;

public interface PtaxQuoteSource {
    PtaxBatch fetch(FxCurrency currency, LocalDate date) throws Exception;
}
