package br.com.cambioclaro.app.economy.application;

import br.com.cambioclaro.app.economy.domain.EconomicSimulator;
import br.com.cambioclaro.app.economy.domain.EconomicQuoteReference;

/** Computed scenario with its published rate provenance. */
public final class EconomicScenario {
    public final EconomicSimulator.Result totals;
    public final EconomicQuoteReference reference;
    EconomicScenario(EconomicSimulator.Result totals,EconomicQuoteReference reference){this.totals=totals;this.reference=reference;}
}
