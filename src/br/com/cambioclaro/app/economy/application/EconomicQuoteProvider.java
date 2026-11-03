package br.com.cambioclaro.app.economy.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.economy.domain.EconomicQuoteReference;
import java.time.Instant;

/** Port that provides a real published rate for a scenario. */
public interface EconomicQuoteProvider {
    EconomicQuoteReference get(FxCurrency currency, boolean marketQuote, Instant now) throws Exception;
}
