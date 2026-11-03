package br.com.cambioclaro.app.market.domain;

import br.com.cambioclaro.app.Rate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed official PTAX response together with the provider payload persisted by the app. */
public final class PtaxBatch {
    public final String rawResponse;
    public final List<Rate> rates;

    public PtaxBatch(String rawResponse, List<Rate> rates) {
        if (rawResponse == null || rawResponse.isEmpty() || rates == null || rates.isEmpty()) {
            throw new IllegalArgumentException("Lote PTAX sem resposta ou observações");
        }
        this.rawResponse = rawResponse;
        this.rates = Collections.unmodifiableList(new ArrayList<>(rates));
    }

    public Rate latest() {
        return rates.get(rates.size() - 1);
    }
}
