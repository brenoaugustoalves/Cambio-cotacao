package br.com.cambioclaro.app.economy.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.economy.domain.EconomicSimulator;
import br.com.cambioclaro.app.economy.domain.EconomicQuoteReference;
import java.math.BigDecimal;
import java.time.Instant;

/** Gets a published rate and applies the transparent cost calculation. */
public final class CalculateEconomicScenario {
    private final EconomicQuoteProvider quotes;
    public CalculateEconomicScenario(EconomicQuoteProvider quotes){if(quotes==null)throw new IllegalArgumentException("Fonte de cotação obrigatória");this.quotes=quotes;}
    public EconomicScenario execute(FxCurrency currency,boolean marketQuote,BigDecimal amount,
            BigDecimal spread,BigDecimal extra,BigDecimal fixedFee,Instant now)throws Exception{
        if(currency==null||now==null)throw new IllegalArgumentException("Moeda e instante são obrigatórios");
        EconomicQuoteReference reference=quotes.get(currency,marketQuote,now);
        if(reference==null)throw new IllegalStateException("Fonte não forneceu cotação publicada");
        return new EconomicScenario(EconomicSimulator.calculate(amount,reference.rate,spread,extra,fixedFee),reference);
    }
}
