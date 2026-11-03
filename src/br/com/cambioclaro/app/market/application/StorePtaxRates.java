package br.com.cambioclaro.app.market.application;

import br.com.cambioclaro.app.FxCurrency;
import br.com.cambioclaro.app.Rate;
import br.com.cambioclaro.app.market.domain.PtaxBatch;
import java.io.IOException;
import java.time.LocalDate;
import java.util.function.LongSupplier;

/** Fetches and persists official PTAX while rejecting time-regressed responses. */
public final class StorePtaxRates {
    private final PtaxQuoteSource source;
    private final PtaxRateRepository rates;
    private final LongSupplier clock;

    public StorePtaxRates(PtaxQuoteSource source, PtaxRateRepository rates) {
        this(source, rates, System::currentTimeMillis);
    }

    public StorePtaxRates(PtaxQuoteSource source, PtaxRateRepository rates, LongSupplier clock) {
        if (source == null || rates == null || clock == null) throw new IllegalArgumentException("Fonte, repositório e relógio PTAX são obrigatórios");
        this.source = source;
        this.rates = rates;
        this.clock = clock;
    }

    public StoredPtaxRates execute(FxCurrency currency, LocalDate date) throws Exception {
        if (currency == null || date == null) throw new IllegalArgumentException("Consulta PTAX inválida");
        PtaxBatch batch = source.fetch(currency, date);
        Rate previous = rates.latest(currency);
        if (previous != null && batch.latest().at.isBefore(previous.at)) {
            throw new IOException("A fonte retornou dados mais antigos. Mantivemos a última consulta válida.");
        }
        long fetchedAt = clock.getAsLong();
        if (fetchedAt <= 0) throw new IllegalStateException("Relógio retornou horário inválido");
        rates.save(currency, batch, fetchedAt);
        return new StoredPtaxRates(batch, fetchedAt);
    }
}
